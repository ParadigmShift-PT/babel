package pt.unl.fct.di.novasys.babel.core.protocols.discovery.timers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

/**
 * Periodic timer that triggers the announcement broadcast in local discovery protocols.
 * Fired at a fixed interval to send service discovery probes to all registered socket addresses.
 */
public class AnoucementTimer extends ProtoTimer {

    public static final short TIMER_ID = 32501;

    /**
     * Creates a new AnoucementTimer with the fixed {@link #TIMER_ID}.
     */
    public AnoucementTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
