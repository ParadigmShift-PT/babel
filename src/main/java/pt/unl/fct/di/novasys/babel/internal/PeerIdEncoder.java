package pt.unl.fct.di.novasys.babel.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;

import pt.unl.fct.di.novasys.babel.internal.security.CryptUtils;

public class PeerIdEncoder {
    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final Base64.Decoder decoder = Base64.getDecoder();

    private static final String DEFAULT_HASH_ALG = "SHA256";

    public static String encodeToString(byte[] peerId) {
        return encoder.encodeToString(peerId);
    }

    public static byte[] decodeString(String peerId) {
        return decoder.decode(peerId);
    }

    public static byte[] fromPublicKey(PublicKey publicKey, String hashAlgorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(hashAlgorithm, CryptUtils.PROVIDER);
            digest.update(publicKey.getEncoded());
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException("Unhandled exception: " + e);
        }
    }

    public static byte[] fromPublicKey(PublicKey publicKey) {
        return fromPublicKey(publicKey, DEFAULT_HASH_ALG);
    }

    public static String stringFromPublicKey(PublicKey publicKey, String hashAlgorithm) {
        return encodeToString( fromPublicKey(publicKey, hashAlgorithm) );
    }

    public static String stringFromPublicKey(PublicKey publicKey) {
        return encodeToString( fromPublicKey(publicKey) );
    }

}
