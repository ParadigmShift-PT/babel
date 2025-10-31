package pt.unl.fct.di.novasys.network.exceptions;

import pt.unl.fct.di.novasys.network.data.Attributes;

public class InvalidHandshakeAttributesException extends InvalidHandshakeException {

    private final Attributes attributes;

    public InvalidHandshakeAttributesException(Attributes attributes) {
        super();
        this.attributes = attributes;
    }

    public InvalidHandshakeAttributesException(Attributes attributes, String message) {
        super(message);
        this.attributes = attributes;
    }

    public InvalidHandshakeAttributesException(Attributes attributes, int handshakeStep) {
        super("Invalid attributes for the handshake step number " + handshakeStep);
        this.attributes = attributes;
    }

    public InvalidHandshakeAttributesException(Attributes attributes, Throwable cause) {
        super(cause);
        this.attributes = attributes;
    }

    public InvalidHandshakeAttributesException(Attributes attributes, int handshakeStep, Throwable cause) {
        super("Invalid attributes for the handshake step number " + handshakeStep, cause);
        this.attributes = attributes;
    }

    public InvalidHandshakeAttributesException(Attributes attributes, String message, Throwable cause) {
        super(message, cause);
        this.attributes = attributes;
    }

    public Attributes getAttributes() {
        return attributes;
    }

}
