package pt.unl.fct.di.novasys.babel.generic;

/**
 * Base class for inter-protocol communication objects (requests and replies).
 *
 * <p>Subclassed by {@link ProtoRequest} and {@link ProtoReply}. The {@link Type}
 * discriminates which direction the IPC object travels.
 */
public abstract class ProtoIPC {

    public enum Type { REPLY, REQUEST }

    private Type type;

    public ProtoIPC(Type t) {
        this.type = t;
    }

    public Type getType() {
        return type;
    }
}
