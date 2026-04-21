package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.timers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

/**
 * One-shot timer that drives the periodic parameter-search cycle in
 * {@link pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.CopySelfConfigurationProtocol}.
 * When it fires, the protocol contacts known peers and asks for unconfigured parameter values;
 * it reschedules itself until all parameters have been resolved.
 */
public class SearchTimer extends ProtoTimer {

    public static final short TIMER_ID = 341;

    /**
     * Creates a new SearchTimer with the fixed {@link #TIMER_ID}.
     */
    public SearchTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
