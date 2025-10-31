package pt.unl.fct.di.novasys.channel.secure.auth;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static pt.unl.fct.di.novasys.channel.secure.auth.AuthSession.ID_ATTR;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Promise;
import pt.unl.fct.di.novasys.channel.secure.SecureChannelListener;
import pt.unl.fct.di.novasys.channel.secure.SecureSingleThreadedBiChannel;
import pt.unl.fct.di.novasys.channel.secure.auth.AuthSession.State;
import pt.unl.fct.di.novasys.channel.secure.events.SecureInConnectionDown;
import pt.unl.fct.di.novasys.channel.secure.events.SecureInConnectionUp;
import pt.unl.fct.di.novasys.channel.secure.events.SecureOutConnectionDown;
import pt.unl.fct.di.novasys.channel.secure.events.SecureOutConnectionFailed;
import pt.unl.fct.di.novasys.channel.secure.events.SecureOutConnectionUp;
import pt.unl.fct.di.novasys.channel.secure.exceptions.AuthenticationException;
import pt.unl.fct.di.novasys.channel.secure.exceptions.MessageAuthenticationException;
import pt.unl.fct.di.novasys.channel.secure.utils.ECPubKeySerializer;
import pt.unl.fct.di.novasys.channel.secure.utils.X509CertificateSerializer;
import pt.unl.fct.di.novasys.network.AttributeValidator;
import pt.unl.fct.di.novasys.network.Connection;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.NetworkManager;
import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.exceptions.InvalidHandshakeAttributesException;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;
import pt.unl.fct.di.novasys.network.data.Bytes;

/**
 * Authenticates connections with certificates
 */
public class AuthChannel<T> extends SecureSingleThreadedBiChannel<T, AuthenticatedMessage>
        implements AttributeValidator {

    private static final Logger logger = LogManager.getLogger(AuthChannel.class);

    public static final short CHANNEL_MAGIC_NUMBER = 0x5505;
    public final static String NAME = "AuthChannel";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";
    public final static String WORKER_GROUP_KEY = "worker_group";
    public final static String TRIGGER_SENT_KEY = "trigger_sent";
    public final static String METRICS_INTERVAL_KEY = "metrics_interval";
    public final static String HEARTBEAT_INTERVAL_KEY = "heartbeat_interval";
    public final static String HEARTBEAT_TOLERANCE_KEY = "heartbeat_tolerance";
    public final static String CONNECT_TIMEOUT_KEY = "connect_timeout";

    public static final String LISTEN_ADDRESS_ATTR = "listen_address";
    public static final String CHANNELMAGIC_ATTR = "magic_number";

    public final static String DEFAULT_PORT = "9573";
    public final static String DEFAULT_HB_INTERVAL = "0";
    public final static String DEFAULT_HB_TOLERANCE = "0";
    public final static String DEFAULT_CONNECT_TIMEOUT = "1000";
    public final static String DEFAULT_METRICS_INTERVAL = "-1";

    public final static int CONNECTION_OUT = 0;
    public final static int CONNECTION_IN = 1;

    private static final int HANDSHAKE_STEPS = 3;

    static final Provider PROVIDER = new BouncyCastleProvider();

    private final SecureRandom rng;

    // private final DefaultEventExecutor loop;

    private final NetworkManager<AuthenticatedMessage> network;
    private final Attributes baseAttributes;

    private final ISerializer<T> msgSerializer;
    private final SecureChannelListener<T> listener;
    private final X509IKeyManager keyManager;
    private final X509ITrustManager trustManager;

    private final boolean metrics;

    private final Map<Host, Set<Bytes>> hostIds;
    private final Map<Host, Bytes> defaultHostIds;

    private final Map<Long, AuthSession<T>> allSessions;
    private final Map<Bytes, Map<Long, AuthSession<T>>> inSessions;
    private final Map<Bytes, AuthSession<T>> outSessions;
    private final Map<Host, AuthSession<T>> pendingOutSessionsWithoutId;

    public AuthChannel(ISerializer<T> serializer, SecureChannelListener<T> listener, Properties properties,
            X509IKeyManager keyManager, X509ITrustManager trustManager) throws IOException {
        super(NAME);

        SecureRandom rngInst;
        try {
            rngInst = SecureRandom.getInstance("DEFAULT", PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Failed to get \"DEFAULT\" secure random");
            rngInst = new SecureRandom();
        }
        rng = rngInst;

        this.msgSerializer = serializer;
        this.listener = listener;
        this.keyManager = keyManager;
        this.trustManager = trustManager;

        hostIds = new HashMap<>();
        defaultHostIds = new HashMap<>();
        inSessions = new HashMap<>();
        outSessions = new HashMap<>();
        allSessions = new HashMap<>();
        pendingOutSessionsWithoutId = new HashMap<>();

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
        // this.triggerSent =
        // Boolean.parseBoolean(properties.getProperty(TRIGGER_SENT_KEY, "false"));
        this.metrics = metricsInterval > 0;

        Host listenAddress = new Host(addr, port);

        EventLoopGroup eventExecutors = properties.containsKey(WORKER_GROUP_KEY)
                ? (EventLoopGroup) properties.get(WORKER_GROUP_KEY)
                : NetworkManager.createNewWorkerGroup();

        baseAttributes = new Attributes();
        baseAttributes.putShort(CHANNEL_MAGIC_ATTRIBUTE, CHANNEL_MAGIC_NUMBER);
        baseAttributes.putHost(LISTEN_ADDRESS_ATTR, listenAddress);

        // TODO for now, set the serializer with the default SHA256 size (i.e., don't
        // negotiate the hashing algorithms in the handshake)
        network = new NetworkManager<>(HANDSHAKE_STEPS,
                AuthenticatedMessage.getSerializer(MAC_BYTES), this, hbInterval, hbTolerance, connTimeout,
                eventExecutors);
        network.createServerSocket(this, listenAddress, baseAttributes, this, eventExecutors);

        if (metrics) {
            // TODO metrics?
            // oldIn = new LinkedList<>();
            // oldOUt = new LinkedList<>();
            loop.scheduleAtFixedRate(this::triggerMetricsEvent,
                    metricsInterval, metricsInterval, TimeUnit.MILLISECONDS);
        }
    }

    void triggerMetricsEvent() {
        // TODO metrics?
        // listener.deliverEvent(new ChannelMetrics(oldIn, oldOUt, inConnections,
        // outConnections));
    }

    // ------------ Secure handshake

    // TODO make the algorithms customizable and agreed upon in the handshake
    static final String ASYM_KEY_ALG = "RSA";
    static final String SYM_KEY_ALG = "AES";
    static final String MAC_ALG = "HmacSHA256";
    static final int MAC_BYTES = 256 / 8;
    // TODO think better about this
    private static final String EC_KDF_ALG = "ECCDHwithSHA256KDF";
    private static final String DH_EC_NAME = "prime192v1";

    private static final String CERT_ATTR = "certificate";
    private static final String DH_PUB_ATTR = "dh_pub";
    private static final String EXPECTED_ID_ATTR = "expected_identity";
    private static final String IV_ATTR = "iv";
    private static final String IV_SIG_ATTR = "iv_sig";
    private static final String ATTRS_SIG_ATTR = "attrs_sig";

    private Attributes createFirstHandshakeAttributes(ECPublicKey ecPubKey, byte[] iv, String idAlias,
            Optional<byte[]> expectedId)
            throws CertificateEncodingException, IOException, InvalidKeyException, SignatureException {
        try {
            var attrs = baseAttributes.shallowClone();

            attrs.putBytes(ID_ATTR, keyManager.getAliasId(idAlias));

            var cert = keyManager.getCertificateChain(idAlias)[0];
            attrs.putObject(CERT_ATTR, cert, X509CertificateSerializer.INSTANCE);

            attrs.putObject(DH_PUB_ATTR, ecPubKey, ECPubKeySerializer.INSTANCE);

            expectedId.ifPresent(id -> attrs.putBytes(EXPECTED_ID_ATTR, id));

            attrs.putBytes(IV_ATTR, iv);

            ByteBuf attrsBytes = Unpooled.buffer();
            Attributes.serializer.serialize(attrs, attrsBytes);

            var sig = Signature.getInstance(cert.getSigAlgName(), PROVIDER);
            sig.initSign(keyManager.getPrivateKey(idAlias));
            sig.update(attrsBytes.array());

            attrs.putBytes(ATTRS_SIG_ATTR, sig.sign());

            return attrs;
        } catch (NoSuchAlgorithmException e) {
            // Shouldn't happen
            throw new RuntimeException(e);
        }
    }

    private byte[] signWithCert(X509Certificate cert, PrivateKey privKey, byte[] tbsBytes)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        var sig = Signature.getInstance(cert.getSigAlgName(), PROVIDER);
        sig.initSign(privKey);
        sig.update(tbsBytes);
        return sig.sign();
    }

    private Attributes createSecondHandshakeAttributes(String idAlias, ECPublicKey dhPubKey, byte[] iv, byte[] peerIv)
            throws CertificateEncodingException, IOException, InvalidKeyException, SignatureException {
        try {
            var attrs = baseAttributes.shallowClone();

            attrs.putBytes(ID_ATTR, keyManager.getAliasId(idAlias));

            var cert = keyManager.getCertificateChain(idAlias)[0];
            attrs.putObject(CERT_ATTR, cert, X509CertificateSerializer.INSTANCE);

            PrivateKey signingKey = keyManager.getPrivateKey(idAlias);
            byte[] signedPeerIv = signWithCert(cert, signingKey, peerIv);
            attrs.putBytes(IV_SIG_ATTR, signedPeerIv);

            attrs.putObject(DH_PUB_ATTR, dhPubKey, ECPubKeySerializer.INSTANCE);

            attrs.putBytes(IV_ATTR, iv);

            ByteBuf attrsBytes = Unpooled.buffer();
            Attributes.serializer.serialize(attrs, attrsBytes);

            var sig = Signature.getInstance(cert.getSigAlgName(), PROVIDER);
            sig.initSign(keyManager.getPrivateKey(idAlias));
            sig.update(attrsBytes.array());

            attrs.putBytes(ATTRS_SIG_ATTR, sig.sign());

            return attrs;
        } catch (NoSuchAlgorithmException | NullPointerException e) {
            // Shouldn't happen
            throw new RuntimeException(e);
        }
    }

    private KeyPair generateECKeyPair() {
        try {
            var start = Instant.now();

            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("EC", PROVIDER);
            keyPairGen.initialize(ECNamedCurveTable.getParameterSpec(DH_EC_NAME), rng);
            KeyPair keyPair = keyPairGen.generateKeyPair();

            if (logger.isDebugEnabled()) {
                Instant end = Instant.now();
                logger.debug(() -> "Generated EC key pair in %sms (%sns)"
                        .formatted(ChronoUnit.MILLIS.between(start, end), ChronoUnit.NANOS.between(start, end)));
            }

            return keyPair;
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e); // Should never happen.
        }
    }

    private SecretKey generateAESFromECKeys(PrivateKey myPriv, PublicKey peerPub) throws InvalidKeyException {
        try {
            Instant start = logger.isDebugEnabled() ? Instant.now() : null;

            var keyAgreement = KeyAgreement.getInstance(EC_KDF_ALG, PROVIDER);
            keyAgreement.init(myPriv, rng);
            keyAgreement.doPhase(peerPub, true);
            SecretKey secretKey = keyAgreement.generateSecret(SYM_KEY_ALG);

            if (logger.isDebugEnabled()) {
                Instant end = Instant.now();
                logger.debug("Generated {} secret key from ECDH in {}ms ({}ns): {}", SYM_KEY_ALG,
                        ChronoUnit.MILLIS.between(start, end), ChronoUnit.NANOS.between(start, end),
                        Hex.toHexString(secretKey.getEncoded()));

            }

            return secretKey;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Should never happen.
        }
    }

    private byte[] generateIv(int size) {
        byte[] iv = new byte[size];
        rng.nextBytes(iv);
        return iv;
    }

    private AuthSession<T> createOutSession(Host peer, Optional<byte[]> expectedId)
            throws CertificateEncodingException, InvalidKeyException, SignatureException, IOException,
            InvalidAlgorithmParameterException {
        var idAlias = keyManager.chooseClientAlias(new String[] { ASYM_KEY_ALG }, null, null);

        var ecKeyPair = generateECKeyPair();
        var iv = generateIv(MAC_BYTES);
        var attrs = createFirstHandshakeAttributes((ECPublicKey) ecKeyPair.getPublic(), iv, idAlias, expectedId);

        var conn = network.createConnection(peer, attrs, this, this);
        var session = AuthSession.startOutSession(peer, conn, msgSerializer, idAlias, ecKeyPair, iv, expectedId);
        allSessions.put(conn.getConnectionId(), session);

        return session;
    }

    // -------------- AttributeValidator

    @Override
    public boolean validateAttributes(Attributes attr) {
        Short channel = attr.getShort(CHANNEL_MAGIC_ATTRIBUTE);
        return (channel != null && channel == CHANNEL_MAGIC_NUMBER);
    }

    @Override
    public Attributes getSecondHandshakeAttributes(long channelId, Attributes peerAttr, Attributes myAttr)
            throws InvalidHandshakeAttributesException {
        try {
            return loop.submit(() -> onGetSecondHandshakeAttributes(channelId, peerAttr, myAttr)).get();
        } catch (ExecutionException e) {
            throw (InvalidHandshakeAttributesException) e.getCause();
        } catch (InterruptedException e) {
            throw new InvalidHandshakeAttributesException(peerAttr, 1, e);
        }
    }

    private Attributes onGetSecondHandshakeAttributes(long channelId, Attributes peerAttr, Attributes myAttr)
            throws InvalidHandshakeAttributesException {
        logger.debug("Validating in connection attribute and creating reply attributes...");

        try {
            Host peerSocket = peerAttr.getHost(LISTEN_ADDRESS_ATTR);
            byte[] peerIv = peerAttr.getBytes(IV_ATTR);

            if (!validateAttributes(peerAttr) || peerSocket == null || peerIv == null)
                throw new InvalidHandshakeAttributesException(peerAttr, "First handshake: missing attributes");

            X509Certificate peerCert = peerAttr.getObject(CERT_ATTR, X509CertificateSerializer.INSTANCE);
            PublicKey peerPubKey = peerCert.getPublicKey();

            byte[] peerId = trustManager.extractIdFromCertificate(peerCert);
            if (!Arrays.equals(peerId, peerAttr.getBytes(ID_ATTR))) {
                logger.debug(
                        "In connection attribute validation failed: peer id in attributes ({}) differs from the one extracted from certificate ({})",
                        Bytes.of(peerId), Bytes.of(peerAttr.getBytes(ID_ATTR)));
                throw new InvalidHandshakeAttributesException(peerAttr, 1);
            }

            trustManager.checkClientTrusted(new X509Certificate[] { peerCert }, peerCert.getPublicKey().getAlgorithm());

            // Verify handshake attributes signature
            if (!verifyAttrSignature(peerAttr, peerPubKey, peerCert.getSigAlgName())) {
                logger.debug("In connection attribute validation failed: Invalid attributes signature");
                throw new InvalidHandshakeAttributesException(peerAttr, 1);
            }

            // Complete Diffie-Hellman
            ECPublicKey peerDHPubKey = peerAttr.getObject(DH_PUB_ATTR, ECPubKeySerializer.INSTANCE);

            // Create pending session and attrs for second handshake
            byte[] requestedId = peerAttr.getBytes(EXPECTED_ID_ATTR);
            String idAlias = requestedId == null
                    ? keyManager.chooseServerAlias(ASYM_KEY_ALG, null, null)
                    : (idAlias = keyManager.getIdAlias(requestedId)) == null
                            ? keyManager.chooseServerAlias(ASYM_KEY_ALG, null, null)
                            : idAlias;

            var ecKeyPair = generateECKeyPair();
            var secretKey = generateAESFromECKeys(ecKeyPair.getPrivate(), peerDHPubKey);
            var iv = generateIv(MAC_BYTES);
            var attrs = createSecondHandshakeAttributes(idAlias, (ECPublicKey) ecKeyPair.getPublic(), iv, peerIv);

            var session = AuthSession.startInSession(peerSocket, msgSerializer, idAlias, ecKeyPair,
                                                     secretKey, iv, peerId, peerIv);
            allSessions.put(channelId, session);
            inSessions.computeIfAbsent(Bytes.of(peerId), __ -> new HashMap<>()).put(channelId, session);

            return attrs;
        } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException | IOException
                | CertificateException | NullPointerException e) {
            logger.debug("In connection attribute validation failed with exception: " + e);
            throw new InvalidHandshakeAttributesException(peerAttr, 1, e);
        }
    }

    @Override
    public Attributes getNthHandshakeAttributes(long connectionId, int handshakeN,
            List<Attributes> peerAttrs, List<Attributes> mySentAttrs)
            throws InvalidHandshakeAttributesException {
        try {
            return loop.submit(() -> onGetNthHandshakeAttributes(connectionId, handshakeN, peerAttrs, mySentAttrs)).get();
        } catch (ExecutionException e) {
            throw (InvalidHandshakeAttributesException) e.getCause();
        } catch (InterruptedException e) {
            throw new InvalidHandshakeAttributesException(peerAttrs.getLast(), 1, e);
        }
    }

    public Attributes onGetNthHandshakeAttributes(long connectionId, int handshakeN,
            List<Attributes> peerAttrs, List<Attributes> mySentAttrs)
            throws InvalidHandshakeAttributesException {
        var invalidHandshakeException = new InvalidHandshakeAttributesException(peerAttrs.getLast(), handshakeN - 1);

        if (!validateAttributes(peerAttrs.getLast()))
            throw invalidHandshakeException;

        try {
            switch (handshakeN) {
                case 3 -> {
                    logger.trace("Getting 3rd handshake message...");

                    var session = allSessions.get(connectionId);
                    var secondHs = peerAttrs.getLast();

                    // Verify peer certificate and identity
                    var peerCert = secondHs.getObject(CERT_ATTR, X509CertificateSerializer.INSTANCE);

                    Bytes peerId = Bytes.of(secondHs.getBytes(ID_ATTR));
                    byte[] expectedId = secondHs.getBytes(EXPECTED_ID_ATTR);

                    String authType = peerCert.getPublicKey().getAlgorithm();
                    if (expectedId != null) {
                        if (!peerId.equals(expectedId))
                        throw new AuthenticationException("Expected peer id %s, but got peerId %s"
                                .formatted(Bytes.of(expectedId), peerId));
                        trustManager.checkServerTrusted(new X509Certificate[] { peerCert }, expectedId, authType);
                    } else {
                        trustManager.checkServerTrusted(new X509Certificate[] { peerCert }, authType);
                    }

                    // Verify attributes signature
                    // TODO use getSigAlgOID instead? It's not supported by default Java providers but i think it is by BC
                    verifyAttrSignature(secondHs, peerCert.getPublicKey(), peerCert.getSigAlgName());

                    // Verify signature of my IV
                    byte[] signedIv = secondHs.getBytes(IV_SIG_ATTR);
                    if (!verifySignature(peerCert, signedIv, session.getMyLastMac()))
                        throw invalidHandshakeException;

                    // Complete session setup
                    byte[] peerIv = secondHs.getBytes(IV_ATTR);

                    ECPublicKey peerDHPubKey = secondHs.getObject(DH_PUB_ATTR, ECPubKeySerializer.INSTANCE);
                    SecretKey sessionKey = generateAESFromECKeys(session.getDhKeyPair().getPrivate(), peerDHPubKey);

                    session.completeOutSessionSetup(peerId.array(), sessionKey, peerIv);

                    // Build 3rd handshake attrs
                    var thirdHs = baseAttributes.shallowClone();

                    var myCert = keyManager.getCertificateChain(session.getMyIdAlias())[0];
                    var myPrivKey = keyManager.getPrivateKey(session.getMyIdAlias());
                    byte[] signedPeerIv = signWithCert(myCert, myPrivKey, peerIv);

                    thirdHs.putBytes(IV_SIG_ATTR, signedPeerIv);

                    return thirdHs;
                }
                case 4 -> {
                    logger.trace("Validating 3rd handshake message...");

                    var session = allSessions.get(connectionId);
                    var firstHs = peerAttrs.getFirst();
                    var thirdHs = peerAttrs.getLast();

                    // Verify signature of my IV
                    var peerCert = firstHs.getObject(CERT_ATTR, X509CertificateSerializer.INSTANCE);
                    var signedIv = thirdHs.getBytes(IV_SIG_ATTR);

                    if (verifySignature(peerCert, signedIv, session.getMyLastMac())) {
                        return Attributes.EMPTY; // Success
                    } else {
                        throw invalidHandshakeException;
                    }
                }
                default -> throw invalidHandshakeException;
            }
        } catch (NullPointerException | AuthenticationException | IOException | CertificateException
                | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new InvalidHandshakeAttributesException(peerAttrs.getLast(), handshakeN - 1, e);
        }
    }

    private boolean verifyAttrSignature(Attributes peerAttr, PublicKey peerPubKey, String sigAlg)
            throws NoSuchAlgorithmException, IOException {
        try {
            Attributes attrWithoutSignature = peerAttr.shallowClone();
            attrWithoutSignature.remove(ATTRS_SIG_ATTR);

            var sig = Signature.getInstance(sigAlg, PROVIDER);
            sig.initVerify(peerPubKey);

            var attrBytes = Unpooled.buffer();
            Attributes.serializer.serialize(attrWithoutSignature, attrBytes);

            sig.update(attrBytes.array());

            return sig.verify(peerAttr.getBytes(ATTRS_SIG_ATTR));
        } catch (SignatureException | InvalidKeyException e) {
            return false;
        }
    }

    private boolean verifySignature(X509Certificate cert, byte[] signature, byte[] unsignedBytes)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        var sig = Signature.getInstance(cert.getSigAlgName(), PROVIDER);
        sig.initVerify(cert);
        sig.update(unsignedBytes);
        return sig.verify(signature);
    }

    // -------------- SecureSingleThreadedBiChannel

    // -------------- ------------- MessageListener

    @Override
    protected void onDeliverMessage(AuthenticatedMessage authMsg, Connection<AuthenticatedMessage> conn) {
        Bytes peerId = Bytes.of(conn.getPeerAttributes().getBytes(ID_ATTR));
        if (peerId == null) {
            try {
                T droppedMsg = msgSerializer.deserialize(Unpooled.wrappedBuffer(authMsg.getData()));
                logger.error("onDeliverMessage error: No identity associated with connection to host {}. Dropping recvd msg: {}",
                        conn.getPeer(), droppedMsg);
            } catch (IOException e) {
                logger.error("onDeliverMessage error: No identity associated with connection to host {}", conn.getPeer());
            }
            conn.disconnect();
            return;
        }

        AuthSession<T> session = allSessions.get(conn.getConnectionId());
        /*
        AuthSession<T> session = conn.isInbound()
                ? inSessions.get(peerId).get(conn.getConnectionId())
                : outSessions.get(peerId);
        */
        if (session == null) {
            try {
                T droppedMsg = msgSerializer.deserialize(Unpooled.wrappedBuffer(authMsg.getData()));
                logger.error("onDeliverMessage error: No session with peer {}. Dropping recvd msg: {}",
                        conn.getPeer(), droppedMsg);
            } catch (IOException e) {
                logger.error("onDeliverMessage error: No session with peer {}", conn.getPeer());
            }
            conn.disconnect();
            return;
        }

        Host host = session.getPeerSocket();
        try {
            T msg = session.receiveMessage(authMsg);
            logger.debug("onDeliverMessage from: {} ({})", host, peerId);
            listener.deliverMessage(msg, host, peerId.array());
        } catch (InvalidKeyException | NoSuchAlgorithmException | MessageAuthenticationException
                | IOException e) {
            logger.error("onDeliverMessage error: Exception on receiving message from {} ({})", host, peerId);
            e.printStackTrace();
        }
    }

    // -------------- ------------- IChannel

    @Override
    protected void onSendMessage(T msg, Host host, int connection) {
        var peerId = defaultHostIds.get(host);
        if (peerId == null && connection <= CONNECTION_OUT) {
            var session = pendingOutSessionsWithoutId.get(host);
            if (session == null) {
                logger.debug("onSendMessage ignored: No connection to {}", host);
                listener.messageFailed(msg, host, new IllegalStateException("No connection to " + host));
            } else {
                assert session.getState() == State.CONNECTING;
                session.enqueue(msg);
            }
        } else {
            onSendMessage(msg, peerId.array(), connection);
        }
    }

    @Override
    protected void onCloseConnection(Host peer, int connection) {
        var peerId = defaultHostIds.get(peer);
        if (peerId == null)
            logger.debug("onCloseConnection ignored: No open connection to {}", peer);
        else
            onCloseConnection(peerId.array(), connection);
    }

    @Override
    protected void onOpenConnection(Host peer, int connection) {
        var knownIds = hostIds.get(peer);
        if (pendingOutSessionsWithoutId.containsKey(peer)
            || ( knownIds != null && knownIds.stream().anyMatch(id -> outSessions.containsKey(id)) ))
            logger.debug("onOpenConnection ignored: A default connection for {} already exists", peer);
        else
            onOpenConnection(peer, null, connection);
    }

    // -------------- ------------- SecureIChannel

    /**
     * Gets a session to a peer. If it's an inbound connection, will get any of
     * them.
     */
    private AuthSession<T> getSessionToSend(Bytes peerId, int connection) {
        if (connection == CONNECTION_IN) {
            var sessions = inSessions.get(peerId);
            if (sessions != null)
                return sessions.values().stream().findAny().orElse(null);
        } else if (connection <= CONNECTION_OUT) {
            return outSessions.get(peerId);
        }

        return null;
    }

    private Bytes getPeerId(Connection<?> connection) {
        Attributes peerAttrs = connection.getPeerAttributes();
        byte[] advertisedId = peerAttrs != null ? peerAttrs.getBytes(ID_ATTR) : null;
        return Bytes.of(advertisedId != null ? advertisedId : connection.getSelfAttributes().getBytes(EXPECTED_ID_ATTR));
    }

    private void addHostId(Host host, Bytes id) {
        hostIds.computeIfAbsent(host, k -> {
            defaultHostIds.put(host, id);
            return new HashSet<>();
        }).add(id);
    }

    /**
     * Removes {@code id} from the given {@code host} in {@value #hostIds} if that
     * id isn't used for that host in any active in or out sessions.
     * <p>
     * If the removed id was the host's default id, substitute it for another known
     * id of that host, if available.
     */
    private void pruneHostId(Host host, Bytes id) {
        if ((outSessions.containsKey(id) && host.equals(outSessions.get(id).getPeerSocket()))
                || (inSessions.containsKey(id) &&
                        inSessions.get(id).values().stream()
                                .anyMatch(s -> s.getPeerSocket().equals(host))))
            return;

        Set<Bytes> ids = hostIds.get(host);
        if (ids == null)
            return;
        ids.remove(id);

        if (id.equals(defaultHostIds.get(host))) {
            if (ids.size() > 0) {
                defaultHostIds.put(host, ids.iterator().next());
            } else {
                defaultHostIds.remove(host);
                hostIds.remove(host);
            }
        }
    }

    @Override
    public void onSendMessage(T msg, byte[] peerId, int connection) {
        var idBytes = Bytes.of(peerId);
        var session = getSessionToSend(idBytes, connection);
        if (session == null) {
            logger.debug("onSendMessage: No session with peer {}. Dropping msg: {}", idBytes, msg);
            listener.messageFailed(msg, empty(), peerId, new IllegalArgumentException("No connection to " + idBytes));
            return;
        }

        Host peerSocket = session.getPeerSocket();

        logger.debug("onSendMessage: Sending message {} to {} ({})", msg, peerSocket, idBytes);
        switch (session.getState()) {
            case State.CONNECTED -> sendWithListener(session, msg, peerSocket, peerId);
            case State.CONNECTING -> session.getMsgQueue().add(msg);
            case State.DISCONNECTING -> listener.messageFailed(msg, of(peerSocket), peerId, new IllegalStateException("Channel state was DISCONNECTING"));
        }

    }

    private void sendWithListener(AuthSession<T> session, T msg, Host peer, byte[] peerId) {
        Promise<Void> promise = loop.newPromise();
        promise.addListener(future -> {
            if (future.isSuccess())
                listener.messageSent(msg, peer, peerId);
            else if (!future.isSuccess())
                listener.messageFailed(msg, Optional.of(peer), peerId, future.cause());
        });

        try {
            session.macAndSend(msg, promise);
        } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            logger.warn("Message MAC failed.");
            listener.messageFailed(msg, of(peer), peerId, e);
        }
    }

    @Override
    public void onCloseConnection(byte[] peerId, int connection) {
        var idBytes = Bytes.of(peerId);
        var session = outSessions.get(idBytes);
        if (session == null) { // || session.getState() == State.DISCONNECTING) {
            logger.debug("onCloseConnection ignored: No out connection to {}", idBytes);
        } else {
            logger.debug("onCloseConnection: {} ({})", session.getPeerSocket(), idBytes);
            session.disconect();
            outSessions.remove(idBytes);
            allSessions.remove(session.getConnectionId());
            pruneHostId(session.getPeerSocket(), idBytes);
        }
    }

    @Override
    public void onOpenConnection(Host peer, byte[] peerId, int connection) {
        try {
            var idBytes = Bytes.of(peerId);

            if (outSessions.containsKey(idBytes)) {
                logger.debug("onOpenConnection ignored: Repeated connection to {} ({})", peer, idBytes);
                return;
            }

            logger.debug("onOpenConnection opening session to: {} ({})", peer, peerId);

            AuthSession<T> session = createOutSession(peer, ofNullable(peerId));
            if (peerId == null) {
                pendingOutSessionsWithoutId.put(peer, session);
            } else {
                outSessions.put(idBytes, session);
                addHostId(peer, idBytes);
            }
        } catch (CertificateEncodingException | InvalidKeyException | SignatureException | IOException
                | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            this.onOutboundConnectionFailed(null, e);
        }
    }

    // -------------- ------------- InConnListener

    @Override
    protected void onInboundConnectionUp(Connection<AuthenticatedMessage> conn) {
        var session = allSessions.get(conn.getConnectionId());
        if (session == null) {
            logger.warn("InboundConnectionUp with no prepared session.");
            conn.disconnect();
        }

        session.completeInSessionSetup(conn);
        session.setState(State.CONNECTED);

        Host host = session.getPeerSocket();
        Bytes peerIdBytes = Bytes.of(session.getPeerId());

        logger.debug("InboundConnectionUp with {} ({})", host, peerIdBytes);

        addHostId(host, peerIdBytes);

        listener.deliverEvent(new SecureInConnectionUp(host, session.getPeerId()));
    }

    @Override
    protected void onInboundConnectionDown(Connection<AuthenticatedMessage> con, Throwable cause) {
        Bytes peerId = getPeerId(con);
        Host host;
        try {
            host = con.getPeerAttributes().getHost(LISTEN_ADDRESS_ATTR);
        } catch (IOException e) {
            host = con.getPeer();
        }
        logger.debug("Inbound connection down with {} ({})", host, peerId);

        inSessions.remove(peerId);
        allSessions.remove(con.getConnectionId());
        pruneHostId(host, peerId);

        listener.deliverEvent(new SecureInConnectionDown(host, peerId, cause));
    }

    @Override
    protected void onServerSocketBind(boolean success, Throwable cause) {
        if (success)
            logger.debug("Server socket ready");
        else
            logger.error("Server socket bind failed: {}", cause);
    }

    @Override
    protected void onServerSocketClose(boolean success, Throwable cause) {
        if (success)
            logger.debug("Server socket closed.");
        else
            logger.error("Server socket closed. Cause: {}", cause);
    }

    // -------------- ------------- OutConnListener

    @Override
    protected void onOutboundConnectionUp(Connection<AuthenticatedMessage> conn) {
        var session = allSessions.get(conn.getConnectionId());
        if (session == null) {
            logger.warn("OutboundConnectionUp with no prepared session.");
            conn.disconnect();
            return;
        }

        Host host = session.getPeerSocket();
        Bytes peerId = Bytes.of(session.getPeerId());

        logger.debug("OutboundConnectionUp with {} ({})", host, peerId);

        if (!outSessions.containsKey(peerId)) {
            pendingOutSessionsWithoutId.remove(host);
            outSessions.put(peerId, session);
            addHostId(host, peerId);
        }

        // Send all pending messages
        Queue<T> msgQueue = session.getMsgQueue();
        while (!msgQueue.isEmpty())
        sendWithListener(session, msgQueue.remove(), host, peerId.array());
        session.setState(State.CONNECTED);

        listener.deliverEvent(new SecureOutConnectionUp(host, peerId));
    }

    @Override
    protected void onOutboundConnectionDown(Connection<AuthenticatedMessage> conn, Throwable cause) {
        Bytes peerId = getPeerId(conn);
        logger.debug("OutboundConnectionDown with {} ({}).{}", conn.getPeer(), peerId,
                cause == null ? "" : "\nCause: " + cause);

        outSessions.remove(peerId);
        AuthSession<T> session = allSessions.remove(conn.getConnectionId());
        if (session == null)
            return;
        else if (session.getState() == State.CONNECTING)
            throw new AssertionError("ConnectionDown in CONNECTING session state: " + conn);

        Host host = conn.getPeer();
        pruneHostId(host, peerId);

        listener.deliverEvent(new SecureOutConnectionDown(host, peerId, cause));
    }

    @Override
    protected void onOutboundConnectionFailed(Connection<AuthenticatedMessage> conn, Throwable cause) {
        var session = allSessions.remove(conn.getConnectionId());
        if (session == null) {
            logger.debug("OutboundConnectionFailed to {}.{}", conn.getPeer(),
                    cause == null ? "" : "\nCause: " + cause);
            listener.deliverEvent(new SecureOutConnectionFailed<T>(conn.getPeer(),
                    new byte[0], new LinkedList<>(), cause));
            return;
        }

        Host host = session.getPeerSocket();
        byte[] peerId = (peerId = session.getPeerId()) != null ? peerId : new byte[0];
        Bytes idBytes = Bytes.of(peerId);
        logger.debug("OutboundConnectionFailed to {} ({}).{}", host, idBytes, cause == null ? "" : "\nCause: " + cause);

        if (outSessions.remove(idBytes) == null)
            pendingOutSessionsWithoutId.remove(host);

        listener.deliverEvent(new SecureOutConnectionFailed<>(host, peerId, session.getMsgQueue(), cause));
    }

}
