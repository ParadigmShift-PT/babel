package pt.unl.fct.di.novasys.babel.exceptions;

/**
 * Thrown when a protocol attempts to register a handler that conflicts with an existing
 * registration — for example, registering two handlers for the same message or timer type.
 */
public class HandlerRegistrationException extends Exception {

    /** Creates a {@code HandlerRegistrationException} with no detail message. */
    public HandlerRegistrationException() {
    }

    /**
     * Creates a {@code HandlerRegistrationException} with the given detail message.
     *
     * @param message a description of the registration conflict
     */
    public HandlerRegistrationException(String message) {
        super(message);
    }

    /**
     * Creates a {@code HandlerRegistrationException} with a detail message and a cause.
     *
     * @param message a description of the registration conflict
     * @param cause   the underlying exception that triggered this error
     */
    public HandlerRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a {@code HandlerRegistrationException} wrapping an existing cause.
     *
     * @param cause the underlying exception that triggered this error
     */
    public HandlerRegistrationException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a {@code HandlerRegistrationException} with full control over suppression and
     * stack-trace writability.
     *
     * @param message            a description of the registration conflict
     * @param cause              the underlying exception that triggered this error
     * @param enableSuppression  whether suppressed exceptions may be added
     * @param writableStackTrace whether the stack trace should be writable
     */
    public HandlerRegistrationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
