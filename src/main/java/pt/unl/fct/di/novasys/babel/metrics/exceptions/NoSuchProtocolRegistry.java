package pt.unl.fct.di.novasys.babel.metrics.exceptions;

/**
 * Thrown when a metrics collection is requested for a protocol that has no associated
 * {@code ProtocolMetrics} registry in the {@code MetricsManager}.
 */
public class NoSuchProtocolRegistry extends RuntimeException{
    /**
     * Creates a new exception identifying the missing registry by protocol ID.
     *
     * @param protoId the numeric ID of the protocol whose registry was not found
     */
    public NoSuchProtocolRegistry(short protoId) {
        super("Protocol " + protoId + " does not have an associated ProtocolMetrics registry.");
    }
}
