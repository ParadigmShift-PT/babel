package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Represents an operation that accepts a single input argument and returns no
 * result. Unlike most other functional interfaces, {@code Consumer} is expected
 * to operate via side-effects.
 *
 */
@FunctionalInterface
public interface MessageSentHandler<T extends ProtoMessage> {

    /**
     * Performs this operation on the ProtocolMessage.
     *
     * @param msg the received message
     */
    void onMessageSent(T msg, Host to, short destProto, int channelId);

    /**
     * Performs this operation on the ProtocolMessage.
     *
     * @param msg the received message
     */
    default void onMessageSent(T msg, Host to, byte[] toId, short destProto, int channelId) {
        onMessageSent(msg, to, destProto, channelId);
    }

}
