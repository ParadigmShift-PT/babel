package pt.unl.fct.di.novasys.babel.generic;

/**
 * Abstract Timer class to be extended by protocol-specific timers.
 */
public abstract class ProtoTimer implements Cloneable {

    private final short id;

    /**
     * Constructs a new timer with the given type identifier.
     *
     * @param id the numeric identifier that distinguishes this timer type within its protocol
     */
    public ProtoTimer(short id){
        this.id = id;
    }

    /**
     * Returns the numeric type identifier of this timer.
     *
     * @return the timer type ID assigned at construction
     */
    public short getId() {
        return id;
    }

    /**
     * Returns a deep copy of this timer, required by the Babel scheduler when rescheduling periodic timers.
     *
     * @return a new {@code ProtoTimer} instance with the same state as this one
     */
    public abstract ProtoTimer clone();
}
