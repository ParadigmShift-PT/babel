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

    /**
     * Creates a {@code ProtocolSample} with an explicit timestamp.
     *
     * @param timestamp     the collection time in epoch milliseconds
     * @param protocolId    the protocol identifier
     * @param protocolName  the protocol name
     * @param metricSamples the metric samples collected for this protocol
     */
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

    /**
     * Creates a {@code ProtocolSample} timestamped at the current system time.
     *
     * @param protocolId    the protocol identifier
     * @param protocolName  the protocol name
     * @param metricSamples the metric samples collected for this protocol
     */
    public ProtocolSample(short protocolId, String protocolName, List<MetricSample> metricSamples) {
        this(System.currentTimeMillis(), protocolId, protocolName, metricSamples);
    }

    /**
     * Appends a {@link MetricSample} to this protocol sample and indexes it by metric name.
     *
     * @param sample the metric sample to add
     */
    public void addMetricSample(MetricSample sample) {
        metricSamples.add(sample);
        metricSampleMap.put(sample.getMetricName(), sample);
    }

    /**
     * Returns the epoch-millisecond timestamp at which these metrics were collected.
     *
     * @return the collection timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the identifier of the protocol that produced this sample.
     *
     * @return the protocol ID
     */
    public short getProtocolId() {
        return protocolId;
    }

    /**
     * Returns the name of the protocol that produced this sample.
     *
     * @return the protocol name
     */
    public String getProtocolName() {
        return protocolName;
    }

    /**
     * Returns the ordered list of all metric samples in this protocol sample.
     *
     * @return the metric samples list
     */
    public List<MetricSample> getMetricSamples() {
        return metricSamples;
    }

    /**
     * Returns the map of metric name to {@link MetricSample} for fast lookup by name.
     *
     * @return a map from metric name to its sample
     */
    public Map<String, MetricSample> getMetricSampleMap() {
        return metricSampleMap;
    }

    /**
     * Returns the {@link MetricSample} for the given metric name, or {@code null} if not present.
     *
     * @param metricName the metric name to look up
     * @return the corresponding {@link MetricSample}, or {@code null}
     */
    public MetricSample getMetricSample(String metricName) {
        return metricSampleMap.get(metricName);
    }

}
