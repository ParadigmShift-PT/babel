package pt.unl.fct.di.novasys.babel.generic.signed;

/**
 * Thrown when a {@link SignedProtoMessage} does not carry the expected binary serialization
 * (e.g. the message was never received from the network and therefore has no wire bytes to verify).
 */
@SuppressWarnings("serial")
public class InvalidFormatException extends Exception {

	/**
	 * Constructs a new {@code InvalidFormatException} with the given detail message.
	 *
	 * @param msg human-readable description of the format problem
	 */
	public InvalidFormatException(String msg) {
		super(msg);
	}

}
