package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;

public class PeerIdEncoder {
    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final Base64.Decoder decoder = Base64.getDecoder();

    private static final String DEFAULT_HASH_ALG = "SHA256";

    public static String encodeToString(byte[] peerId) {
        return encoder.encodeToString(peerId);
    }

    public static byte[] decode(String peerId) {
        return decoder.decode(peerId);
    }

    public static String withoutEscapeBackslashes(String peerId) {
        return peerId.replaceAll("\\\\(\\\\)?", "$1");
    }

    public static byte[] fromPublicKey(PublicKey publicKey, String hashAlgorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(hashAlgorithm, CryptUtils.PROVIDER);
        digest.update(publicKey.getEncoded());
        return digest.digest();
    }

    public static byte[] fromPublicKey(PublicKey publicKey) {
        try {
            return fromPublicKey(publicKey, DEFAULT_HASH_ALG);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Shouldn't happen
        }
    }

    public static String stringFromPublicKey(PublicKey publicKey, String hashAlgorithm)
            throws NoSuchAlgorithmException {
        return encodeToString(fromPublicKey(publicKey, hashAlgorithm));
    }

    public static String stringFromPublicKey(PublicKey publicKey) {
        return encodeToString(fromPublicKey(publicKey));
    }

}
