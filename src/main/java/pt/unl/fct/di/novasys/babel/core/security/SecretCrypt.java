package pt.unl.fct.di.novasys.babel.core.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.function.Supplier;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.lang3.tuple.Pair;

import pt.unl.fct.di.novasys.babel.core.BabelSecurity;

/**
 * Class to do cryptographic operations over a specific secret key form the
 * internal key store.
 */
public class SecretCrypt {
    private final String alias;
    private final SecretKey key;
    private final String macAlgorithm;
    private final String cipherTransform;
    private final Supplier<AlgorithmParameterSpec> cipherParamSupplier;

    private static final BabelSecurity babelSecurity = BabelSecurity.getInstance();

    /**
     * @throws NoSuchAlgorithmException
     * @see <a href=
     *      "https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html">Java
     *      Security Standard Algorithm Names</a>
     */
    public SecretCrypt(String alias, SecretKey key, String macAlgorithm, String cipherMode, String cipherPadding,
            Supplier<AlgorithmParameterSpec> cipherParamSupplier) throws NoSuchAlgorithmException {
        this(alias, key, macAlgorithm, "%s/%s/%s".formatted(key.getAlgorithm(), cipherMode, cipherPadding),
                cipherParamSupplier);
    }

    /**
     * @throws NoSuchAlgorithmException
     * @see <a href=
     *      "https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html">Java
     *      Security Standard Algorithm Names</a>
     */
    public SecretCrypt(String alias, SecretKey key, String macAlgorithm, String cipherTransform,
            Supplier<AlgorithmParameterSpec> cipherParamSupplier)
            throws NoSuchAlgorithmException {
        this.alias = alias;
        this.key = key;
        this.macAlgorithm = macAlgorithm;
        this.cipherParamSupplier = cipherParamSupplier;

        var algorithms = Security.getAlgorithms("Cipher");
        // Some cipher algorithms (stream ciphers) don't use the default algorithm
        // naming format, and have the same name as the key algorithm, so this checks
        // if this might be the case.
        this.cipherTransform = algorithms.contains(cipherTransform.toUpperCase())
                ? cipherTransform
                : key.getAlgorithm();

        if (!algorithms.contains(cipherTransform.toUpperCase()))
            throw new NoSuchAlgorithmException("Cipher algorithm not available: " + cipherTransform);
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

    public byte[] mac(byte[]... data) throws InvalidKeyException {
        Mac mac = initMac();
        Arrays.stream(data).forEach(mac::update);
        return initMac().doFinal();
    }

    public byte[] mac(ByteBuffer data) throws InvalidKeyException {
        var mac = initMac();
        mac.update(data);
        return mac.doFinal();
    }

    // Mac with default secret
    // throws InvalidKeyException if the internal key is innapropriate for
    // initializing the mac with the given algorithm
    public byte[] mac(String algorithm, byte[]... data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = initMac(algorithm);
        Arrays.stream(data).forEach(mac::update);
        return initMac(algorithm).doFinal();
    }

    public byte[] mac(String algorithm, ByteBuffer data) throws NoSuchAlgorithmException, InvalidKeyException {
        var mac = initMac(algorithm);
        mac.update(data);
        return mac.doFinal();
    }

    // Mac with default secret
    // throws InvalidKeyException if the internal key is innapropriate for
    // initializing the mac with the given algorithm
    public byte[] mac(String algorithm, AlgorithmParameterSpec params, byte[]... data)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        Mac mac = initMac(algorithm, params);
        Arrays.stream(data).forEach(mac::update);
        return mac.doFinal();
    }

    public byte[] mac(String algorithm, AlgorithmParameterSpec params, ByteBuffer data)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        var mac = initMac(algorithm, params);
        mac.update(data);
        return mac.doFinal();
    }

    public Mac initMac() throws InvalidKeyException {
        try {
            return initMac(macAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    public Mac initMac(String algorithm) throws InvalidKeyException, NoSuchAlgorithmException {
        Mac mac;
        try {
            mac = Mac.getInstance(algorithm, babelSecurity.PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            mac = Mac.getInstance(algorithm);
        }
        mac.init(key);
        return mac;
    }

    public Mac initMac(String algorithm, AlgorithmParameterSpec params)
            throws InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        Mac mac;
        try {
            mac = Mac.getInstance(algorithm, babelSecurity.PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            mac = Mac.getInstance(algorithm);
        }
        mac.init(key, params);
        return mac;
    }

    /* ------------------------ CIPHER --------------------- */

    public byte[] decrypt(byte[] data, byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        return initDecryptCipher(iv).doFinal(data);
    }

    public int decrypt(ByteBuffer in, ByteBuffer out, byte[] iv)
            throws InvalidKeyException, InvalidAlgorithmParameterException, ShortBufferException,
            IllegalBlockSizeException, BadPaddingException {
        return initDecryptCipher(iv).doFinal(in, out);
    }

    public byte[] decrypt(byte[] data, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException {
        return initDecryptCipher(params).doFinal(data);
    }

    public int decrypt(ByteBuffer in, ByteBuffer out, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException, ShortBufferException,
            IllegalBlockSizeException, BadPaddingException {
        return initDecryptCipher(params).doFinal(in, out);
    }

    /**
     * @see <a href=
     *      "https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html#cipher-algorithms">
     *      Java Security Standard Algorithm Names Specification</a>
     * @throws NoSuchPaddingException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public byte[] decrypt(byte[] data, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException {
        return initDecryptCipher(transformation, params).doFinal(data);
    }

    public int decrypt(ByteBuffer in, ByteBuffer out, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException,
            ShortBufferException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException {
        return initDecryptCipher(transformation, params).doFinal(in, out);
    }

    public CipherInputStream getDecryptionStream(ByteArrayInputStream in, byte[] iv)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        return new CipherInputStream(in, initDecryptCipher(iv));
    }

    public CipherInputStream getDecryptionStream(ByteArrayInputStream in, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        return new CipherInputStream(in, initDecryptCipher(params));
    }

    public CipherInputStream getDecryptionStream(ByteArrayInputStream in, String transformation, byte[] iv)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException,
            NoSuchPaddingException {
        return new CipherInputStream(in, initDecryptCipher(transformation, iv));
    }

    public CipherInputStream getDecryptionStream(ByteArrayInputStream in, String transformation,
            AlgorithmParameterSpec params) throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidAlgorithmParameterException, NoSuchPaddingException {
        return new CipherInputStream(in, initDecryptCipher(transformation, params));
    }

    public Cipher initDecryptCipher(byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException {
        return initDecryptCipher(new IvParameterSpec(iv));
    }

    public Cipher initDecryptCipher(AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            return initDecryptCipher(cipherTransform, params);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    public Cipher initDecryptCipher(String transformation, byte[] iv) throws InvalidKeyException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
        return initDecryptCipher(transformation, new IvParameterSpec(iv));
    }

    public Cipher initDecryptCipher(String transformation, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchPaddingException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(transformation, babelSecurity.PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            cipher = Cipher.getInstance(transformation);
        }
        cipher.init(Cipher.DECRYPT_MODE, key, params);
        return cipher;
    }

    // --------

    public CipherData encrypt(byte[] data)
            throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException {
        try {
            var cipher = initEncryptCipher();
            byte[] cipherText = cipher.doFinal(data);
            return new CipherData(cipherText, cipher.getParameters());
        } catch (BadPaddingException never) {
            throw new AssertionError(never); // Only happens in decryption mode
        }
    }

    public AlgorithmParameters encrypt(ByteBuffer in, ByteBuffer out) throws InvalidKeyException,
            InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException {
        try {
            var cipher = initEncryptCipher();
            cipher.doFinal(in, out);
            return cipher.getParameters();
        } catch (BadPaddingException never) {
            throw new AssertionError(never); // Only happens in decryption mode
        }
    }

    /**
     * @see <a href=
     *      "https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html#cipher-algorithms">
     *      Java Security Standard Algorithm Names Specification</a>
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     */
    public CipherData encrypt(byte[] data, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException {
        try {
            var cipher = initEncryptCipher();
            byte[] cipherText = cipher.doFinal(data);
            return new CipherData(cipherText, cipher.getParameters());
        } catch (BadPaddingException never) {
            throw new AssertionError(never); // Only happens in decryption mode
        }
    }

    public AlgorithmParameters encrypt(ByteBuffer in, ByteBuffer out, String transformation,
            AlgorithmParameterSpec params) throws InvalidKeyException, InvalidAlgorithmParameterException,
            NoSuchPaddingException, NoSuchAlgorithmException, ShortBufferException, IllegalBlockSizeException {
        try {
            var cipher = initEncryptCipher(transformation, params);
            cipher.doFinal(in, out);
            return cipher.getParameters();
        } catch (BadPaddingException never) {
            throw new AssertionError(never); // Only happens in decryption mode
        }
    }

    public Pair<CipherOutputStream, AlgorithmParameters> getEncryptionStream(ByteArrayOutputStream out)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        var cipher = initEncryptCipher();
        return Pair.of(new CipherOutputStream(out, cipher), cipher.getParameters());
    }

    public Pair<CipherOutputStream, AlgorithmParameters> getEncryptionStream(ByteArrayOutputStream out,
            String transformation, AlgorithmParameterSpec params) throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidAlgorithmParameterException, NoSuchPaddingException {
        var cipher = initEncryptCipher(transformation, params);
        return Pair.of(new CipherOutputStream(out, cipher), cipher.getParameters());
    }

    public Cipher initEncryptCipher() throws InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            return initEncryptCipher(cipherTransform, cipherParamSupplier.get());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    public Cipher initEncryptCipher(String transformation, byte[] iv) throws InvalidKeyException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
        return initEncryptCipher(transformation, new IvParameterSpec(iv));
    }

    public Cipher initEncryptCipher(String transformation, AlgorithmParameterSpec params) throws InvalidKeyException,
            InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(transformation, babelSecurity.PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            cipher = Cipher.getInstance(transformation);
        }
        cipher.init(Cipher.ENCRYPT_MODE, key, params);
        return cipher;
    }

}
