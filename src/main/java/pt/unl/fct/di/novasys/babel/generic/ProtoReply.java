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

    public ProtoReply(short id) {
        super(Type.REPLY);
        this.id = id;
    }

    public short getId() {
        return id;
    }
}
