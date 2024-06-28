package pt.unl.fct.di.novasys.babel.metrics.exceptions;

public class DuplicatedProtocolMetric extends RuntimeException{
    public DuplicatedProtocolMetric(short protoId, String metricName) {
        super("Metric " + metricName + " already exists for protocol with ID " + protoId);
    }
}
