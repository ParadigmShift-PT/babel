package pt.unl.fct.di.novasys.babel.metrics;

public class MetricSchedule {
    private int protocolId;
    private Metric metric;

    public MetricSchedule(int protocolId, Metric metric) {
        this.protocolId = protocolId;
        this.metric = metric;
    }

    public Metric getMetric() {
        return metric;
    }

    public int getProtocolId() {
        return protocolId;
    }
}
