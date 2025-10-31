package pt.unl.fct.di.novasys.channel.secure.events;

import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionUp;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.data.Bytes;

public class SecureOutConnectionUp extends OutConnectionUp {

    public static final short EVENT_ID = OutConnectionUp.EVENT_ID + 10;

    private final byte[] nodeId;

    public SecureOutConnectionUp(Host node, Bytes nodeId) {
        this(node, nodeId.array());
    }

    public SecureOutConnectionUp(Host node, byte[] nodeId) {
        super(EVENT_ID, node);
        this.nodeId = nodeId;
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return "SecureOutConnectionUp { " +
                "node=" + getNode() +
                ", nodeId=" + Bytes.of(nodeId) +
                " }";
    }

}
