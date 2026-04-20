package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Functional interface for handling inbound protocol messages.
 *
 * <p>Registered via {@code GenericProtocol.registerMessageHandler}. The runtime invokes
 * {@link #receive(ProtoMessage, Host, short, int)} for channels that do not carry a peer
 * identity; the {@link #receive(ProtoMessage, Host, byte[], short, int) five-argument overload}
 * is invoked by secure channels that supply a cryptographic peer id. The default implementation
 * of the five-argument form delegates to the four-argument form, discarding the peer id —
 * override it (or use {@link SecureMessageInHandler}) when the identity is required.
 *
 * @param <T> the concrete message type
 */
@FunctionalInterface
public interface MessageInHandler<T extends ProtoMessage> {

    /**
     * Called when a message of type {@code T} is received from a remote peer.
     *
     * @param msg         the received message
     * @param from        the sender's address
     * @param sourceProto the protocol ID of the sender
     * @param channelId   the channel on which the message arrived
     */
    void receive(T msg, Host from, short sourceProto, int channelId);

    /**
     * Called when a message arrives from a secure channel that supplies a peer identity.
     * Defaults to {@link #receive(ProtoMessage, Host, short, int)}, ignoring {@code peerId}.
     * Use {@link SecureMessageInHandler} when the peer identity is needed.
     *
     * @param msg         the received message
     * @param from        the sender's address
     * @param peerId      the cryptographic identity of the sender, or {@code null}
     * @param sourceProto the protocol ID of the sender
     * @param channelId   the channel on which the message arrived
     */
    default void receive(T msg, Host from, byte[] peerId, short sourceProto, int channelId) {
        receive(msg, from, sourceProto, channelId);
    }

}
