package pt.unl.fct.di.novasys.channel.secure.auth;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Promise;
import pt.unl.fct.di.novasys.channel.secure.exceptions.AuthenticationException;
import pt.unl.fct.di.novasys.channel.secure.exceptions.MessageAuthenticationException;
import pt.unl.fct.di.novasys.network.Connection;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Bytes;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * NOT THREAD SAFE
 */
public class AuthSession<T> {

    private static final Logger logger = LogManager.getLogger(AuthSession.class);

    public enum State {
        CONNECTING, CONNECTED, DISCONNECTING
    }

    static final String ID_ATTR = "identity";

    private static final String DEFAULT_MAC_ALG = AuthChannel.MAC_ALG;
    private String macAlgorithm = DEFAULT_MAC_ALG;

    private Connection<AuthenticatedMessage> connection;

    private final Queue<T> msgQueue;
    private final ISerializer<T> serializer;

    private State state;

    private final KeyPair dhKeyPair;

    private final String myIdAlias;
    private final Host peerSocket;

    private byte[] peerId;

    private SecretKey sessionKey;
    private byte[] myLastMac;
    private byte[] peerLastMac;

    private AuthSession(Host peerSocket, ISerializer<T> serializer, String myIdAlias, KeyPair dhKeyPair, byte[] myIv) {
        this.peerSocket = peerSocket;
        this.serializer = serializer;
        this.myIdAlias = myIdAlias;
        this.dhKeyPair = dhKeyPair;
        this.myLastMac = myIv;

        this.state = State.CONNECTING;
        this.msgQueue = new LinkedList<>();
    }

    public static <T> AuthSession<T> startOutSession(Host peerSocket, Connection<AuthenticatedMessage> connection,
            ISerializer<T> serializer, String myIdAlias, KeyPair dhKeyPair, byte[] myIv,
            Optional<byte[]> expectedPeerId) {
        var session = new AuthSession<>(peerSocket, serializer, myIdAlias, dhKeyPair, myIv);

        expectedPeerId.ifPresent(id -> session.peerId = id);
        session.connection = connection;

        return session;
    }

    public static <T> AuthSession<T> startInSession(Host peerSocket, ISerializer<T> serializer,
            String myIdAlias, KeyPair dhKeyPair, SecretKey secretKey, byte[] myIv, byte[] peerId, byte[] peerIv) {
        var session = new AuthSession<>(peerSocket, serializer, myIdAlias, dhKeyPair, myIv);

        session.peerId = peerId;
        session.peerLastMac = peerIv;
        session.sessionKey = secretKey;

        logger.debug("Starting in session with my iv: {}\nand peer iv: {}", Bytes.of(myIv), Bytes.of(peerIv));

        return session;
    }

    // TODO give use to this?
    public void setMacAlgorithm(String macAlgorithm) {
        this.macAlgorithm = macAlgorithm;
    }

    public void completeOutSessionSetup(byte[] peerId, SecretKey sessionKey, byte[] peerIv)
            throws IOException, InvalidKeyException, NoSuchAlgorithmException, AuthenticationException {
        if (state != State.CONNECTING)
            throw new IllegalStateException("Tried to complete a connection that was already completed.");

        if (this.peerId != null && !Arrays.equals(this.peerId, peerId))
            throw new AuthenticationException("Expected peer id %s, but got %s"
                    .formatted(this.peerId, peerId));

        this.peerId = peerId;
        this.sessionKey = sessionKey;
        this.peerLastMac = peerIv;

        logger.debug("Completing out session with my iv: {}\nand peer iv: {}", Bytes.of(myLastMac), Bytes.of(peerIv));
    }

    public void completeInSessionSetup(Connection<AuthenticatedMessage> connection) {
        if (state != State.CONNECTING)
            throw new IllegalStateException("Tried to complete a connection that was already completed.");

        this.connection = connection;
    }

    public void setState(State newState) {
        this.state = newState;
    }

    public void disconect() {
        state = State.DISCONNECTING;
        connection.disconnect();
    }

    private Promise<Void> lastMsgPromise;

    public synchronized void macAndSend(T msg, Promise<Void> promise)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException {
        ByteBuf encodedMsg = Unpooled.buffer();
        serializer.serialize(msg, encodedMsg);

        // Needed to make sure the MACs are synchronized
        while (lastMsgPromise != null && !lastMsgPromise.isDone()) {
            try {
                lastMsgPromise.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Mac mac = Mac.getInstance(macAlgorithm, AuthChannel.PROVIDER);
        mac.init(sessionKey);
        mac.update(encodedMsg.array());
        mac.update(myLastMac);
        byte[] newMac = mac.doFinal();

        myLastMac = newMac;

        AuthenticatedMessage authMsg = new AuthenticatedMessage(encodedMsg.array(), newMac);
        connection.sendMessage(authMsg, promise);
    }

    public T receiveMessage(AuthenticatedMessage authMsg)
            throws NoSuchAlgorithmException, InvalidKeyException, MessageAuthenticationException, IOException {
        Mac mac = Mac.getInstance(macAlgorithm, AuthChannel.PROVIDER);
        mac.init(sessionKey);
        mac.update(authMsg.getData());
        mac.update(peerLastMac);
        byte[] expectedMac = mac.doFinal();

        if (Arrays.equals(authMsg.getMac(), expectedMac)) {
            peerLastMac = expectedMac;
            return serializer.deserialize(Unpooled.wrappedBuffer(authMsg.getData()));
        } else {
            HexFormat hex = HexFormat.of();
            throw new MessageAuthenticationException("Expected MAC %s but got %s."
                    .formatted(hex.formatHex(expectedMac), hex.formatHex(authMsg.getMac())));
        }
    }

    public Connection<AuthenticatedMessage> getConnection() {
        return connection;
    }

    public long getConnectionId() {
        return connection.getConnectionId();
    }

    public boolean enqueue(T msg) {
        return msgQueue.add(msg);
    }

    public Queue<T> getMsgQueue() {
        return msgQueue;
    }

    public State getState() {
        return state;
    }

    public KeyPair getDhKeyPair() {
        return dhKeyPair;
    }

    public SecretKey getSessionKey() {
        return sessionKey;
    }

    public String getMyIdAlias() {
        return myIdAlias;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public byte[] getMyLastMac() {
        return myLastMac;
    }

    public byte[] getPeerLastMac() {
        return peerLastMac;
    }

    public Host getPeerSocket() {
        return peerSocket;
    }

}
