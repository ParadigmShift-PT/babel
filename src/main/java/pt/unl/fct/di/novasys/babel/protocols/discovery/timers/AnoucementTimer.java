package pt.unl.fct.di.novasys.babel.protocols.discovery.timers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class AnoucementTimer extends ProtoTimer {

    public static final short TIMER_ID = 10603;

    public AnoucementTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
