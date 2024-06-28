package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * An extension of {@link MessageFailedHandler} that also receives the peer id,
 * if the handled event was sent by a secure channel.
 */
@FunctionalInterface
public interface SecureMessageFailedHandler<T extends ProtoMessage> extends MessageFailedHandler<T> {

    @Override
    void onMessageFailed(T msg, Host to, byte[] toId, short destProto, Throwable cause, int channelId);

    @Override
    default void onMessageFailed(T msg, Host to, short destProto, Throwable cause, int channelId) {
        onMessageFailed(msg, to, null, destProto, cause, channelId);
    }

}
