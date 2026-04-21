package pt.unl.fct.di.novasys.babel.generic;

/**
 * Base class for inter-protocol communication objects (requests and replies).
 *
 * <p>Subclassed by {@link ProtoRequest} and {@link ProtoReply}. The {@link Type}
 * discriminates which direction the IPC object travels.
 */
public abstract class ProtoIPC {

    /** Discriminates the direction of an IPC object: either a reply or a request. */
    public enum Type { REPLY, REQUEST }

    private Type type;

    /**
     * Constructs a new IPC object with the given direction type.
     *
     * @param t the IPC direction ({@link Type#REQUEST} or {@link Type#REPLY})
     */
    public ProtoIPC(Type t) {
        this.type = t;
    }

    /**
     * Returns the IPC direction type of this object.
     *
     * @return {@link Type#REQUEST} if this is a request, {@link Type#REPLY} if this is a reply
     */
    public Type getType() {
        return type;
    }
}
