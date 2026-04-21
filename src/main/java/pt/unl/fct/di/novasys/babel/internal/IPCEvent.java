package pt.unl.fct.di.novasys.babel.internal;

import pt.unl.fct.di.novasys.babel.generic.ProtoIPC;

/**
 * An internal event that carries an inter-protocol communication (IPC) object between
 * two protocols identified by their numeric IDs.
 */
public class IPCEvent extends InternalEvent {

    private final ProtoIPC ipc;
    private final short senderID;
    private final short destinationID;

    /**
     * Constructs an IPCEvent carrying {@code ipc} from protocol {@code sender} to protocol {@code destination}.
     *
     * @param ipc         the IPC payload (request or reply)
     * @param sender      numeric ID of the originating protocol
     * @param destination numeric ID of the receiving protocol
     */
    public IPCEvent(ProtoIPC ipc, short sender, short destination) {
        super(EventType.IPC_EVENT);
        this.ipc = ipc;
        this.senderID = sender;
        this.destinationID = destination;
    }

    /**
     * Returns the IPC payload carried by this event.
     *
     * @return the IPC object (request or reply)
     */
    public ProtoIPC getIpc() {
        return ipc;
    }

    /**
     * Returns the numeric ID of the destination protocol.
     *
     * @return destination protocol ID
     */
    public short getDestinationID() {
        return destinationID;
    }

    /**
     * Returns the numeric ID of the protocol that originated the IPC.
     *
     * @return sender protocol ID
     */
    public short getSenderID() {
        return senderID;
    }

    @Override
    public String toString() {
        return "IPCEvent { senderID=" + senderID + ", destinationID=" + destinationID + ", ipc=" + ipc  + " }";
    }

}
