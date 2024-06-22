package pt.unl.fct.di.novasys.babel.core.security;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Class to do cryptographic operations over a specific id and its asymmetric
 * key pair from the internal key store.
 */
public class IdCrypt {
    private final String alias;
    private final byte[] id;

    private final PrivateKey privKey;
    private final PublicKey pubKey;
    private final Certificate[] certChain;
    private final String defaultSigAlg;
    private final AlgorithmParameterSpec defaultSigParams;
    private final String defaultCipherTransformation;
    private final AlgorithmParameterSpec defaultCipherParams;
    private final String defaultKeyWrappingAlg;

    public IdCrypt(String alias, byte[] id, PrivateKey privKey, PublicKey pubKey, Certificate[] certChain,
            String defaultSigAlg, AlgorithmParameterSpec defaultSigParams, String defaultCipherTransformation,
            AlgorithmParameterSpec defaultCipherParams, String defaultKeyWrappingAlg) {
        this.alias = alias;
        this.id = id;
        this.privKey = privKey;
        this.pubKey = pubKey;
        this.certChain = certChain;
        this.defaultSigAlg = defaultSigAlg;
        this.defaultSigParams = defaultSigParams;
        this.defaultCipherTransformation = defaultCipherTransformation;
        this.defaultCipherParams = defaultCipherParams;
        this.defaultKeyWrappingAlg = defaultKeyWrappingAlg;
    }

    /**
     * @return The id's alias in the key store it is stored.
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @return The id.
     */
    public byte[] getId() {
        return id;
    }

    /**
     * @return The id's private key
     */
    public PrivateKey getPrivateKey() {
        return privKey;
    }

    /**
     * @return The id's public key
     */
    public PublicKey getPublicKey() {
        return pubKey;
    }

    /**
     * @return The id's key pair algorithm
     */
    public String getKeysAlgorithm() {
        assert pubKey.getAlgorithm() == privKey.getAlgorithm();
        return privKey.getAlgorithm();
    }

    /**
     * @return The id's certificate chain
     */
    public Certificate[] getCertChain() {
        return certChain;
    }

    /* ------------------------ SIGNING --------------------- */

    public byte[] sign(byte[] data) throws InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public byte[] sign(ByteBuffer data) throws InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public byte[] sign(byte[] data, String algorithm) throws InvalidKeyException, NoSuchAlgorithmException {
        throw new UnsupportedOperationException("TODO");
    }

    public byte[] sign(ByteBuffer data, String algorithm) throws InvalidKeyException, NoSuchAlgorithmException {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * @param algorithm An uninitialized {@link Signature} implementation gotten
     *                  from {@link Signature#getInstance}.
     */
    public byte[] sign(byte[] data, Signature algorithm) throws InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public byte[] sign(ByteBuffer data, Signature algorithm) throws InvalidKeyException {
        throw new UnsupportedOperationException("TODO");
    }

    public Signature getSignature() throws InvalidKeyException {
        try {
            var sig = Signature.getInstance(defaultSigAlg);
            sig.setParameter(defaultSigParams);
            sig.initSign(privKey);
            return sig;
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    /* ------------------------ DECRYPTING --------------------- */

    // Decrypt with private key (asymmetric encryption should be done through an
    // external API, as its dependant on others' public keys)

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

    public SecretKey unwrapSecretKey(byte[] wrappedKey, String keyAlgorithm) throws InvalidKeyException {
        try {
            return unwrapSecretKey(wrappedKey, keyAlgorithm, defaultKeyWrappingAlg);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    public SecretKey unwrapSecretKey(byte[] wrappedKey, String keyAlgorithm, String wrappingTransformation)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(wrappingTransformation);
        cipher.init(Cipher.UNWRAP_MODE, privKey);
        return (SecretKey) cipher.unwrap(wrappedKey, keyAlgorithm, Cipher.SECRET_KEY);
    }

    public Cipher getDecryptCipher() throws InvalidKeyException {
        try {
            var cipher = Cipher.getInstance(defaultCipherTransformation);
            cipher.init(Cipher.DECRYPT_MODE, privKey, defaultCipherParams);
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

}
