package pt.unl.fct.di.novasys.channel.tcp.events;

import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Triggered when an incoming connection is disconnected.
 */
public class InConnectionDown extends TCPEvent {

    public static final short EVENT_ID = 1;

    private final Host node;
    private final Throwable cause;

    @Override
    public String toString() {
        return "InConnectionDown{" +
                "node=" + node +
                ", cause=" + cause +
                '}';
    }

    protected InConnectionDown(short eventId, Host node, Throwable cause) {
        super(eventId);
        this.cause = cause;
        this.node = node;
    }

    public InConnectionDown(Host node, Throwable cause) {
        this(EVENT_ID, node, cause);
    }

    public Throwable getCause() {
        return cause;
    }

    public Host getNode() {
        return node;
    }

}
