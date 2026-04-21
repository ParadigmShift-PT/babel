package pt.unl.fct.di.novasys.babel.generic.signed;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

/**
 * <p>Abstract Message class to be extended by protocol-specific messages, similar to ProtoMessage.
 * Used just like ProtoMessage, but requires implementing the getSerializer() method.
 *
 * <p>This class also provides methods to sign and check the signature of the message, by calling the signMessage() and
 * checkSignature() methods.
 */
public abstract class SignedProtoMessage extends ProtoMessage {

	private static final String SignatureAlgorithm = "SHA256withRSA";
	
	private static final Logger logger = LogManager.getLogger(SignedProtoMessage.class);
	
	protected byte[] serializedMessage;
	protected byte[] signature;
	
	/**
	 * Constructs a new signed protocol message with the given type identifier.
	 * The {@code serializedMessage} and {@code signature} byte arrays are initially {@code null}
	 * and are populated on the first call to {@link #signMessage} or after network deserialization.
	 *
	 * @param id the numeric identifier that distinguishes this message type within its protocol
	 */
	public SignedProtoMessage(short id) {
		super(id);
		this.serializedMessage = null;
		this.signature = null;
	}

	/**
	 * Signs this message using SHA256withRSA and the supplied private key, storing the resulting
	 * signature in {@link #signature}. If the wire-format bytes ({@link #serializedMessage}) have
	 * not yet been computed, they are generated via {@link SignedMessageSerializer#serializeBody}
	 * before signing.
	 *
	 * @param key the RSA private key used to produce the signature
	 * @throws NoSuchAlgorithmException   if SHA256withRSA is not available in the JVM
	 * @throws InvalidKeyException        if {@code key} is not a valid RSA private key
	 * @throws SignatureException         if the signing operation fails
	 * @throws InvalidSerializerException if {@link #getSerializer()} returns {@code null}
	 */
	public final void signMessage(PrivateKey key) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidSerializerException {
		if(this.serializedMessage == null) {
			SignedMessageSerializer<SignedProtoMessage> serializer = (SignedMessageSerializer<SignedProtoMessage>) this.getSerializer();
			if(serializer == null) {
				throw new InvalidSerializerException("No Serializer available for type: " + this.getClass().getCanonicalName() +
						"\nVerify that the serializer exists and is returned by the method getSerializer()");
			} else {
				ByteBuf b = Unpooled.buffer();
				b.writeShort(this.getId());
				try {
					serializer.serializeBody(this, b);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				this.serializedMessage = ByteBufUtil.getBytes(b.slice());
			}
		}
		
		Signature sig = Signature.getInstance(SignedProtoMessage.SignatureAlgorithm);
		sig.initSign(key);
		sig.update(serializedMessage);
		this.signature = sig.sign();
	}
	
	/**
	 * Verifies the SHA256withRSA signature of this message against the supplied public key.
	 * Both the wire-format bytes and a non-empty signature must already be present (i.e. the
	 * message must have been received from the network and deserialized by a
	 * {@link SignedMessageSerializer}).
	 *
	 * @param key the RSA public key corresponding to the signer's private key
	 * @return {@code true} if the signature is valid, {@code false} otherwise
	 * @throws InvalidFormatException      if the wire-format bytes are absent
	 * @throws NoSignaturePresentException if no signature bytes are present
	 * @throws NoSuchAlgorithmException    if SHA256withRSA is not available in the JVM
	 * @throws InvalidKeyException         if {@code key} is not a valid RSA public key
	 * @throws SignatureException          if the verification operation fails
	 */
	public final boolean checkSignature(PublicKey key) throws InvalidFormatException, NoSignaturePresentException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		if(this.serializedMessage == null || this.serializedMessage.length == 0)
			throw new InvalidFormatException("Message serialization format is not present. Was this message received from the network?");
		if(this.signature == null || this.signature.length == 0)
			throw new NoSignaturePresentException("This message does not contain a signature. Was this message received from the network?");
		
		Signature sig = Signature.getInstance(SignedProtoMessage.SignatureAlgorithm);
		sig.initVerify(key);
		sig.update(this.serializedMessage);
		boolean valid = sig.verify(this.signature);
		if (!valid)
			logger.debug("Invalid signature on message: <" + this.getClass().getCanonicalName() + "> :: " + this.toString());
		return valid;
	}
	
	/**
	 * Returns the {@link SignedMessageSerializer} responsible for serializing and deserializing
	 * this message type. Implementations must not return {@code null}; doing so causes
	 * {@link #signMessage} to throw {@link InvalidSerializerException}.
	 *
	 * @return the serializer for this concrete message type
	 */
	public abstract SignedMessageSerializer<? extends SignedProtoMessage> getSerializer();

}
