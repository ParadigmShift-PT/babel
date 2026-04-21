package pt.unl.fct.di.novasys.babel.exceptions;

/**
 * Thrown when a protocol is registered with the Babel runtime but another protocol
 * with the same ID is already running.
 */
public class ProtocolAlreadyExistsException extends Exception {

    /** Creates a {@code ProtocolAlreadyExistsException} with no detail message. */
    public ProtocolAlreadyExistsException() {
    }

    /**
     * Creates a {@code ProtocolAlreadyExistsException} with the given detail message.
     *
     * @param message a description of the conflict (e.g. the duplicate protocol ID)
     */
    public ProtocolAlreadyExistsException(String message) {
        super(message);
    }

    /**
     * Creates a {@code ProtocolAlreadyExistsException} with a detail message and a cause.
     *
     * @param message a description of the conflict
     * @param cause   the underlying exception that triggered this error
     */
    public ProtocolAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a {@code ProtocolAlreadyExistsException} wrapping an existing cause.
     *
     * @param cause the underlying exception that triggered this error
     */
    public ProtocolAlreadyExistsException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a {@code ProtocolAlreadyExistsException} with full control over suppression
     * and stack-trace writability.
     *
     * @param message            a description of the conflict
     * @param cause              the underlying exception that triggered this error
     * @param enableSuppression  whether suppressed exceptions may be added
     * @param writableStackTrace whether the stack trace should be writable
     */
    public ProtocolAlreadyExistsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
