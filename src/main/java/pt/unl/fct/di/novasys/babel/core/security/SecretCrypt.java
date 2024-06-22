package pt.unl.fct.di.novasys.babel.core.security;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Class to do cryptographic operations over a specific secret key form the
 * internal key store.
 */
public class SecretCrypt {
    private final String alias;
    private final SecretKey key;
    private final String defaultMacAlg;
    private final AlgorithmParameterSpec defaultMacParams;
    private final String defaultCipherTransformation;
    private final AlgorithmParameterSpec defaultCipherParams;

    public SecretCrypt(String alias, SecretKey key, String defaultMacAlg, AlgorithmParameterSpec defaultMacParams,
            String defaultCipherTransformation, AlgorithmParameterSpec defaultCipherParams) {
        this.alias = alias;
        this.key = key;
        this.defaultMacAlg = defaultMacAlg;
        this.defaultMacParams = defaultMacParams;
        this.defaultCipherTransformation = defaultCipherTransformation;
        this.defaultCipherParams = defaultCipherParams;
    }

    /**
     * @return The alias in the key store the key is stored.
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @return The secret key.
     */
    public SecretKey getKey() {
        return key;
    }

    /* ------------------------ MAC --------------------- */

    public byte[] mac(byte[] data) throws InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public byte[] mac(ByteBuffer data) throws InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    // Mac with default secret
    // throws InvalidKeyException if the internal key is innapropriate for
    // initializing the mac with the given algorithm
    public byte[] mac(byte[] data, String algorithm) throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public byte[] mac(ByteBuffer data, String algorithm) throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    // Mac with default secret
    // throws InvalidKeyException if the internal key is innapropriate for
    // initializing the mac with the given algorithm
    public byte[] mac(byte[] data, String algorithm, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public byte[] mac(ByteBuffer data, String algorithm, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public Mac getMac() throws InvalidKeyException {
        try {
            var mac = Mac.getInstance(defaultMacAlg);
            mac.init(key, defaultMacParams);
            return mac;
        } catch (UnsupportedOperationException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    /* ------------------------ CIPHER --------------------- */

    public byte[] decrypt(byte[] data) throws InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public int decrypt(ByteBuffer in, ByteBuffer out) throws InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * @param See the Cipher section in the <a href=
     *            "https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html#cipher-algorithms">
     *            Java Security Standard Algorithm Names Specification</a>
     */
    public byte[] decrypt(byte[] data, String transformation) throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public int decrypt(ByteBuffer in, ByteBuffer out, String transformation) throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public byte[] decrypt(byte[] data, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public int decrypt(ByteBuffer in, ByteBuffer out, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public Cipher getDecryptCipher() throws InvalidKeyException {
        try {
            var cipher = Cipher.getInstance(defaultCipherTransformation);
            cipher.init(Cipher.DECRYPT_MODE, key, defaultCipherParams);
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    public byte[] encrypt(byte[] data) throws InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public int encrypt(ByteBuffer in, ByteBuffer out) throws InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * @param See the Cipher section in the <a href=
     *            "https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html#cipher-algorithms">
     *            Java Security Standard Algorithm Names Specification</a>
     */
    public byte[] encrypt(byte[] data, String transformation) throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public int encrypt(ByteBuffer in, ByteBuffer out, String transformation) throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public byte[] encrypt(byte[] data, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public int encrypt(ByteBuffer in, ByteBuffer out, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public Cipher getEncryptCipher() throws InvalidKeyException {
        try {
            var cipher = Cipher.getInstance(defaultCipherTransformation);
            cipher.init(Cipher.ENCRYPT_MODE, key, defaultCipherParams);
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

}
