package pt.unl.fct.di.novasys.babel.metrics.monitor;

public class MetricIdentifier {
    private final String metricName;
    private final short protocolId;

    /**
     * Creates a new MetricIdentifier, uniquely identifying a metric in a protocol
     * @param metricName The name of the metric
     * @param protocolId The id of the protocol
     */
    public MetricIdentifier(String metricName, short protocolId) {
        this.metricName = metricName;
        this.protocolId = protocolId;
    }

    public String getMetricName() {
        return metricName;
    }

    public short getProtocolId() {
        return protocolId;
    }

    @Override
    public int hashCode() {
        return metricName.hashCode() + 31 * protocolId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MetricIdentifier)) return false;
        MetricIdentifier other = (MetricIdentifier) obj;
        return this.metricName.equals(other.metricName) && this.protocolId == other.protocolId;
    }

    @Override
    public String toString() {
        return "MetricIdentifier{" +
                "metricName='" + metricName + '\'' +
                ", protocolId=" + protocolId +
                '}';
    }
}
