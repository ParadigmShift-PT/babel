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

    /**
     * Computes a MAC over one or more byte arrays using this secret key and the default MAC algorithm.
     *
     * @param data one or more byte arrays to authenticate
     * @return the MAC bytes
     * @throws InvalidKeyException if the secret key is inappropriate for the MAC algorithm
     */
    public byte[] mac(byte[]... data) throws InvalidKeyException {
        Mac mac = initMac();
        Arrays.stream(data).forEach(mac::update);
        return initMac().doFinal();
    }

    /**
     * Computes a MAC over a {@link ByteBuffer} using this secret key and the default MAC algorithm.
     *
     * @param data the buffer containing the data to authenticate
     * @return the MAC bytes
     * @throws InvalidKeyException if the secret key is inappropriate for the MAC algorithm
     */
    public byte[] mac(ByteBuffer data) throws InvalidKeyException {
        var mac = initMac();
        mac.update(data);
        return mac.doFinal();
    }

    /**
     * Computes a MAC over one or more byte arrays using this secret key and an explicit algorithm.
     *
     * @param algorithm the MAC algorithm name (e.g. {@code "HmacSHA256"})
     * @param data      one or more byte arrays to authenticate
     * @return the MAC bytes
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws InvalidKeyException      if the secret key is inappropriate for the algorithm
     */
    public byte[] mac(String algorithm, byte[]... data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = initMac(algorithm);
        Arrays.stream(data).forEach(mac::update);
        return initMac(algorithm).doFinal();
    }

    /**
     * Computes a MAC over a {@link ByteBuffer} using this secret key and an explicit algorithm.
     *
     * @param algorithm the MAC algorithm name
     * @param data      the buffer containing the data to authenticate
     * @return the MAC bytes
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws InvalidKeyException      if the secret key is inappropriate for the algorithm
     */
    public byte[] mac(String algorithm, ByteBuffer data) throws NoSuchAlgorithmException, InvalidKeyException {
        var mac = initMac(algorithm);
        mac.update(data);
        return mac.doFinal();
    }

    /**
     * Computes a MAC over one or more byte arrays using this secret key, an explicit algorithm, and parameters.
     *
     * @param algorithm the MAC algorithm name
     * @param params    algorithm parameters for MAC initialisation
     * @param data      one or more byte arrays to authenticate
     * @return the MAC bytes
     * @throws NoSuchAlgorithmException           if the algorithm is not available
     * @throws InvalidKeyException                if the secret key is inappropriate for the algorithm
     * @throws InvalidAlgorithmParameterException if the parameters are invalid for the algorithm
     */
    public byte[] mac(String algorithm, AlgorithmParameterSpec params, byte[]... data)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        Mac mac = initMac(algorithm, params);
        Arrays.stream(data).forEach(mac::update);
        return mac.doFinal();
    }

    /**
     * Computes a MAC over a {@link ByteBuffer} using this secret key, an explicit algorithm, and parameters.
     *
     * @param algorithm the MAC algorithm name
     * @param params    algorithm parameters for MAC initialisation
     * @param data      the buffer containing the data to authenticate
     * @return the MAC bytes
     * @throws NoSuchAlgorithmException           if the algorithm is not available
     * @throws InvalidKeyException                if the secret key is inappropriate for the algorithm
     * @throws InvalidAlgorithmParameterException if the parameters are invalid for the algorithm
     */
    public byte[] mac(String algorithm, AlgorithmParameterSpec params, ByteBuffer data)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        var mac = initMac(algorithm, params);
        mac.update(data);
        return mac.doFinal();
    }

    /**
     * Creates a {@link Mac} instance initialised with this secret key and the default MAC algorithm.
     *
     * @return an initialised {@link Mac} ready to receive data
     * @throws InvalidKeyException if the secret key is inappropriate for the algorithm
     */
    public Mac initMac() throws InvalidKeyException {
        try {
            return initMac(macAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    /**
     * Creates a {@link Mac} instance initialised with this secret key and an explicit algorithm.
     *
     * @param algorithm the MAC algorithm name
     * @return an initialised {@link Mac} ready to receive data
     * @throws InvalidKeyException      if the secret key is inappropriate for the algorithm
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
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

    /**
     * Creates a {@link Mac} instance initialised with this secret key, an explicit algorithm, and parameters.
     *
     * @param algorithm the MAC algorithm name
     * @param params    algorithm-specific parameters for MAC initialisation
     * @return an initialised {@link Mac} ready to receive data
     * @throws InvalidKeyException                if the secret key is inappropriate for the algorithm
     * @throws NoSuchAlgorithmException           if the algorithm is not available
     * @throws InvalidAlgorithmParameterException if the parameters are invalid for the algorithm
     */
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

    /**
     * Decrypts a byte array using this secret key, the default cipher transformation, and an explicit IV.
     *
     * @param data the cipher-text to decrypt
     * @param iv   the initialisation vector bytes
     * @return the decrypted plain-text
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the IV is invalid for the cipher
     * @throws IllegalBlockSizeException          if the data length is not valid for the cipher
     * @throws BadPaddingException                if the padding is incorrect
     */
    public byte[] decrypt(byte[] data, byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        return initDecryptCipher(iv).doFinal(data);
    }

    /**
     * Decrypts from a {@link ByteBuffer} into another using this secret key, the default cipher, and an explicit IV.
     *
     * @param in  the source buffer containing cipher-text
     * @param out the destination buffer for plain-text
     * @param iv  the initialisation vector bytes
     * @return the number of bytes written to {@code out}
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the IV is invalid for the cipher
     * @throws ShortBufferException               if {@code out} is too small to hold the result
     * @throws IllegalBlockSizeException          if the data length is not valid for the cipher
     * @throws BadPaddingException                if the padding is incorrect
     */
    public int decrypt(ByteBuffer in, ByteBuffer out, byte[] iv)
            throws InvalidKeyException, InvalidAlgorithmParameterException, ShortBufferException,
            IllegalBlockSizeException, BadPaddingException {
        return initDecryptCipher(iv).doFinal(in, out);
    }

    /**
     * Decrypts a byte array using this secret key, the default cipher transformation, and explicit parameters.
     *
     * @param data   the cipher-text to decrypt
     * @param params the cipher parameters (e.g. IV or GCM spec)
     * @return the decrypted plain-text
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the parameters are invalid for the cipher
     * @throws IllegalBlockSizeException          if the data length is not valid for the cipher
     * @throws BadPaddingException                if the padding is incorrect
     */
    public byte[] decrypt(byte[] data, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException {
        return initDecryptCipher(params).doFinal(data);
    }

    /**
     * Decrypts from a {@link ByteBuffer} into another using this secret key, the default cipher, and explicit parameters.
     *
     * @param in     the source buffer containing cipher-text
     * @param out    the destination buffer for plain-text
     * @param params the cipher parameters
     * @return the number of bytes written to {@code out}
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the parameters are invalid for the cipher
     * @throws ShortBufferException               if {@code out} is too small to hold the result
     * @throws IllegalBlockSizeException          if the data length is not valid for the cipher
     * @throws BadPaddingException                if the padding is incorrect
     */
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

    /**
     * Decrypts from a {@link ByteBuffer} into another using an explicit cipher transformation and parameters.
     *
     * @param in             the source buffer containing cipher-text
     * @param out            the destination buffer for plain-text
     * @param transformation the cipher transformation string (e.g. {@code "AES/GCM/NoPadding"})
     * @param params         the cipher parameters
     * @return the number of bytes written to {@code out}
     * @throws NoSuchAlgorithmException           if the transformation is not available
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws ShortBufferException               if {@code out} is too small
     * @throws IllegalBlockSizeException          if the data length is invalid
     * @throws BadPaddingException                if the padding is incorrect
     * @throws NoSuchPaddingException             if the padding scheme is not available
     */
    public int decrypt(ByteBuffer in, ByteBuffer out, String transformation, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException,
            ShortBufferException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException {
        return initDecryptCipher(transformation, params).doFinal(in, out);
    }

    /**
     * Returns a {@link CipherInputStream} that decrypts data read from {@code in} using the default
     * cipher transformation and an explicit IV.
     *
     * @param in the source stream of cipher-text
     * @param iv the initialisation vector bytes
     * @return a decrypting {@link CipherInputStream}
     * @throws NoSuchAlgorithmException           if the cipher algorithm is not available
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the IV is invalid
     */
    public CipherInputStream getDecryptionStream(ByteArrayInputStream in, byte[] iv)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        return new CipherInputStream(in, initDecryptCipher(iv));
    }

    /**
     * Returns a {@link CipherInputStream} that decrypts data read from {@code in} using the default
     * cipher transformation and explicit parameters.
     *
     * @param in     the source stream of cipher-text
     * @param params the cipher parameters (e.g. IV or GCM spec)
     * @return a decrypting {@link CipherInputStream}
     * @throws NoSuchAlgorithmException           if the cipher algorithm is not available
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     */
    public CipherInputStream getDecryptionStream(ByteArrayInputStream in, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        return new CipherInputStream(in, initDecryptCipher(params));
    }

    /**
     * Returns a {@link CipherInputStream} that decrypts data read from {@code in} using an explicit
     * cipher transformation and IV.
     *
     * @param in             the source stream of cipher-text
     * @param transformation the cipher transformation string
     * @param iv             the initialisation vector bytes
     * @return a decrypting {@link CipherInputStream}
     * @throws NoSuchAlgorithmException           if the transformation is not available
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the IV is invalid
     * @throws NoSuchPaddingException             if the padding scheme is not available
     */
    public CipherInputStream getDecryptionStream(ByteArrayInputStream in, String transformation, byte[] iv)
            throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException,
            NoSuchPaddingException {
        return new CipherInputStream(in, initDecryptCipher(transformation, iv));
    }

    /**
     * Returns a {@link CipherInputStream} that decrypts data read from {@code in} using an explicit
     * cipher transformation and parameters.
     *
     * @param in             the source stream of cipher-text
     * @param transformation the cipher transformation string
     * @param params         the cipher parameters
     * @return a decrypting {@link CipherInputStream}
     * @throws NoSuchAlgorithmException           if the transformation is not available
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws NoSuchPaddingException             if the padding scheme is not available
     */
    public CipherInputStream getDecryptionStream(ByteArrayInputStream in, String transformation,
            AlgorithmParameterSpec params) throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidAlgorithmParameterException, NoSuchPaddingException {
        return new CipherInputStream(in, initDecryptCipher(transformation, params));
    }

    /**
     * Creates a {@link Cipher} initialised for decryption with this secret key, the default
     * transformation, and an explicit IV byte array.
     *
     * @param iv the initialisation vector bytes
     * @return a {@link Cipher} in DECRYPT mode
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the IV is invalid
     */
    public Cipher initDecryptCipher(byte[] iv) throws InvalidKeyException, InvalidAlgorithmParameterException {
        return initDecryptCipher(new IvParameterSpec(iv));
    }

    /**
     * Creates a {@link Cipher} initialised for decryption with this secret key, the default
     * transformation, and explicit parameters.
     *
     * @param params the cipher parameters (e.g. IV or GCM spec)
     * @return a {@link Cipher} in DECRYPT mode
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     */
    public Cipher initDecryptCipher(AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            return initDecryptCipher(cipherTransform, params);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    /**
     * Creates a {@link Cipher} initialised for decryption with this secret key, an explicit transformation,
     * and an IV byte array.
     *
     * @param transformation the cipher transformation string
     * @param iv             the initialisation vector bytes
     * @return a {@link Cipher} in DECRYPT mode
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the IV is invalid
     * @throws NoSuchAlgorithmException           if the transformation is not available
     * @throws NoSuchPaddingException             if the padding scheme is not available
     */
    public Cipher initDecryptCipher(String transformation, byte[] iv) throws InvalidKeyException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
        return initDecryptCipher(transformation, new IvParameterSpec(iv));
    }

    /**
     * Creates a {@link Cipher} initialised for decryption with this secret key, an explicit transformation,
     * and explicit parameters.
     *
     * @param transformation the cipher transformation string
     * @param params         the cipher parameters
     * @return a {@link Cipher} in DECRYPT mode
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws NoSuchAlgorithmException           if the transformation is not available
     * @throws NoSuchPaddingException             if the padding scheme is not available
     */
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

    /**
     * Encrypts a byte array using this secret key and the default cipher transformation.
     * The cipher parameters (e.g. the generated IV) are included in the returned {@link CipherData}.
     *
     * @param data the plain-text to encrypt
     * @return a {@link CipherData} containing the cipher-text and the cipher parameters
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the generated parameters are invalid
     * @throws IllegalBlockSizeException          if the data length is not valid for the cipher
     */
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

    /**
     * Encrypts from a {@link ByteBuffer} into another using this secret key and the default cipher transformation.
     *
     * @param in  the source buffer containing plain-text
     * @param out the destination buffer for cipher-text
     * @return the {@link AlgorithmParameters} used during encryption (e.g. containing the IV)
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the generated parameters are invalid
     * @throws ShortBufferException               if {@code out} is too small to hold the result
     * @throws IllegalBlockSizeException          if the data length is not valid for the cipher
     */
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

    /**
     * Encrypts from a {@link ByteBuffer} into another using an explicit cipher transformation and parameters.
     *
     * @param in             the source buffer containing plain-text
     * @param out            the destination buffer for cipher-text
     * @param transformation the cipher transformation string
     * @param params         the cipher parameters to use for encryption
     * @return the {@link AlgorithmParameters} used during encryption
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws NoSuchPaddingException             if the padding scheme is not available
     * @throws NoSuchAlgorithmException           if the transformation is not available
     * @throws ShortBufferException               if {@code out} is too small
     * @throws IllegalBlockSizeException          if the data length is not valid for the cipher
     */
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

    /**
     * Returns an encrypting {@link CipherOutputStream} wrapping {@code out}, using this secret key and
     * the default cipher transformation with auto-generated parameters.
     *
     * @param out the underlying output stream to write cipher-text to
     * @return a pair of the wrapping {@link CipherOutputStream} and the {@link AlgorithmParameters} used
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the generated parameters are invalid
     */
    public Pair<CipherOutputStream, AlgorithmParameters> getEncryptionStream(ByteArrayOutputStream out)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        var cipher = initEncryptCipher();
        return Pair.of(new CipherOutputStream(out, cipher), cipher.getParameters());
    }

    /**
     * Returns an encrypting {@link CipherOutputStream} wrapping {@code out}, using this secret key,
     * an explicit transformation, and explicit parameters.
     *
     * @param out            the underlying output stream to write cipher-text to
     * @param transformation the cipher transformation string
     * @param params         the cipher parameters to use for encryption
     * @return a pair of the wrapping {@link CipherOutputStream} and the {@link AlgorithmParameters} used
     * @throws NoSuchAlgorithmException           if the transformation is not available
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws NoSuchPaddingException             if the padding scheme is not available
     */
    public Pair<CipherOutputStream, AlgorithmParameters> getEncryptionStream(ByteArrayOutputStream out,
            String transformation, AlgorithmParameterSpec params) throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidAlgorithmParameterException, NoSuchPaddingException {
        var cipher = initEncryptCipher(transformation, params);
        return Pair.of(new CipherOutputStream(out, cipher), cipher.getParameters());
    }

    /**
     * Creates a {@link Cipher} initialised for encryption with this secret key and the default
     * transformation, generating fresh cipher parameters via the configured supplier.
     *
     * @return a {@link Cipher} in ENCRYPT mode
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the generated parameters are invalid
     */
    public Cipher initEncryptCipher() throws InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            return initEncryptCipher(cipherTransform, cipherParamSupplier.get());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    /**
     * Creates a {@link Cipher} initialised for encryption with this secret key, an explicit transformation,
     * and an IV byte array.
     *
     * @param transformation the cipher transformation string
     * @param iv             the initialisation vector bytes
     * @return a {@link Cipher} in ENCRYPT mode
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the IV is invalid
     * @throws NoSuchAlgorithmException           if the transformation is not available
     * @throws NoSuchPaddingException             if the padding scheme is not available
     */
    public Cipher initEncryptCipher(String transformation, byte[] iv) throws InvalidKeyException,
            InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
        return initEncryptCipher(transformation, new IvParameterSpec(iv));
    }

    /**
     * Creates a {@link Cipher} initialised for encryption with this secret key, an explicit transformation,
     * and explicit parameters.
     *
     * @param transformation the cipher transformation string
     * @param params         the cipher parameters
     * @return a {@link Cipher} in ENCRYPT mode
     * @throws InvalidKeyException                if the secret key is inappropriate
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws NoSuchPaddingException             if the padding scheme is not available
     * @throws NoSuchAlgorithmException           if the transformation is not available
     */
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
