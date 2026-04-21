package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;

import pt.unl.fct.di.novasys.babel.core.BabelSecurity;

/**
 * Utility class for deriving, encoding, and decoding Babel peer identity bytes.
 * Peer IDs are computed as the hash of a public key's encoded form (default: SHA-256)
 * and serialised as standard Base64 strings.
 */
public class PeerIdEncoder {
    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final Base64.Decoder decoder = Base64.getDecoder();

    private static final String DEFAULT_HASH_ALG = "SHA256";

    /**
     * Encodes raw peer identity bytes as a Base64 string.
     *
     * @param peerId raw peer identity bytes
     * @return Base64-encoded representation of {@code peerId}
     */
    public static String encodeToString(byte[] peerId) {
        return encoder.encodeToString(peerId);
    }

    /**
     * Decodes a Base64-encoded peer identity string back to raw bytes.
     *
     * @param peerId Base64-encoded peer ID string
     * @return the decoded raw identity bytes
     */
    public static byte[] decode(String peerId) {
        return decoder.decode(peerId);
    }

    /**
     * Removes BouncyCastle-style escape backslashes from an X.500 string value,
     * normalising the peer ID string extracted from a certificate subject.
     *
     * @param peerId raw string as extracted from the certificate (may contain escape sequences)
     * @return the unescaped peer ID string
     */
    public static String withoutEscapeBackslashes(String peerId) {
        return peerId.replaceAll("\\\\(\\\\)?", "$1");
    }

    /**
     * Computes the peer identity by hashing the already-encoded (DER) form of a public key.
     *
     * @param publicKey     DER-encoded public key bytes
     * @param hashAlgorithm hash algorithm name (e.g. {@code "SHA256"})
     * @return the hash digest used as the peer identity
     * @throws NoSuchAlgorithmException if {@code hashAlgorithm} is not available
     */
    public static byte[] fromEncodedPublicKey(byte[] publicKey, String hashAlgorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(hashAlgorithm, BabelSecurity.getInstance().PROVIDER);
        digest.update(publicKey);
        return digest.digest();
    }

    /**
     * Computes the peer identity by hashing the DER encoding of the given {@link PublicKey}.
     *
     * @param publicKey     the public key to derive an identity from
     * @param hashAlgorithm hash algorithm name (e.g. {@code "SHA256"})
     * @return the hash digest used as the peer identity
     * @throws NoSuchAlgorithmException if {@code hashAlgorithm} is not available
     */
    public static byte[] fromPublicKey(PublicKey publicKey, String hashAlgorithm) throws NoSuchAlgorithmException {
        return fromEncodedPublicKey(publicKey.getEncoded(), hashAlgorithm);
    }

    /**
     * Computes the peer identity using the default hash algorithm (SHA-256).
     *
     * @param publicKey the public key to derive an identity from
     * @return the SHA-256 hash of the DER-encoded public key
     */
    public static byte[] fromPublicKey(PublicKey publicKey) {
        try {
            return fromPublicKey(publicKey, DEFAULT_HASH_ALG);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Shouldn't happen
        }
    }

    /**
     * Computes the peer identity and returns it as a Base64 string, using the specified hash algorithm.
     *
     * @param publicKey     the public key to derive an identity from
     * @param hashAlgorithm hash algorithm name (e.g. {@code "SHA256"})
     * @return Base64-encoded peer identity string
     * @throws NoSuchAlgorithmException if {@code hashAlgorithm} is not available
     */
    public static String stringFromPublicKey(PublicKey publicKey, String hashAlgorithm)
            throws NoSuchAlgorithmException {
        return encoder.encodeToString(fromPublicKey(publicKey, hashAlgorithm));
    }

    /**
     * Computes the peer identity using the default hash algorithm (SHA-256) and returns it as a Base64 string.
     *
     * @param publicKey the public key to derive an identity from
     * @return Base64-encoded peer identity string
     */
    public static String stringFromPublicKey(PublicKey publicKey) {
        return encoder.encodeToString(fromPublicKey(publicKey));
    }

}
