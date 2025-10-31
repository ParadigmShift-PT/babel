package pt.unl.fct.di.novasys.channel.secure.events;

import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionUp;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.data.Bytes;

public class SecureInConnectionUp extends InConnectionUp {

    public static final short EVENT_ID = InConnectionUp.EVENT_ID + 10;

    private final byte[] nodeId;

    public SecureInConnectionUp(Host node, Bytes nodeId) {
        this(node, nodeId.array());
    }

    public SecureInConnectionUp(Host node, byte[] nodeId) {
        super(EVENT_ID, node);
        this.nodeId = nodeId;
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return "SecureInConnectionUp { " +
                "node=" + getNode() +
                ", nodeId=" + Bytes.of(nodeId) +
                " }";
    }

}
