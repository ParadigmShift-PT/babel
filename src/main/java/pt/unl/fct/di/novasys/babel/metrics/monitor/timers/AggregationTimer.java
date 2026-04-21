package pt.unl.fct.di.novasys.babel.metrics.monitor.timers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

/**
 * Periodic Babel timer that triggers the aggregation cycle in {@link pt.unl.fct.di.novasys.babel.metrics.monitor.SimpleMonitor}.
 */
public class AggregationTimer extends ProtoTimer {

    public static final short PROTO_ID = 11101;

    /**
     * Constructs a new {@code AggregationTimer} with its fixed protocol ID.
     */
    public AggregationTimer() {
        super(PROTO_ID);
    }

    /**
     * Returns this instance unchanged, as {@code AggregationTimer} carries no mutable state.
     *
     * @return this timer
     */
    @Override
    public ProtoTimer clone() {
        return this;
    }

}
