package pt.unl.fct.di.novasys.babel.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A sample of metrics belonging to a specific protocol
 */
public class ProtocolSample implements Serializable {
    private final long timestamp;

    private final short protocolId;

    private final String protocolName;

    private final List<MetricSample> metricSamples;

    @JsonIgnore
    private final Map<String, MetricSample> metricSampleMap;

    public ProtocolSample(long timestamp, short protocolId, String protocolName, List<MetricSample> metricSamples) {
        this.timestamp = timestamp;
        this.protocolId = protocolId;
        this.protocolName = protocolName;
        this.metricSamples = metricSamples;
        this.metricSampleMap = new HashMap<>();
        for (MetricSample sample : metricSamples) {
            metricSampleMap.put(sample.getMetricName(), sample);
        }
    }

    public ProtocolSample(short protocolId, String protocolName, List<MetricSample> metricSamples) {
        this(System.currentTimeMillis(), protocolId, protocolName, metricSamples);
    }

    public void addMetricSample(MetricSample sample) {
        metricSamples.add(sample);
        metricSampleMap.put(sample.getMetricName(), sample);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public short getProtocolId() {
        return protocolId;
    }

    public String getProtocolName() {
        return protocolName;
    }

    public List<MetricSample> getMetricSamples() {
        return metricSamples;
    }

    public Map<String, MetricSample> getMetricSampleMap() {
        return metricSampleMap;
    }
    public MetricSample getMetricSample(String metricName) {
        return metricSampleMap.get(metricName);
    }

}
