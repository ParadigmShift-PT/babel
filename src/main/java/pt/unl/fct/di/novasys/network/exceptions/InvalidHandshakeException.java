package pt.unl.fct.di.novasys.network.exceptions;

public class InvalidHandshakeException extends Exception {

    public InvalidHandshakeException() {
        super();
    }

    public InvalidHandshakeException(String message) {
        super(message);
    }

    public InvalidHandshakeException(Throwable cause) {
        super(cause);
    }

    public InvalidHandshakeException(String message, Throwable cause) {
        super(message, cause);
    }

}
