package pt.unl.fct.di.novasys.babel.exceptions;

/**
 * Checked variant of {@link NoSuchProtocolException}: thrown when a requested protocol
 * cannot be found in the Babel runtime and the call site must handle this condition
 * explicitly.
 */
public class ProtocolDoesNotExist extends Exception {

    /** Creates a {@code ProtocolDoesNotExist} with no detail message. */
    public ProtocolDoesNotExist() {
    }

    /**
     * Creates a {@code ProtocolDoesNotExist} with the given detail message.
     *
     * @param message a description identifying which protocol was not found
     */
    public ProtocolDoesNotExist(String message) {
        super(message);
    }

    /**
     * Creates a {@code ProtocolDoesNotExist} with a detail message and a cause.
     *
     * @param message a description identifying which protocol was not found
     * @param cause   the underlying exception that triggered this error
     */
    public ProtocolDoesNotExist(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a {@code ProtocolDoesNotExist} wrapping an existing cause.
     *
     * @param cause the underlying exception that triggered this error
     */
    public ProtocolDoesNotExist(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a {@code ProtocolDoesNotExist} with full control over suppression and
     * stack-trace writability.
     *
     * @param message            a description identifying which protocol was not found
     * @param cause              the underlying exception that triggered this error
     * @param enableSuppression  whether suppressed exceptions may be added
     * @param writableStackTrace whether the stack trace should be writable
     */
    public ProtocolDoesNotExist(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
