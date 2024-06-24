package pt.unl.fct.di.novasys.babel.core.security.exceptions;

public class NoSuchIdentityException extends Exception {

    public NoSuchIdentityException() {
        super();
    }

    public NoSuchIdentityException(String message) {
        super(message);
    }

    public NoSuchIdentityException(Throwable cause) {
        super(cause);
    }

    public NoSuchIdentityException(String message, Throwable cause) {
        super(message, cause);
    }

}
