package pt.unl.fct.di.novasys.babel.generic;

/**
 * Abstract base class for protocol-specific replies.
 *
 * <p>Extend this class to define a typed reply to a {@link ProtoRequest}.
 * The {@code id} field identifies the reply type and must match the value
 * registered with {@code GenericProtocol.registerReplyHandler}.
 */
public abstract class ProtoReply extends ProtoIPC {

    private final short id;

    /**
     * Constructs a new reply with the given type identifier.
     *
     * @param id the numeric identifier that distinguishes this reply type within its protocol
     */
    public ProtoReply(short id) {
        super(Type.REPLY);
        this.id = id;
    }

    /**
     * Returns the numeric type identifier of this reply.
     *
     * @return the reply type ID assigned at construction
     */
    public short getId() {
        return id;
    }
}
