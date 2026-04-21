package pt.unl.fct.di.novasys.babel.metrics.exceptions;

/**
 * Thrown when a metric with the same name is registered more than once for the same protocol.
 */
public class DuplicatedProtocolMetric extends RuntimeException{
    /**
     * Creates a new exception indicating that {@code metricName} already exists for the protocol identified by {@code protoId}.
     *
     * @param protoId    the numeric ID of the protocol that already owns the metric
     * @param metricName the name of the duplicate metric
     */
    public DuplicatedProtocolMetric(short protoId, String metricName) {
        super("Metric " + metricName + " already exists for protocol with ID " + protoId);
    }
}
