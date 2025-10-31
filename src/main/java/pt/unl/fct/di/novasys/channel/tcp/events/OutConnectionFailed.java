package pt.unl.fct.di.novasys.channel.tcp.events;

import pt.unl.fct.di.novasys.network.data.Host;

import java.util.Queue;

/**
 * Triggered when an outbound connection fails to establish.
 */
public class OutConnectionFailed<T>  extends TCPEvent {

    public static final short EVENT_ID = 4;

    private final Host node;
    private final Queue<T> pendingMessages;
    private final Throwable cause;

    @Override
    public String toString() {
        return "OutConnectionFailed{" +
                "node=" + node +
                ", pendingMessages=" + pendingMessages +
                ", cause=" + cause +
                '}';
    }

    protected OutConnectionFailed(short eventId, Host node, Queue<T> pendingMessages, Throwable cause) {
        super(eventId);
        this.cause = cause;
        this.node = node;
        this.pendingMessages = pendingMessages;
    }

    public OutConnectionFailed(Host node, Queue<T> pendingMessages, Throwable cause) {
        this(EVENT_ID, node, pendingMessages, cause);
    }

    public Throwable getCause() {
        return cause;
    }

    public Host getNode() {
        return node;
    }

    public Queue<T> getPendingMessages() {
        return pendingMessages;
    }
}
