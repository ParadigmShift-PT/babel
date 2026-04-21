package pt.unl.fct.di.novasys.babel.generic;

/**
 * Abstract Message class to be extended by protocol-specific messages.
 */
public abstract class ProtoMessage {

    private final short id;

    /**
     * Constructs a new protocol message with the given type identifier.
     *
     * @param id the numeric identifier that distinguishes this message type within its protocol
     */
    public ProtoMessage(short id){
        this.id = id;
    }

    /**
     * Returns the numeric type identifier of this message.
     *
     * @return the message type ID assigned at construction
     */
    public short getId() {
        return id;
    }

}
