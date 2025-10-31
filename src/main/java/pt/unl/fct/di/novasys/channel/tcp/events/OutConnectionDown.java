package pt.unl.fct.di.novasys.channel.tcp.events;

import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Triggered when an established outbound connection is disconnected.
 */
public class OutConnectionDown extends TCPEvent {

    public static final short EVENT_ID = 3;

    private final Host node;
    private final Throwable cause;

    @Override
    public String toString() {
        return "OutConnectionDown{" +
                "node=" + node +
                ", cause=" + cause +
                '}';
    }

    protected OutConnectionDown(short eventId, Host node, Throwable cause) {
        super(eventId);
        this.cause = cause;
        this.node = node;
    }

    public OutConnectionDown(Host node, Throwable cause) {
        this(EVENT_ID, node, cause);
    }

    public Throwable getCause() {
        return cause;
    }

    public Host getNode() {
        return node;
    }

}
