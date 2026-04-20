package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;

/**
 * Functional interface for handling protocol notifications.
 *
 * <p>Registered via {@code GenericProtocol.subscribeNotification}. Invoked on the
 * subscriber's thread when another protocol emits a notification of type {@code T}.
 *
 * @param <T> the concrete notification type
 */
@FunctionalInterface
public interface NotificationHandler<T extends ProtoNotification> {

    /**
     * Called when a notification of type {@code T} is delivered to this protocol.
     *
     * @param notification the received notification
     * @param emitter      the protocol ID of the emitter
     */
    void uponNotification(T notification, short emitter);

}
