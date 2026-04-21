package pt.unl.fct.di.novasys.babel.metrics.exceptions;

/**
 * Thrown when the OS metrics subsystem is given an invalid or unsupported configuration.
 */
public class OSMetricsConfigException extends RuntimeException {
    /**
     * Creates a new exception with the supplied detail message describing the configuration error.
     *
     * @param message description of the configuration problem
     */
    public OSMetricsConfigException(String message) {
        super(message);
    }

}
