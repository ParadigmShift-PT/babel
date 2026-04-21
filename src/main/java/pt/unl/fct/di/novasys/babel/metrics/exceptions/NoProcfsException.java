package pt.unl.fct.di.novasys.babel.metrics.exceptions;

/**
 * Thrown when the Linux {@code /proc} filesystem cannot be accessed, making OS-level
 * metric collection impossible.
 */
public class NoProcfsException extends RuntimeException{
    /**
     * Creates a new exception with the supplied detail message.
     *
     * @param message description of the procfs access failure
     */
    public NoProcfsException(String message) {
        super(message);
    }
}
