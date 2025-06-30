package pt.unl.fct.di.novasys.babel.metrics.exporters.timers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class ExportMetricsTimer extends ProtoTimer {

    public static final short PROTO_ID = 11110;

    public ExportMetricsTimer() {
        super(PROTO_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
    
}
