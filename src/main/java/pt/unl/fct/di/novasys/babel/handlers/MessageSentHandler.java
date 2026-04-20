package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Functional interface for handling successful message-send confirmations.
 *
 * <p>Registered via {@code GenericProtocol.registerMessageHandler} when
 * {@code triggerSent} is enabled on the channel. The five-argument overload is
 * invoked by secure channels that supply a peer identity; the default implementation
 * delegates to the four-argument form. Use {@link SecureMessageSentHandler} when the
 * peer identity is needed.
 *
 * @param <T> the concrete message type
 */
@FunctionalInterface
public interface MessageSentHandler<T extends ProtoMessage> {

    /**
     * Called when a message of type {@code T} has been successfully sent.
     *
     * @param msg       the message that was sent
     * @param to        the recipient's address
     * @param destProto the protocol ID of the recipient
     * @param channelId the channel on which the message was sent
     */
    void onMessageSent(T msg, Host to, short destProto, int channelId);

    /**
     * Called when a message is confirmed sent on a secure channel that supplies a peer identity.
     * Defaults to {@link #onMessageSent(ProtoMessage, Host, short, int)}, ignoring {@code toId}.
     *
     * @param msg       the message that was sent
     * @param to        the recipient's address
     * @param toId      the cryptographic identity of the recipient, or {@code null}
     * @param destProto the protocol ID of the recipient
     * @param channelId the channel on which the message was sent
     */
    default void onMessageSent(T msg, Host to, byte[] toId, short destProto, int channelId) {
        onMessageSent(msg, to, destProto, channelId);
    }

}
