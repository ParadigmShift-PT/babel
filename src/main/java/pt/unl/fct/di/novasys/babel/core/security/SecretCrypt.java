package pt.unl.fct.di.novasys.babel.core.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.function.Supplier;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import pt.unl.fct.di.novasys.babel.core.BabelSecurity;

/**
 * Class to do cryptographic operations over a specific secret key form the
 * internal key store.
 */
public class SecretCrypt {
    private final String alias;
    private final SecretKey key;
    private final String macAlgorithm;
    private final String cipherAlgorithm;
    private final Supplier<AlgorithmParameterSpec> cipherParams;

    private static final BabelSecurity babelSecurity = BabelSecurity.getInstance();

    /**
     * @throws NoSuchAlgorithmException
     * @see <a href=
     *      "https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html">Java
     *      Security Standard Algorithm Names</a>
     */
    public SecretCrypt(String alias, SecretKey key, String macAlgorithm, String cipherMode, String cipherPadding,
            Supplier<AlgorithmParameterSpec> cipherParams) throws NoSuchAlgorithmException {
        this(alias, key, macAlgorithm, "%s/%s/%s".formatted(key.getAlgorithm(), cipherMode, cipherPadding), cipherParams);
    }

    /**
     * @throws NoSuchAlgorithmException
     * @see <a href=
     *      "https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html">Java
     *      Security Standard Algorithm Names</a>
     */
    public SecretCrypt(String alias, SecretKey key, String macAlgorithm, String cipherAlgorithm, Supplier<AlgorithmParameterSpec> cipherParams)
            throws NoSuchAlgorithmException {
        this.alias = alias;
        this.key = key;
        this.macAlgorithm = macAlgorithm;
        this.cipherParams = cipherParams;

        var algorithms = Security.getAlgorithms("Cipher");
        // Some cipher algorithms (stream ciphers) don't use the default algorithm
        // naming format, and have the same name as the key algorithm, so this checks
        // if this might be the case.
        this.cipherAlgorithm = algorithms.contains(cipherAlgorithm)
                ? cipherAlgorithm
                : key.getAlgorithm();

        if (!algorithms.contains(cipherAlgorithm))
            throw new NoSuchAlgorithmException("Cipher algorithm not available: " + cipherAlgorithm);
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
            var mac = Mac.getInstance(macAlgorithm);
            mac.init(key);
            return mac;
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    /* ------------------------ CIPHER --------------------- */

    public byte[] decrypt(byte[] data, byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public int decrypt(ByteBuffer in, ByteBuffer out, byte[] iv)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public byte[] decrypt(byte[] data, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public int decrypt(ByteBuffer in, ByteBuffer out, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * @param See the Cipher section in the <a href=
     *            "https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html#cipher-algorithms">
     *            Java Security Standard Algorithm Names Specification</a>
     */
    public byte[] decrypt(byte[] data, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public int decrypt(ByteBuffer in, ByteBuffer out, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public CipherInputStream getDecryptStream(ByteArrayInputStream in, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public CipherOutputStream getDecryptStream(ByteArrayOutputStream out, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public CipherInputStream getDecryptStream(ByteArrayInputStream in, byte[] iv)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public CipherOutputStream getDecryptStream(ByteArrayOutputStream out, byte[] iv)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }
    public CipherInputStream getDecryptStream(ByteArrayInputStream in, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public CipherOutputStream getDecryptStream(ByteArrayOutputStream out, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public CipherInputStream getDecryptStream(ByteArrayInputStream in, String transformation, byte[] iv)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public CipherOutputStream getDecryptStream(ByteArrayOutputStream out, String transformation, byte[] iv)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public Cipher getDecryptCipher(byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException {
        return getDecryptCipher(new IvParameterSpec(iv));
    }

    public Cipher getDecryptCipher(AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            var cipher = Cipher.getInstance(cipherAlgorithm, babelSecurity.PROVIDER);
            cipher.init(Cipher.DECRYPT_MODE, key, params);
            return cipher;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    public CipherData encrypt(byte[] data) throws InvalidKeyException {
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
    public byte[] encrypt(byte[] data, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public int encrypt(ByteBuffer in, ByteBuffer out, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public CipherInputStream getEncryptStream(ByteArrayInputStream in)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public CipherOutputStream getEncryptStream(ByteArrayOutputStream out)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public CipherInputStream getEncryptStream(ByteArrayInputStream in, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public CipherOutputStream getEncryptStream(ByteArrayOutputStream out, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException("TODO");
    }

    public Cipher getEncryptCipher() throws InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            var cipher = Cipher.getInstance(cipherAlgorithm, babelSecurity.PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, key, cipherParams.get());
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

}
