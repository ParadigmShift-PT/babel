package pt.unl.fct.di.novasys.babel.exceptions;

/**
 * Thrown when a configuration or initialization parameter supplied to a Babel component
 * is missing, out of range, or otherwise invalid.
 */
public class InvalidParameterException extends Exception {

    /** Creates an {@code InvalidParameterException} with no detail message. */
    public InvalidParameterException() {
    }

    /**
     * Creates an {@code InvalidParameterException} with the given detail message.
     *
     * @param message a description of the invalid parameter
     */
    public InvalidParameterException(String message) {
        super(message);
    }

    /**
     * Creates an {@code InvalidParameterException} with a detail message and a cause.
     *
     * @param message a description of the invalid parameter
     * @param cause   the underlying exception that triggered this error
     */
    public InvalidParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an {@code InvalidParameterException} wrapping an existing cause.
     *
     * @param cause the underlying exception that triggered this error
     */
    public InvalidParameterException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an {@code InvalidParameterException} with full control over suppression and
     * stack-trace writability.
     *
     * @param message            a description of the invalid parameter
     * @param cause              the underlying exception that triggered this error
     * @param enableSuppression  whether suppressed exceptions may be added
     * @param writableStackTrace whether the stack trace should be writable
     */
    public InvalidParameterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
