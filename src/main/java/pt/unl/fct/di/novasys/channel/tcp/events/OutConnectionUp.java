package pt.unl.fct.di.novasys.channel.tcp.events;

import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Triggered when a new outbound connection is established.
 */
public class OutConnectionUp extends TCPEvent {

    public static final short EVENT_ID = 5;

    private final Host node;

    @Override
    public String toString() {
        return "OutConnectionUp{" +
                "node=" + node +
                '}';
    }

    protected OutConnectionUp(short eventId, Host node) {
        super(eventId);
        this.node = node;
    }

    public OutConnectionUp(Host node) {
        this(EVENT_ID, node);
    }


    public Host getNode() {
        return node;
    }

}
