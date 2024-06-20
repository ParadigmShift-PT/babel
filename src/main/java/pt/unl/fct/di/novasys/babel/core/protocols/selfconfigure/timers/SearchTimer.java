package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.timers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class SearchTimer extends ProtoTimer {

    public static final short TIMER_ID = 341;

    public SearchTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
