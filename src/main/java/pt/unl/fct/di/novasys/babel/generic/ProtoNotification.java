package pt.unl.fct.di.novasys.babel.generic;

/**
 * Abstract Notification class to be extended by protocol-specific notifications.
 */
public abstract class ProtoNotification {

    private final short id;

    /**
     * Constructs a new notification with the given type identifier.
     *
     * @param id the numeric identifier that distinguishes this notification type within its protocol
     */
    public ProtoNotification(short id){
        this.id = id;
    }

    /**
     * Returns the numeric type identifier of this notification.
     *
     * @return the notification type ID assigned at construction
     */
    public short getId() {
        return id;
    }

}
