package pt.unl.fct.di.novasys.babel.internal;

import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;

/**
 * An internal event that carries a {@link ProtoNotification} emitted by one protocol
 * to all subscribed protocols.
 */
public class NotificationEvent extends InternalEvent {

    private final ProtoNotification notification;
    private final short emitterID;

    /**
     * Constructs a NotificationEvent wrapping {@code notification} from the given emitter.
     *
     * @param notification the notification payload to deliver
     * @param emitterID    numeric ID of the protocol that emitted the notification
     */
    public NotificationEvent(ProtoNotification notification, short emitterID) {
        super(EventType.NOTIFICATION_EVENT);
        this.notification = notification;
        this.emitterID = emitterID;
    }

    /**
     * Returns the notification payload.
     *
     * @return the protocol notification
     */
    public ProtoNotification getNotification() {
        return notification;
    }

    /**
     * Returns the numeric ID of the protocol that emitted this notification.
     *
     * @return emitter protocol ID
     */
    public short getEmitterID() {
        return emitterID;
    }
}
