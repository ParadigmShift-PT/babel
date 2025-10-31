package pt.unl.fct.di.novasys.channel.secure.weakauth;

import static java.util.Optional.empty;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Promise;
import pt.unl.fct.di.novasys.channel.secure.SecureChannelListener;
import pt.unl.fct.di.novasys.channel.secure.SecureSingleThreadedBiChannel;
import pt.unl.fct.di.novasys.channel.secure.events.SecureInConnectionDown;
import pt.unl.fct.di.novasys.channel.secure.events.SecureInConnectionUp;
import pt.unl.fct.di.novasys.channel.secure.events.SecureOutConnectionDown;
import pt.unl.fct.di.novasys.channel.secure.events.SecureOutConnectionFailed;
import pt.unl.fct.di.novasys.channel.secure.events.SecureOutConnectionUp;
import pt.unl.fct.di.novasys.channel.secure.utils.X509CertificateSerializer;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionFailed;
import pt.unl.fct.di.novasys.network.AttributeValidator;
import pt.unl.fct.di.novasys.network.Connection;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.NetworkManager;
import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.data.Bytes;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.exceptions.InvalidHandshakeAttributesException;
import pt.unl.fct.di.novasys.network.exceptions.InvalidHandshakeException;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

/**
 * TODO docs
 */
public class WeakAuthChannel<T> extends SecureSingleThreadedBiChannel<T, T> implements AttributeValidator {

    private static final Logger logger = LogManager.getLogger(WeakAuthChannel.class);
    public static final short CHANNEL_MAGIC_NUMBER = 0x5504;

    public final static String NAME = "WeakAuthChannel";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";
    public final static String WORKER_GROUP_KEY = "worker_group";
    public final static String TRIGGER_SENT_KEY = "trigger_sent";
    public final static String METRICS_INTERVAL_KEY = "metrics_interval";
    public final static String HEARTBEAT_INTERVAL_KEY = "heartbeat_interval";
    public final static String HEARTBEAT_TOLERANCE_KEY = "heartbeat_tolerance";
    public final static String CONNECT_TIMEOUT_KEY = "connect_timeout";

    /**
     * nonce size in bytes
     */
    public static final String NONCE_SIZE_KEY = "nonce_length";

    public static final String LISTEN_ADDRESS_ATTR = "listen_address";

    public final static String DEFAULT_PORT = "9572";
    public final static String DEFAULT_HB_INTERVAL = "0";
    public final static String DEFAULT_HB_TOLERANCE = "0";
    public final static String DEFAULT_CONNECT_TIMEOUT = "1000";
    public final static String DEFAULT_METRICS_INTERVAL = "-1";

    public static final String DEFAULT_NONCE_SIZE = "8";

    public final static int CONNECTION_OUT = 0;
    public final static int CONNECTION_IN = 1;

    private final static byte HANDSHAKE_STEPS = 3;

    static final String DEFAULT_ASYM_KEY_ALG = "RSA";

    private final NetworkManager<T> network;
    private final SecureChannelListener<T> listener;

    private final Attributes baseAttributes;

    // Host represents the client server socket, not the client tcp connection
    // address!
    // client connection address is in connection.getPeer

    private final Map<Long, IdentifiedConnectionState<T>> outConnections;

    private final Map<Bytes, IdentifiedConnectionState<T>> outIdConnections;
    private final Map<Host, List<IdentifiedConnectionState<T>>> outHostConnections;

    private final Map<Bytes, List<Pair<Connection<T>, Host>>> inIdConnections;
    private final Map<Host, List<Pair<Connection<T>, byte[]>>> inHostConnections;

    private List<Pair<Host, Connection<T>>> oldIn;
    private List<Pair<Host, IdentifiedConnectionState<T>>> oldOUt;

    private final boolean triggerSent;
    private final boolean metrics;

    private final Provider securityProvider;
    private final SecureRandom nonceRng;

    private final int nonceSize;

    private final X509IKeyManager keyManager;
    private final X509ITrustManager trustManager;

    public WeakAuthChannel(ISerializer<T> serializer, SecureChannelListener<T> list, Properties properties,
            X509IKeyManager keyManager, X509ITrustManager trustManager) throws IOException {
        super(NAME);
        this.listener = list;

        securityProvider = new BouncyCastleProvider();
        SecureRandom rng;
        try {
            rng = SecureRandom.getInstance("NonceAndIV", securityProvider);
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Failed to get \"NonceAndIV\" secure random");
            rng = new SecureRandom();
        }
        nonceRng = rng;

        this.keyManager = keyManager;
        this.trustManager = trustManager;

        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        int hbInterval = Integer.parseInt(properties.getProperty(HEARTBEAT_INTERVAL_KEY, DEFAULT_HB_INTERVAL));
        int hbTolerance = Integer.parseInt(properties.getProperty(HEARTBEAT_TOLERANCE_KEY, DEFAULT_HB_TOLERANCE));
        int connTimeout = Integer.parseInt(properties.getProperty(CONNECT_TIMEOUT_KEY, DEFAULT_CONNECT_TIMEOUT));
        int metricsInterval = Integer.parseInt(properties.getProperty(METRICS_INTERVAL_KEY, DEFAULT_METRICS_INTERVAL));
        this.triggerSent = Boolean.parseBoolean(properties.getProperty(TRIGGER_SENT_KEY, "false"));
        this.metrics = metricsInterval > 0;

        this.nonceSize = Integer.parseInt(properties.getProperty(NONCE_SIZE_KEY, DEFAULT_NONCE_SIZE));

        Host listenAddress = new Host(addr, port);

        EventLoopGroup eventExecutors = properties.containsKey(WORKER_GROUP_KEY)
                ? (EventLoopGroup) properties.get(WORKER_GROUP_KEY)
                : NetworkManager.createNewWorkerGroup();

        baseAttributes = new Attributes();
        baseAttributes.putShort(CHANNEL_MAGIC_ATTRIBUTE, CHANNEL_MAGIC_NUMBER);
        baseAttributes.putHost(LISTEN_ADDRESS_ATTR, listenAddress);

        network = new NetworkManager<>(HANDSHAKE_STEPS, serializer, this, hbInterval, hbTolerance, connTimeout,
                eventExecutors);
        network.createServerSocket(this, listenAddress, baseAttributes, this, eventExecutors);

        outConnections = new HashMap<>();
        outHostConnections = new HashMap<>();
        outIdConnections = new HashMap<>();
        inIdConnections = new HashMap<>();
        inHostConnections = new HashMap<>();

        if (metrics) {
            oldIn = new LinkedList<>();
            oldOUt = new LinkedList<>();
            loop.scheduleAtFixedRate(this::triggerMetricsEvent, metricsInterval, metricsInterval,
                    TimeUnit.MILLISECONDS);
        }
    }

    void triggerMetricsEvent() {
        // TODO listener.deliverEvent(new ChannelMetrics(oldIn, oldOUt, inConnections,
        // outConnections));
    }

    // ------------------ Attribute validator

    // First handhsake
    private static final String EXPECTED_ID_ATTR = "expected_identity";
    // First + second handshake
    private static final String ID_ATTR = "identity";
    private static final String CERT_ATTR = "certificate";
    private static final String NONCE_ATTR = "nonce";
    // Second + third handshake
    private static final String SIG_NONCE_ATTR = "signed_nonce";

    @Override
    public boolean validateAttributes(Attributes attr) {
        Short channel = attr.getShort(CHANNEL_MAGIC_ATTRIBUTE);
        return channel != null && channel == CHANNEL_MAGIC_NUMBER;
    }

    private Attributes getFirstHandshakeAttributes(byte[] expectedId) throws CertificateException {
        var attrs = baseAttributes.shallowClone();

        if (expectedId != null)
            attrs.putBytes(EXPECTED_ID_ATTR, expectedId);

        String idAlias = keyManager.chooseServerAlias(DEFAULT_ASYM_KEY_ALG, null, null);
        if (idAlias == null)
            idAlias = keyManager.chooseServerAlias(null, null, null);

        var id = keyManager.getAliasId(idAlias);
        attrs.putBytes(ID_ATTR, id);

        var cert = keyManager.getCertificateChain(idAlias)[0];
        try {
            attrs.putObject(CERT_ATTR, cert, X509CertificateSerializer.INSTANCE);
        } catch (IOException e) {
            throw new CertificateException(e);
        }

        var nonce = new byte[nonceSize];
        nonceRng.nextBytes(nonce);
        attrs.putBytes(NONCE_ATTR, nonce);

        return attrs;
    }

    @Override
    public Attributes getSecondHandshakeAttributes(long channelId, Attributes peerAttrs, Attributes myBaseAttrs)
            throws InvalidHandshakeAttributesException {
        try {
            return loop.submit(() -> onGetSecondHandshakeAttributes(channelId, peerAttrs, myBaseAttrs)).get();
        } catch (ExecutionException e) {
            throw (InvalidHandshakeAttributesException) e.getCause();
        } catch (InterruptedException e) {
            throw new InvalidHandshakeAttributesException(peerAttrs, 1, e);
        }
    }

    public Attributes onGetSecondHandshakeAttributes(long channelId, Attributes peerAttrs, Attributes myBaseAttrs)
            throws InvalidHandshakeAttributesException {
        if (!validateAttributes(peerAttrs))
            throw new InvalidHandshakeAttributesException(peerAttrs, 1);

        try {
            X509Certificate peerCert = peerAttrs.getObject(CERT_ATTR, X509CertificateSerializer.INSTANCE);

            // Check peer id and certificate consistency and trust
            byte[] peerId = trustManager.extractIdFromCertificate(peerCert);
            if (!Arrays.equals(peerId, peerAttrs.getBytes(ID_ATTR))) {
                logger.debug(
                        "In connection attribute validation failed: peer id in attributes ({}) differs from the one extracted from certificate ({})",
                        Bytes.of(peerId), Bytes.of(peerAttrs.getBytes(ID_ATTR)));
                throw new InvalidHandshakeAttributesException(peerAttrs, 1);
            }
            trustManager.checkClientTrusted(new X509Certificate[] { peerCert }, peerCert.getSigAlgName());

            // Prepare reply
            Attributes replyAttrs = myBaseAttrs.shallowClone();

            X509Certificate myCert;
            PrivateKey privKey;
            byte[] myId = peerAttrs.getBytes(EXPECTED_ID_ATTR);

            if (myId != null) {
                myCert = keyManager.getCertificateChain(myId)[0];
                privKey = keyManager.getPrivateKey(myId);
            } else {
                String alias = keyManager.chooseServerAlias(DEFAULT_ASYM_KEY_ALG, null, null);
                if (alias == null)
                    alias = keyManager.chooseServerAlias(DEFAULT_ASYM_KEY_ALG, null, null);
                myId = keyManager.getAliasId(alias);
                myCert = keyManager.getCertificateChain(alias)[0];
                privKey = keyManager.getPrivateKey(alias);
            }

            replyAttrs.putBytes(ID_ATTR, myId);
            replyAttrs.putObject(CERT_ATTR, myCert, X509CertificateSerializer.INSTANCE);

            byte[] signedNonce = signNonce(peerAttrs.getBytes(NONCE_ATTR), myCert, privKey);
            replyAttrs.putBytes(SIG_NONCE_ATTR, signedNonce);

            var nonce = new byte[nonceSize];
            nonceRng.nextBytes(nonce);
            replyAttrs.putBytes(NONCE_ATTR, nonce);

            return replyAttrs;
        } catch (NullPointerException | IOException | CertificateException | NoSuchAlgorithmException
                | InvalidKeyException | SignatureException e) {
            throw new InvalidHandshakeAttributesException(peerAttrs, 1, e);
        }
    }

    private byte[] signNonce(byte[] nonce, X509Certificate myCert, PrivateKey privKey)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(myCert.getSigAlgName(), securityProvider);
        signature.initSign(privKey);
        signature.update(nonce);
        return signature.sign();
    }

    @Override
    public Attributes getNthHandshakeAttributes(long connectionId, int handshakeN,
            List<Attributes> peerAttrs, List<Attributes> mySentAttrs) throws InvalidHandshakeAttributesException {
        try {
            return loop.submit(() -> onGetNthHandshakeAttributes(connectionId, handshakeN, peerAttrs, mySentAttrs))
                    .get();
        } catch (ExecutionException e) {
            throw (InvalidHandshakeAttributesException) e.getCause();
        } catch (InterruptedException e) {
            throw new InvalidHandshakeAttributesException(peerAttrs.getLast(), handshakeN - 1, e);
        }
    }

    public Attributes onGetNthHandshakeAttributes(long connectionId, int handshakeN,
            List<Attributes> peerAttrs, List<Attributes> mySentAttrs) throws InvalidHandshakeAttributesException {
        var conState = outConnections.get(connectionId);
        try {
            if (handshakeN == 3) {
                var firstHsAttrs = mySentAttrs.getFirst();
                var secondHsAttrs = peerAttrs.getFirst();

                // Verify second hs attrs
                byte[] peerId = secondHsAttrs.getBytes(ID_ATTR);

                if (conState.getPeerId() != null && !Arrays.equals(peerId, conState.getPeerId()))
                    throw new InvalidHandshakeAttributesException(secondHsAttrs,
                            "Handshake step 2: peer id was different from the expected id");

                var peerCert = secondHsAttrs.getObject(CERT_ATTR, X509CertificateSerializer.INSTANCE);

                if (!Arrays.equals(peerId, trustManager.extractIdFromCertificate(peerCert))) {
                    logger.debug("Out connection attribute validation failed: "
                            + "peer id in attributes ({}) differs from the one extracted from certificate ({})",
                            Bytes.of(peerId), Bytes.of(secondHsAttrs.getBytes(ID_ATTR)));
                    throw new InvalidHandshakeAttributesException(secondHsAttrs,
                            "Handshake step 2: peer id was different from the one exctracted from certificate");
                }
                trustManager.checkClientTrusted(new X509Certificate[] { peerCert }, peerCert.getSigAlgName());

                var sentNonce = firstHsAttrs.getBytes(NONCE_ATTR);
                var recvdSignedNonce = secondHsAttrs.getBytes(SIG_NONCE_ATTR);

                Signature sig = Signature.getInstance(peerCert.getSigAlgName(), securityProvider);
                sig.initVerify(peerCert.getPublicKey());
                sig.update(sentNonce);
                if (!sig.verify(recvdSignedNonce)) {
                    logger.debug("Out connection attribute validation failed: sent nonce wasn't correctly signed.");
                    throw new InvalidHandshakeAttributesException(secondHsAttrs,
                            "Handshake step 2: invalid nonce signature");
                }

                // Update connection state
                if (conState.getPeerId() == null) {
                    outIdConnections.put(Bytes.of(peerId), conState);
                } else if (outIdConnections.get(Bytes.of(peerId)).isAuthenticated()) {
                    throw new InvalidHandshakeException("Handshake step 2: repeated connection to peer");
                }

                conState.setPeerId(peerId);
                conState.setAuthenticated();

                // Build third hs attrs
                Attributes attrs = new Attributes();

                byte[] myId = firstHsAttrs.getBytes(ID_ATTR);
                byte[] signedNonce = signNonce(
                        secondHsAttrs.getBytes(NONCE_ATTR),
                        keyManager.getCertificateChain(myId)[0],
                        keyManager.getPrivateKey(myId));

                attrs.putBytes(SIG_NONCE_ATTR, signedNonce);

                return attrs;
            } else if (handshakeN == 4) {
                var firstHsAttrs = peerAttrs.getFirst();
                var thirdHsAttrs = peerAttrs.getLast();

                // Verify nonce
                var peerCert = firstHsAttrs.getObject(CERT_ATTR, X509CertificateSerializer.INSTANCE);
                byte[] sentNonce = mySentAttrs.getFirst().getBytes(NONCE_ATTR);
                byte[] recvdSignedNonce = thirdHsAttrs.getBytes(SIG_NONCE_ATTR);

                Signature sig = Signature.getInstance(peerCert.getSigAlgName(), securityProvider);
                sig.initVerify(peerCert.getPublicKey());
                sig.update(sentNonce);
                if (!sig.verify(recvdSignedNonce)) {
                    logger.debug("In connection attribute validation failed: sent nonce wasn't correctly signed.");
                    throw new InvalidHandshakeAttributesException(thirdHsAttrs, 3);
                }

                // Sucess!
                return Attributes.EMPTY;
            } else {
                throw new InvalidHandshakeAttributesException(peerAttrs.getLast(), handshakeN - 1);
            }
        } catch (Exception e) {
            if (e instanceof InvalidHandshakeAttributesException hsEx)
                throw hsEx;
            else
                throw new InvalidHandshakeAttributesException(peerAttrs.getLast(), handshakeN - 1, e);
        }
    }

    // -------------- SecureSingleThreadedBiChannel

    private IdentifiedConnectionState<T> removeHostIdConnection(Host host, byte[] peerId) {
        var hostCons = outHostConnections.get(host);
        var it = hostCons.iterator();
        while (it.hasNext()) {
            var conState = it.next();
            if (Arrays.equals(conState.getPeerId(), peerId)) {
                it.remove();
                return conState;
            }
        }
        return null;
    }

    @Override
    protected void onOpenConnection(Host peer, int connection) {
        var connections = outHostConnections.get(peer);
        if (connections == null) {
            try {
                logger.debug("onOpenConnection creating connection to: " + peer);
                Attributes firstHsAttrs = getFirstHandshakeAttributes(null);
                var conState = new IdentifiedConnectionState<>(network.createConnection(peer, firstHsAttrs, this, this),
                        peer);
                connections = new LinkedList<>();
                connections.add(conState);
                outHostConnections.put(peer, connections);
                outConnections.put(conState.getConnectionId(), conState);
            } catch (CertificateException e) {
                logger.error("onOpenConnection to {} failed with exception {}", peer, e);
                listener.deliverEvent(new OutConnectionFailed<>(peer, new LinkedList<>(), e));
            }
        } else {
            logger.debug("onOpenConnection ignored: " + peer);
        }
    }

    @Override
    protected void onOpenConnection(Host peer, byte[] expectedId, int connection) {
        var expectedIdB = Bytes.of(expectedId);
        var conState = outIdConnections.get(expectedIdB);
        if (conState == null) {
            try {
                logger.debug("onOpenConnection creating connection to {} ({})", peer, expectedIdB);
                var attrs = getFirstHandshakeAttributes(expectedId);
                conState = new IdentifiedConnectionState<>(network.createConnection(peer, attrs, this, this), peer);
                conState.setPeerId(expectedId);
                outIdConnections.put(expectedIdB, conState);
                outHostConnections.computeIfAbsent(peer, __ -> new LinkedList<>()).add(conState);
                outConnections.put(conState.getConnectionId(), conState);
            } catch (CertificateException e) {
                logger.error("onOpenConnection to {} ({}) failed with exception {}", peer, expectedIdB, e);
                listener.deliverEvent(new SecureOutConnectionFailed<>(peer, expectedId, new LinkedList<>(), e));
            }
        } else {
            logger.debug("onOpenConnection ignored: {} ({})", peer, expectedId);
        }
    }

    @Override
    protected void onSendMessage(T msg, Host peer, int connection) {
        logger.debug("onSendMessage " + msg + " " + peer + " " + (connection == CONNECTION_IN ? "IN" : "OUT"));

        if (connection <= CONNECTION_OUT) {
            List<IdentifiedConnectionState<T>> conList = outHostConnections.get(peer);
            if (conList != null) {
                assert !conList.isEmpty();
                sendOutConMessage(conList.getLast(), msg);
            } else {
                listener.messageFailed(msg, peer, new IllegalArgumentException("No outgoing connection"));
            }
        } else if (connection == CONNECTION_IN) {
            List<Pair<Connection<T>, byte[]>> inCons = inHostConnections.get(peer);
            if (inCons != null) {
                assert !inCons.isEmpty();
                var conAndId = inCons.getLast();
                sendWithListener(msg, peer, conAndId.getRight(), conAndId.getLeft());
            } else {
                listener.messageFailed(msg, peer, new IllegalArgumentException("No incoming connection"));
            }
        } else {
            listener.messageFailed(msg, peer, new IllegalArgumentException("Invalid connection: " + connection));
            logger.error("Invalid sendMessage mode " + connection);
        }
    }

    @Override
    protected void onSendMessage(T msg, byte[] peerIdentity, int connection) {
        var peerIdB = Bytes.of(peerIdentity);
        logger.debug("onSendMessage {} to {} {}", msg, peerIdB, connection == CONNECTION_IN ? "IN" : "OUT");

        if (connection <= CONNECTION_OUT) {
            IdentifiedConnectionState<T> conState = outIdConnections.get(peerIdB);
            if (conState != null)
                sendOutConMessage(conState, msg);
            else
                listener.messageFailed(msg, empty(), peerIdentity,
                        new IllegalArgumentException("No outgoing connection"));
        } else if (connection == CONNECTION_IN) {
            List<Pair<Connection<T>, Host>> inCons = inIdConnections.get(Bytes.of(peerIdentity));
            if (inCons != null) {
                assert !inCons.isEmpty();
                var conAndHost = inCons.getLast();
                sendWithListener(msg, conAndHost.getRight(), peerIdentity, conAndHost.getLeft());
            } else {
                listener.messageFailed(msg, empty(), peerIdentity,
                        new IllegalArgumentException("No incoming connection"));
            }
        } else {
            listener.messageFailed(msg, empty(), peerIdentity,
                    new IllegalArgumentException("Invalid connection: " + connection));
            logger.error("Invalid sendMessage mode " + connection);
        }
    }

    private void sendOutConMessage(IdentifiedConnectionState<T> conState, T msg) {
        if (conState.isConnected()) {
            sendWithListener(msg, conState.getPeerListenAddress(), conState.getPeerId(), conState.getConnection());
        } else {
            conState.getQueue().add(msg);
        }
    }

    private void sendWithListener(T msg, Host peer, byte[] peerId, Connection<T> established) {
        Promise<Void> promise = loop.newPromise();
        promise.addListener(future -> {
            if (future.isSuccess() && triggerSent)
                listener.messageSent(msg, peer, peerId);
            else if (!future.isSuccess())
                listener.messageFailed(msg, Optional.of(peer), peerId, future.cause());
        });
        established.sendMessage(msg, promise);
    }

    @Override
    protected void onOutboundConnectionUp(Connection<T> con) {
        var conState = outConnections.get(con.getConnectionId());
        if (conState == null || !conState.isAuthenticated())
            throw new AssertionError("Connection up with no authenticated connection state");

        Host peerHost = conState.getPeerListenAddress();
        byte[] peerId = conState.getPeerId();
        Bytes peerIdB = Bytes.of(peerId);

        logger.debug("OutboundConnectionUp {} ({})", peerHost, peerIdB);

        outIdConnections.put(peerIdB, conState);

        if (conState.isConnected()) {
            throw new AssertionError("ConnectionUp in CONNECTED state: " + con);
        } else {
            conState.setConnected();
            conState.getQueue().forEach(m -> sendWithListener(m, peerHost, peerId, con));
            conState.getQueue().clear();
            listener.deliverEvent(new SecureOutConnectionUp(peerHost, peerId));
        }
    }

    @Override
    protected void onCloseConnection(Host peer, int connection) {
        logger.debug("onCloseConnection {}. Closing all out connections to host...", peer);

        var connections = outHostConnections.remove(peer);
        if (connections != null) {
            for (var conState : connections) {
                outIdConnections.remove(Bytes.of(conState.getPeerId()));
                outConnections.remove(conState.getConnectionId());
                conState.disconnect();
            }
        }
    }

    @Override
    protected void onCloseConnection(byte[] peerId, int connection) {
        Bytes peerIdB = Bytes.of(peerId);
        logger.debug("onCloseConnection " + peerIdB);

        var conState = outIdConnections.remove(peerIdB);
        if (conState != null) {
            removeHostIdConnection(conState.getPeerListenAddress(), peerId);
            outConnections.remove(conState.getConnectionId());
            conState.disconnect();
        }
    }

    @Override
    protected void onOutboundConnectionDown(Connection<T> con, Throwable cause) {
        logger.debug("onOutboundConnectionDown {}" + con.getPeer() + (cause != null ? (" " + cause) : ""));

        var conState = outConnections.remove(con.getConnectionId());
        if (conState != null) {
            var peerId = conState.getPeerId();
            if (peerId != null) {
                outIdConnections.remove(Bytes.of(peerId));
                removeHostIdConnection(conState.getPeerListenAddress(), peerId);
            }

            // if (metrics)
            // oldOUt.add(Pair.of(con.getPeer(), conState));

            listener.deliverEvent(new SecureOutConnectionDown(conState.getPeerListenAddress(), conState.getPeerId(), cause));
        }
    }

    @Override
    protected void onOutboundConnectionFailed(Connection<T> conn, Throwable cause) {
        var conState = outConnections.remove(conn.getConnectionId());
        if (conState == null)
            return;

        Bytes peerIdB = Bytes.of(conState.getPeerId());

        logger.debug("OutboundConnectionFailed {} ({}){}", conn.getPeer(), peerIdB,
                (cause != null ? (" " + cause) : ""));

        if (peerIdB != null) {
            outIdConnections.remove(peerIdB);
            removeHostIdConnection(conState.getPeerListenAddress(), peerIdB.array());
        }

        // if (metrics)
        // oldOUt.add(Pair.of(conn.getPeer(), conState));

        listener.deliverEvent(new SecureOutConnectionFailed<>(conn.getPeer(), conState.getPeerId(),
                conState.getQueue(), cause));
    }

    @Override
    protected void onInboundConnectionUp(Connection<T> con) {
        Host clientSocket;
        try {
            clientSocket = con.getPeerAttributes().getHost(LISTEN_ADDRESS_ATTR);
        } catch (IOException e) {
            con.disconnect();
            throw new AssertionError(
                    "Inbound connection without valid listen address in connectionUp: " + e.getMessage());
        }

        byte[] peerId = con.getPeerAttributes().getBytes(ID_ATTR);
        assert peerId != null;
        Bytes peerIdB = Bytes.of(peerId);

        var idConList = inIdConnections.computeIfAbsent(peerIdB, k -> new LinkedList<>());
        idConList.add(Pair.of(con, clientSocket));

        var hostConList = inHostConnections.computeIfAbsent(clientSocket, k -> new LinkedList<>());
        hostConList.add(Pair.of(con, peerId));

        if (idConList.size() == 1 || hostConList.size() == 1) {
            logger.debug("InboundConnectionUp {} ({})", clientSocket, peerIdB);
            listener.deliverEvent(new SecureInConnectionUp(clientSocket, peerId));
        } else {
            logger.debug("Multiple InboundConnectionUp with {} ({})", clientSocket, peerIdB);
        }
    }

    @Override
    protected void onInboundConnectionDown(Connection<T> con, Throwable cause) {
        Host serverSocket;
        try {
            serverSocket = con.getPeerAttributes().getHost(LISTEN_ADDRESS_ATTR);
        } catch (IOException e) {
            con.disconnect();
            throw new AssertionError(
                    "Inbound connection without valid listen address in connectionUp: " + e.getMessage());
        }

        byte[] peerId = con.getPeerAttributes().getBytes(ID_ATTR);
        assert peerId != null;
        Bytes peerIdB = Bytes.of(peerId);

        var idConList = inIdConnections.get(peerIdB);
        var hostConList = inHostConnections.get(serverSocket);
        if (idConList == null || idConList.isEmpty() || hostConList == null || hostConList.isEmpty())
            throw new AssertionError("No connections in InboundConnectionDown " + serverSocket + " (" + peerIdB + ")");

        var idConIt = idConList.iterator();
        while (idConIt.hasNext() && !idConIt.next().getRight().equals(serverSocket))
            ;
        idConIt.remove();

        var hostConIt = hostConList.iterator();
        while (hostConIt.hasNext() && !Arrays.equals(hostConIt.next().getRight(), peerId))
            ;
        hostConIt.remove();

        if (idConList.isEmpty() || hostConList.isEmpty()) {
            logger.debug(() -> "InboundConnectionDown %s (%s)%s"
                    .formatted(serverSocket, peerIdB, (cause != null ? (" " + cause) : "")));

            if (idConList.isEmpty())
                inIdConnections.remove(peerIdB);
            if (hostConList.isEmpty())
                inHostConnections.remove(serverSocket);

            listener.deliverEvent(new SecureInConnectionDown(serverSocket, peerId, cause));
        } else {
            logger.debug("Extra InboundConnectionDown {}: {} or {}: {} remaining",
                    serverSocket, hostConList.size(), peerIdB, idConList.size());
        }

        if (metrics)
            oldIn.add(Pair.of(serverSocket, con));
    }

    @Override
    public void onServerSocketBind(boolean success, Throwable cause) {
        if (success)
            logger.debug("Server socket ready");
        else
            logger.error("Server socket bind failed: " + cause);
    }

    @Override
    public void onServerSocketClose(boolean success, Throwable cause) {
        logger.debug("Server socket closed. " + (success ? "" : "Cause: " + cause));
    }

    // -------------- ------------- MessageListener

    @Override
    public void onDeliverMessage(T msg, Connection<T> conn) {
        byte[] peerId = conn.getPeerAttributes().getBytes(ID_ATTR);
        assert peerId != null;

        Host host = conn.getPeer();
        if (conn.isInbound()) {
            try {
                host = conn.getPeerAttributes().getHost(LISTEN_ADDRESS_ATTR);
            } catch (IOException e) {
                conn.disconnect();
                throw new AssertionError(
                        "Inbound connection without valid listen address in deliver message: " + e.getMessage());
            }
        }

        logger.debug("DeliverMessage {} {} ({}) {}", msg, host, (conn.isInbound() ? "IN" : "OUT"));
        listener.deliverMessage(msg, host, peerId);
    }

}
