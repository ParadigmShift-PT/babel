package pt.unl.fct.di.novasys.babel.core.security;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;

import pt.unl.fct.di.novasys.babel.core.BabelSecurity;

/**
 * Class to do cryptographic operations over a specific id and its asymmetric
 * key pair from the internal key store.
 */
public class IdentityCrypt {
    private final String alias;
    private final byte[] id;

    private final PrivateKey privKey;
    private final PublicKey pubKey;
    private final Certificate[] certChain;
    private final String signatureAlgorithm;

    private static final BabelSecurity babelSecurity = BabelSecurity.getInstance();

    /**
     * @throws NoSuchAlgorithmException 
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html#signature-algorithms">Java Security Standard Algorithm Names</a>
     */
    public IdentityCrypt(String alias, byte[] id, PrivateKey privKey, PublicKey pubKey, Certificate[] certChain,
            String signatureHashOrAlgorithm) throws NoSuchAlgorithmException {
        this.alias = alias;
        this.id = id;
        this.privKey = privKey;
        this.pubKey = pubKey;
        this.certChain = certChain;

        var algorithms = Security.getAlgorithms("Signature");
        this.signatureAlgorithm = algorithms.contains(signatureHashOrAlgorithm.toUpperCase())
                ? signatureHashOrAlgorithm
                : signatureHashOrAlgorithm + "WITH" + privKey.getAlgorithm();

        if (!algorithms.contains(this.signatureAlgorithm.toUpperCase()))
            throw new NoSuchAlgorithmException("Signature hash or algorithm not available: " + signatureHashOrAlgorithm);
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

    public byte[] sign(byte[]... data) throws InvalidKeyException, SignatureException {
        var sig = initSignature();
        for (byte[] part : data)
            sig.update(part);
        return sig.sign();
    }

    public byte[] sign(ByteBuffer data) throws InvalidKeyException, SignatureException {
        var sig = initSignature();
        sig.update(data);
        return sig.sign();
    }

    public byte[] sign(String algorithm, byte[]... data)
            throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
        var sig = initSignature(algorithm);
        for (byte[] part : data)
            sig.update(part);
        return sig.sign();
    }

    public byte[] sign(String algorithm, ByteBuffer data)
            throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
        var sig = initSignature(algorithm);
        sig.update(data);
        return sig.sign();
    }

    public Signature initSignature() throws InvalidKeyException {
        try {
            return initSignature(signatureAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    public Signature initSignature(String algorithm)
            throws InvalidKeyException, NoSuchAlgorithmException {
        Signature sig;
        try {
            sig = Signature.getInstance(algorithm, babelSecurity.PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            sig = Signature.getInstance(algorithm);
        }
        sig.initSign(privKey);
        return sig;
    }

    /* ------------------------ DECRYPTING --------------------- */
    /* TODO All of this is (mostly) wrong. you shouldn't really use CBC or CTR modes etc. with asym keys (bc of speed).
     * See https://www.cs.cornell.edu/courses/cs5430/2017sp/l/07-asymm-enc/notes.html
     * TODO Make a default message type that includes a secret ephemeral key wrapped with RSA/NONE/OEAP as well as the encrypted data, which is generated automatically by this class
     * See Java Cryptography Tools and Techniques Chapter 7: Key Transport
     */

    /*
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
     * /
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

    public CipherInputStream decryptStream(ByteArrayInputStream in, AlgorithmParameterSpec params)
            throws NoSuchAlgorithmException, InvalidKeyException {
        return new CipherInputStream(in);
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

    public Cipher getDecryptCipher(AlgorithmParameterSpec params) throws InvalidKeyException {
        try {
            var cipher = Cipher.getInstance(defaultCipherTransformation);
            cipher.init(Cipher.DECRYPT_MODE, privKey, params);
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }
    */

}
