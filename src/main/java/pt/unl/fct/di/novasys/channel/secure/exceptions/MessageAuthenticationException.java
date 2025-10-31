package pt.unl.fct.di.novasys.channel.secure.exceptions;

public class MessageAuthenticationException extends AuthenticationException {
    public MessageAuthenticationException() {
        super();
    }

    public MessageAuthenticationException(String message) {
        super(message);
    }

    public MessageAuthenticationException(Throwable cause) {
        super(cause);
    }

    public MessageAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
