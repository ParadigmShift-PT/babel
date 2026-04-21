package pt.unl.fct.di.novasys.babel.generic.signed;

/**
 * Thrown when a {@link SignedProtoMessage} cannot be signed because its {@code getSerializer()}
 * method returns {@code null} or a serializer that is not a {@link SignedMessageSerializer}.
 */
@SuppressWarnings("serial")
public class InvalidSerializerException extends Exception {

	/**
	 * Constructs a new {@code InvalidSerializerException} with the given detail message.
	 *
	 * @param msg human-readable description of the serializer problem
	 */
	public InvalidSerializerException(String msg) {
		super(msg);
	}

}
