package pt.unl.fct.di.novasys.babel.generic;

/**
 * Abstract Request class to be extended by protocol-specific requests.
 */
public abstract class ProtoRequest extends ProtoIPC{

    private final short id;

    /**
     * Constructs a new request with the given type identifier.
     *
     * @param id the numeric identifier that distinguishes this request type within its protocol
     */
    public ProtoRequest(short id){
        super(Type.REQUEST);
        this.id = id;
    }

    /**
     * Returns the numeric type identifier of this request.
     *
     * @return the request type ID assigned at construction
     */
    public short getId() {
        return id;
    }
}
