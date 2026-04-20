package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

/**
 * Functional interface for handling inter-protocol requests.
 *
 * <p>Registered via {@code GenericProtocol.registerRequestHandler}. Invoked on this
 * protocol's thread when another protocol sends a request of type {@code T}. The
 * handler is expected to eventually send back a {@code ProtoReply} via
 * {@code GenericProtocol.sendReply}.
 *
 * @param <T> the concrete request type
 */
@FunctionalInterface
public interface RequestHandler<T extends ProtoRequest> {

    /**
     * Called when a request of type {@code T} is received from another protocol.
     *
     * @param request the received request
     * @param from    the protocol ID of the requester
     */
    void uponRequest(T request, short from);

}
