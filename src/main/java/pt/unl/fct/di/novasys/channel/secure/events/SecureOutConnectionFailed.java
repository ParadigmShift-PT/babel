package pt.unl.fct.di.novasys.channel.secure.events;

import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionFailed;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.data.Bytes;

import java.util.Queue;

/**
 * Triggered when an outbound connection fails to establish.
 */
public class SecureOutConnectionFailed<T>  extends OutConnectionFailed<T> {

    public static final short EVENT_ID = OutConnectionFailed.EVENT_ID + 10;

    private final byte[] nodeId;

    public SecureOutConnectionFailed(Host node, byte[] nodeId, Queue<T> pendingMessages, Throwable cause) {
        super(EVENT_ID, node, pendingMessages, cause);
        this.nodeId = nodeId;
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return "SecureOutConnectionFailed {" +
                "node=" + getNode() +
                ", nodeId=" + Bytes.of(nodeId) +
                ", pendingMessages=" + getPendingMessages() +
                ", cause=" + getCause() +
                " }";
    }

}
