package pt.unl.fct.di.novasys.channel.secure.events;

import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionDown;

import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.data.Bytes;

public class SecureInConnectionDown extends InConnectionDown {

    public static final short EVENT_ID = InConnectionDown.EVENT_ID + 10;

    private final byte[] nodeId;

    public SecureInConnectionDown(Host node, Bytes nodeId, Throwable cause) {
        this(node, nodeId.array(), cause);
    }

    public SecureInConnectionDown(Host node, byte[] nodeId, Throwable cause) {
        super(EVENT_ID, node, cause);
        this.nodeId = nodeId;
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return "SecureInConnectionDown { " +
                "node=" + getNode() +
                ", nodeId=" + Bytes.of(nodeId) +
                ", cause=" + getCause() +
                " }";
    }

}
