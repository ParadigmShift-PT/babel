package pt.unl.fct.di.novasys.babel.exceptions;

/**
 * Thrown when an operation targets a protocol ID that is not currently registered and
 * executing in the Babel runtime.
 */
public class NoSuchProtocolException extends RuntimeException {

    /**
     * Creates a {@code NoSuchProtocolException} for the given protocol ID.
     *
     * @param protoId the numeric protocol ID that was not found
     */
    public NoSuchProtocolException(short protoId) { super(protoId + " not executing.");}

    /**
     * Creates a {@code NoSuchProtocolException} with a custom error message.
     *
     * @param error a description of the lookup failure
     */
    public NoSuchProtocolException(String error) { super(error);}

    /**
     * Creates a {@code NoSuchProtocolException} for the given protocol ID, wrapping a cause.
     *
     * @param protoId the numeric protocol ID that was not found
     * @param cause   the underlying exception that triggered this error
     */
    public NoSuchProtocolException(short protoId, Throwable cause) { super(protoId + " not executing.", cause);}

    /**
     * Creates a {@code NoSuchProtocolException} with full control over suppression and
     * stack-trace writability.
     *
     * @param protoId            the numeric protocol ID that was not found
     * @param cause              the underlying exception that triggered this error
     * @param enableSuppression  whether suppressed exceptions may be added
     * @param writableStackTrace whether the stack trace should be writable
     */
    public NoSuchProtocolException(short protoId, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(protoId + " not executing.", cause, enableSuppression, writableStackTrace);
    }
}
