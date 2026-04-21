package pt.unl.fct.di.novasys.babel.generic.signed;

/**
 * Thrown when {@link SignedProtoMessage#checkSignature} is called on a message that carries
 * no signature bytes — typically a locally constructed message that was never signed and sent
 * over the network.
 */
@SuppressWarnings("serial")
public class NoSignaturePresentException extends Exception {

	/**
	 * Constructs a new {@code NoSignaturePresentException} with the given detail message.
	 *
	 * @param msg human-readable description of why no signature is present
	 */
	public NoSignaturePresentException(String msg) {
		super(msg);
	}

}
