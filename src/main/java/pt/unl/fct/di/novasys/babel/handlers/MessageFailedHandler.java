package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Functional interface for handling message-send failures.
 *
 * <p>Registered via {@code GenericProtocol.registerMessageHandler}. Invoked when the
 * channel cannot deliver a message — for example because the connection dropped before
 * the send completed. The five-argument overload is invoked by secure channels that
 * supply a peer identity; the default delegates to the four-argument form. Use
 * {@link SecureMessageFailedHandler} when the identity is needed.
 *
 * @param <T> the concrete message type
 */
@FunctionalInterface
public interface MessageFailedHandler<T extends ProtoMessage> {

    /**
     * Called when delivery of a message of type {@code T} has failed.
     *
     * @param msg       the message that could not be delivered
     * @param to        the intended recipient's address
     * @param destProto the protocol ID of the intended recipient
     * @param cause     the reason for the failure
     * @param channelId the channel on which the send was attempted
     */
    void onMessageFailed(T msg, Host to, short destProto, Throwable cause, int channelId);

    /**
     * Called when delivery fails on a secure channel that supplies a peer identity.
     * Defaults to {@link #onMessageFailed(ProtoMessage, Host, short, Throwable, int)},
     * ignoring {@code toId}.
     *
     * @param msg       the message that could not be delivered
     * @param to        the intended recipient's address
     * @param toId      the cryptographic identity of the intended recipient, or {@code null}
     * @param destProto the protocol ID of the intended recipient
     * @param cause     the reason for the failure
     * @param channelId the channel on which the send was attempted
     */
    default void onMessageFailed(T msg, Host to, byte[] toId, short destProto, Throwable cause, int channelId) {
        onMessageFailed(msg, to, destProto, cause, channelId);
    }

}
