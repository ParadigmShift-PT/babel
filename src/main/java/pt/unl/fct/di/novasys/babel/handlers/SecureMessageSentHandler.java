package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * An extension of {@link MessageSentHandler} that also receives the peer id, if
 * the handled event was sent by a secure channel.
 */
@FunctionalInterface
public interface SecureMessageSentHandler<T extends ProtoMessage> extends MessageSentHandler<T> {

    @Override
    void onMessageSent(T msg, Host from, byte[] peerId, short sourceProto, int channelId);

    @Override
    default void onMessageSent(T msg, Host from, short sourceProto, int channelId) {
        onMessageSent(msg, from, null, sourceProto, channelId);
    }

}
