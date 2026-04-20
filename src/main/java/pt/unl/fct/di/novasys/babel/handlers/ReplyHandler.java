package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

/**
 * Functional interface for handling inter-protocol replies.
 *
 * <p>Registered via {@code GenericProtocol.registerReplyHandler}. Invoked on this
 * protocol's thread when a previously issued request receives a reply of type {@code T}.
 *
 * @param <T> the concrete reply type
 */
@FunctionalInterface
public interface ReplyHandler<T extends ProtoReply> {

    /**
     * Called when a reply of type {@code T} is received from another protocol.
     *
     * @param reply the received reply
     * @param from  the protocol ID of the replier
     */
    void uponReply(T reply, short from);

}
