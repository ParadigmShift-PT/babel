package pt.unl.fct.di.novasys.babel.metrics.monitor.timers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class AggregationTimer extends ProtoTimer {

    public static final short PROTO_ID = 11101;

    public AggregationTimer() {
        super(PROTO_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
    
}
