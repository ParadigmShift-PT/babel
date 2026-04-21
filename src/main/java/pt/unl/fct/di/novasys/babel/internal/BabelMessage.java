package pt.unl.fct.di.novasys.babel.internal;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

/**
 * An envelope that wraps a {@link ProtoMessage} with the numeric protocol IDs of
 * its sender and intended recipient, allowing Babel to route it across channels.
 */
public class BabelMessage {

    private final ProtoMessage message;
    private final short sourceProto;
    private final short destProto;

    @Override
    public String toString() {
        return "BabelMessage{" +
                "message=" + message +
                ", sourceProto=" + sourceProto +
                ", destProto=" + destProto +
                '}';
    }

    /**
     * Constructs a BabelMessage wrapping the given protocol message with routing metadata.
     *
     * @param message     the payload message to be delivered
     * @param sourceProto numeric ID of the sending protocol
     * @param destProto   numeric ID of the destination protocol
     */
    public BabelMessage(ProtoMessage message, short sourceProto, short destProto) {
        this.message = message;
        this.sourceProto = sourceProto;
        this.destProto = destProto;
    }

    /**
     * Returns the wrapped protocol message payload.
     *
     * @return the protocol message
     */
    public ProtoMessage getMessage() {
        return message;
    }

    /**
     * Returns the numeric ID of the protocol that sent this message.
     *
     * @return source protocol ID
     */
    public short getSourceProto() {
        return sourceProto;
    }

    /**
     * Returns the numeric ID of the protocol that should receive this message.
     *
     * @return destination protocol ID
     */
    public short getDestProto() {
        return destProto;
    }
}
