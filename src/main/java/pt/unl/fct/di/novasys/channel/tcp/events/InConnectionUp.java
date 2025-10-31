package pt.unl.fct.di.novasys.channel.tcp.events;

import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Triggered when an incoming connection is established.
 */
public class InConnectionUp extends TCPEvent {

    public static final short EVENT_ID = 2;

    private final Host node;

    @Override
    public String toString() {
        return "InConnectionUp{" +
                "node=" + node +
                '}';
    }

    protected InConnectionUp(short eventId, Host node) {
        super(eventId);
        this.node = node;
    }

    public InConnectionUp(Host node) {
        this(EVENT_ID, node);
    }


    public Host getNode() {
        return node;
    }

}
