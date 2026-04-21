package pt.unl.fct.di.novasys.babel.metrics.exceptions;

/**
 * Thrown when OS-level metrics are requested but no OS metric sources have been registered
 * with the {@code MetricsManager}.
 */
public class NoOSMetricsRegistered extends Exception{
    /**
     * Creates a new exception with the supplied detail message.
     *
     * @param message description of why no OS metrics are available
     */
    public NoOSMetricsRegistered(String message){
        super(message);
    }
}
