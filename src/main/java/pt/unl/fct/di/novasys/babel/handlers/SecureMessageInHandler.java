package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * An extension of {@link MessageInHandler} that also receives the peer id, if
 * the handled event was sent by a secure channel.
 */
@FunctionalInterface
public interface SecureMessageInHandler<T extends ProtoMessage> extends MessageInHandler<T> {

    @Override
    void receive(T msg, Host from, byte[] peerId, short sourceProto, int channelId);

    @Override
    default void receive(T msg, Host from, short sourceProto, int channelId) {
        receive(msg, from, null, sourceProto, channelId);
    }

}
