package pt.unl.fct.di.novasys.babel.metrics.exceptions;

public class NoSuchProtocolRegistry extends RuntimeException{
    public NoSuchProtocolRegistry(short protoId) {
        super("Protocol " + protoId + " does not have an associated ProtocolMetrics registry.");
    }
}
