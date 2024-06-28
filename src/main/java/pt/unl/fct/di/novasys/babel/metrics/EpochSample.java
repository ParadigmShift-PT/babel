package pt.unl.fct.di.novasys.babel.metrics;

import java.util.List;

/**
 * A sample of metrics collected at a given epoch for a single protocol registry
 */
public class EpochSample {
    private final long epoch;

    private final short protocolId;
    private final List<MetricSample> metricSamples;

    public  EpochSample(long epoch, short protocolId, List<MetricSample> metricSamples) {
        this.epoch = epoch;
        this.protocolId = protocolId;
        this.metricSamples = metricSamples;
    }

    public long getEpoch() {
        return epoch;
    }

    public short getProtocolId() {
        return protocolId;

    }
    public List<MetricSample> getMetricSamples() {
        return metricSamples;
    }
}
