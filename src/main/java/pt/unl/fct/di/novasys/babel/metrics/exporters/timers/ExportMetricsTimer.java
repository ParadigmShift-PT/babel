package pt.unl.fct.di.novasys.babel.metrics.exporters.timers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

/**
 * Periodic Babel timer that triggers a metric export cycle in {@link pt.unl.fct.di.novasys.babel.metrics.exporters.MonitorExporter}.
 */
public class ExportMetricsTimer extends ProtoTimer {

    public static final short PROTO_ID = 11110;

    /**
     * Creates a new {@code ExportMetricsTimer} with the fixed timer ID {@link #PROTO_ID}.
     */
    public ExportMetricsTimer() {
        super(PROTO_ID);
    }

    /**
     * Returns this instance; the timer carries no mutable state and does not need to be cloned.
     *
     * @return this timer instance
     */
    @Override
    public ProtoTimer clone() {
        return this;
    }

}
