package pt.unl.fct.di.novasys.babel.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.CallbackHandlerProtection;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import pt.unl.fct.di.novasys.babel.core.security.IdentityGenerator;
import pt.unl.fct.di.novasys.babel.core.security.IdentityCrypt;
import pt.unl.fct.di.novasys.babel.core.security.SecretCrypt;
import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
import pt.unl.fct.di.novasys.babel.core.security.IdentityPair;
import pt.unl.fct.di.novasys.babel.internal.security.BabelCredentialHandler;
import pt.unl.fct.di.novasys.babel.internal.security.IdAliasMapper;
import pt.unl.fct.di.novasys.babel.internal.security.PeerIdEncoder;
import pt.unl.fct.di.novasys.babel.internal.security.X509BabelKeyManager;
import pt.unl.fct.di.novasys.babel.internal.security.X509BabelTrustManager;
import pt.unl.fct.di.novasys.babel.internal.security.X509CertificateChainPredicate;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

// TODO documentation (also document properties)
public class BabelSecurity {

    private static final Logger logger = LogManager.getLogger(BabelSecurity.class);

    private final ProtectionParameter EMPTY_PWD = new KeyStore.PasswordProtection(new char[0]);

    private static BabelSecurity instance;

    public static synchronized BabelSecurity getInstance() {
        if (instance == null)
            instance = new BabelSecurity();
        return instance;
    }

    public static final String PREFIX = "babel.security";
    public static final String PRNG_ALG = "DEFAULT";//"SHA1PRNG"; 
    public static final String NONCE_ALG = "NonceAndIV";

    private static final String PAR_KEY_STORE_TYPE = PREFIX + ".keystore.type";
    private String keyStoreType = "PKCS12";
    private static final String PAR_KEY_STORE_PATH = PREFIX + ".keystore.path";
    private String keyStoreLoadPath = "babelKeyStore.jks";
    /**
     * Can be a String (a path) or a boolean. If boolean and true, keyStoreWritePath
     * must be set to keyStoreLoadPath
     */
    private static final String PAR_KEY_STORE_WRITABLE = PREFIX + ".keystore.writable";
    private String keyStoreWritePath = null;
    private static final String PAR_KEY_STORE_PWD = PREFIX + ".keystore.password";
    private static final String PAR_KEY_STORE_PROTECTION = PREFIX + ".keystore.protection_handler";
    private ProtectionParameter keyStoreProtection = EMPTY_PWD;
    private static final String PAR_DEFAULT_ID = PREFIX + ".keystore.default_identity";
    // TODO support LoadStorePrameter (or at least DomainLoadStorePrameter)?
    private static final String PAR_ASYM_KEY_ALG = PREFIX + ".asym_key_algorithm";
    private String asymKeyAlgorithm = "RSA";
    private static final String PAR_ASYM_KEY_LEN = PREFIX + ".asym_key_length";
    /**
     * Will be ignored if {@value #PAR_ASYM_KEY_PARAMS} is set.
     */
    private int asymKeyLength = 2048;
    /**
     * The classpath of a supplier for the asymmetric key pair parameters.
     * <p>
     * If set, {@value #PAR_ASYM_KEY_LEN} will be ignored.
     */
    private static final String PAR_ASYM_KEY_PARAMS = PREFIX + ".asym_key_parameter_supplier";
    private AlgorithmParameterSpec asymKeyParameters = new RSAKeyGenParameterSpec(asymKeyLength,
            RSAKeyGenParameterSpec.F4);

    private static final String PAR_ID_EXTRACTOR = PREFIX + ".identity_extractor";
    private IdFromCertExtractor identityExtractor = new BabelCredentialHandler();
    private static final String PAR_ID_GENERATOR = PREFIX + ".identity_generator";
    private IdentityGenerator identityGenerator = (BabelCredentialHandler) identityExtractor;

    /** Defaults to the same as {@value #PAR_KEY_STORE_TYPE} */
    private static final String PAR_TRUST_STORE_TYPE = PREFIX + ".truststore.type";
    private String trustStoreType;
    private static final String PAR_TRUST_STORE_PWD = PREFIX + ".truststore.password";
    private static final String PAR_TRUST_STORE_PROTECTION = PREFIX + ".truststore.protection_handler";
    private ProtectionParameter trustStoreProtection = EMPTY_PWD;
    private static final String PAR_TRUST_STORE_PATH = PREFIX + ".truststore.path";
    private String trustStoreLoadPath = "babelTrustStore.jks";
    /**
     * Can be a String (a path) or a boolean. If boolean and true,
     * trustStoreWritePath must be set to trustStoreLoadPath
     */
    private static final String PAR_TRUST_STORE_WRITABLE = PREFIX + ".truststore.writable";
    private String trustStoreWritePath = null;

    private static final String PAR_TRUST_MANAGER_POLICY = PREFIX + ".trustmanager.policy";
    private X509BabelTrustManager.TrustPolicy trustManagerPolicy = X509BabelTrustManager.TrustPolicy.UNKNOWN;
    private static final String PAR_TRUST_MANAGER_SAVE_ENCOUNTERED = PREFIX + ".trustmanager.save_encountered";
    private boolean trustManagerSaveEncountered = true;
    /**
     * Ignored if {@value #PAR_TRUST_MANAGER_SAVE_ENCOUNTERED} is set to
     * {@code false}
     */
    private static final String PAR_TRUST_MANAGER_PERSIST_DISCOVERED_CERTS = PREFIX
            + ".trustmanager.persist_discovered_certificates";
    private boolean trustManagerPersistCerts = false;
    private static final String PAR_TRUST_MANAGER_UNKNOWN_PEER_CALLBACK = PREFIX
            + ".trustmanager.unknown_peer_callback";
    private X509CertificateChainPredicate trustManagerUknownPeerCallback = (certChain, id) -> false;
    private static final String PAR_TRUST_MANAGER_VERIFY_SIGNATURE_CALLBACK = PREFIX
            + ".trustmanager.verify_cert_signature_callback";
    private X509CertificateChainPredicate trustManagerVerifySignatureCallback = (certChain, id) -> {
        try {
            certChain[0].verify(certChain[0].getPublicKey());
            return true;
        } catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException
                | NoSuchProviderException | SignatureException e) {
            throw new CertificateException("Failed self-signed certificate verification.", e);
        }
    };

    /** Defaults to the same as {@value #PAR_KEY_STORE_TYPE} */
    private static final String PAR_SECRET_STORE_TYPE = PREFIX + ".secretstore.type";
    private String secretStoreType;
    private static final String PAR_SECRET_STORE_PWD = PREFIX + ".secretstore.password";
    private static final String PAR_SECRET_STORE_PROTECTION = PREFIX + ".secretstore.protection_handler";
    private ProtectionParameter secretStoreProtection = EMPTY_PWD;
    private static final String PAR_SECRET_STORE_PATH = PREFIX + ".secretstore.path";
    private String secretStoreLoadPath = "babelSecretStore.jks";
    /**
     * Can be a String (a path) or a boolean. If boolean and true,
     * secretStoreWritePath must be set to secretStoreLoadPath
     */
    private static final String PAR_SECRET_STORE_WRITABLE = PREFIX + ".secretstore.writable";
    private String secretStoreWritePath = null;
    private static final String PAR_SYM_KEY_ALG = PREFIX + ".sym_key_algorithm";
    private String symKeyAlgorithm = "AES";
    private static final String PAR_SYM_KEY_LEN = PREFIX + ".secretkey_length";
    private int symKeyLength = 128;
    // TODO PAR_SYM_KEY_PARAMS ?

    /** Algorithm for password based key derivation function */
    private static final String PAR_PBKDF_ALG = PREFIX + ".pbkdf.algorithm";
    private String pbkdfAlgorithm = "PBKDF2WithHmacSHA256";
    /** Base64 encoded salt for the PBKDF */
    private static final String PAR_PBKDF_SALT = PREFIX + ".pbkdf.salt";
    private byte[] pbkdfSalt = "Babel sa(u)lt defa(u)lt! You (or I?) should change this!!!".getBytes();
    private static final String PAR_PBKDF_ITERATIONS = PREFIX + ".pbkdf.iterations";
    private int pbkdfIterations = 131072;
    private static final String PAR_PBKDF_KEY_LEN = PREFIX + ".pbkdf.key_length";
    private int pbkdfKeyLength = 256;
    /**
     * If set, a secret key from this password will be generated at startup and
     * added to the ephemeral key manager.
     */
    private static final String PAR_PBKDF_PWD = PREFIX + ".pbkdf.initial_secret_password";

    private static final String STARTUP_PWD_DERIVED_KEY_ALIAS = "babel.password_derived";

    // See
    // https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html
    public static final String PAR_HASH_ALG = PREFIX + ".hash_algorithm";
    private String hashAlgorithm = "SHA256";

    public static final String PAR_MAC_ALG = PREFIX + ".mac_algorithm";
    private String macAlgorithm = null;
    // The defaults loosely follow TLS 1.3 as described in
    // https://www.rfc-editor.org/rfc/rfc5288
    public static final String PAR_CIPHER_TRANSFORM = PREFIX + ".cipher.transformation";
    private String cipherTransform = null;
    /** Ignored if {@value #PAR_CIPHER_TRANSFORM} is set */
    public static final String PAR_CIPHER_MODE = PREFIX + ".cipher.mode";
    private String cipherMode = "GCM";
    /** Ignored if {@value #PAR_CIPHER_TRANSFORM} is set */
    public static final String PAR_CIPHER_PADDING = PREFIX + ".cipher.padding";
    private String cipherPadding = "NoPadding";
    /** Ignored if {@value #PAR_CIPHER_PARAM_SUPPLIER} is set */
    public static final String PAR_CIPHER_IV_SIZE = PREFIX + ".cipher.iv_size";
    public static final String PAR_CIPHER_PARAM_SUPPLIER = PREFIX + ".cipher.parameter_supplier";
    private Supplier<AlgorithmParameterSpec> cipherParameterSupplier = () -> new GCMParameterSpec(128, generateIv(12));
    // public static final String PAR_CIPHER_KEYWRAP_ALG = "kwyrap_cipher";
    /*
     * public static final String PAR_... = PREFIX + ... ?
     */

    // Lazy loaded fields
    // TODO are keystores thread safe like ConcurrentHashMap?
    private KeyStore keyStore;
    private KeyStore ephKeyStore;
    private X509IKeyManager keyManager;

    private KeyStore trustStore;
    private KeyStore ephTrustStore;
    private X509ITrustManager trustManager;

    private KeyStore secretStore;
    private KeyStore ephSecretStore;

    // Fields to be instantiated at construction
    public final Provider PROVIDER = new BouncyCastleProvider();

    // This is also used as a lock (with synchronized blocks) for both keyStores
    private final IdAliasMapper idAliasMapper;

    private final SecureRandom keyRng;
    private final SecureRandom nonceRng;

    private BabelSecurity() {
        Security.addProvider(PROVIDER);

        
        
        try {
            this.keyRng = SecureRandom.getInstance(PRNG_ALG, PROVIDER);
            this.nonceRng = SecureRandom.getInstance(NONCE_ALG, PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }

        idAliasMapper = new IdAliasMapper();
    }

    /* -------------- Lazy key store loaders -------------- */

    public synchronized KeyStore getKeyStore() {
        if (keyStore == null) {
            try {
                keyStore = loadOrCreateStore(keyStoreLoadPath, keyStoreType, keyStoreProtection);

                if (keyStore.size() == 0) {
                    logger.debug("Empty key store loaded. Generating an identity...");
                    generateIdentityWithAliasPrefix(true, "babel");
                } else {
                    logger.debug("Non-empty key store loaded. Analyzing its private key entries...");
                    idAliasMapper.populateFromPrivateKeyStore(keyStore, keyStoreProtection, identityExtractor);
                }
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }

        return keyStore;
    }

    public synchronized KeyStore getEphemeralKeyStore() {
        if (ephKeyStore == null) {
            try {
                logger.debug("Creating new ephemeral key store with an auto-generated identity.");
                ephKeyStore = KeyStore.Builder.newInstance(keyStoreType, null, EMPTY_PWD).getKeyStore();
                generateIdentityWithAliasPrefix(false, "babel");
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }

        return ephKeyStore;
    }

    public synchronized X509IKeyManager getKeyManager() {
        if (keyManager == null) {
            try {
                keyManager = new X509BabelKeyManager(keyStoreProtection, idAliasMapper,
                        getKeyStore(), getEphemeralKeyStore());
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }

        return keyManager;
    }

    public synchronized KeyStore getTrustStore() {
        if (trustStore == null) {
            try {
                trustStore = loadOrCreateStore(trustStoreLoadPath, trustStoreType, trustStoreProtection);
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }

        return trustStore;
    }

    public synchronized KeyStore getEphemeralTrustStore() {
        if (ephTrustStore == null) {
            try {
                logger.debug("Creating new ephemeral trust store");
                ephTrustStore = KeyStore.Builder.newInstance(trustStoreType, null, EMPTY_PWD).getKeyStore();
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }

        return ephTrustStore;
    }

    public synchronized X509ITrustManager getTrustManager() {
        if (trustManager == null) {
            try {
                var targetStore = trustManagerSaveEncountered
                        ? trustManagerPersistCerts ? getTrustStore() : getEphemeralTrustStore()
                        : null;
                trustManager = new X509BabelTrustManager(identityExtractor,
                        List.of(getTrustStore(), getEphemeralTrustStore()),
                        trustManagerPolicy, trustManagerUknownPeerCallback,
                        trustManagerVerifySignatureCallback, targetStore);
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }

        return trustManager;
    }

    public synchronized KeyStore getSecretStore() {
        if (secretStore == null) {
            try {
                secretStore = loadOrCreateStore(secretStoreLoadPath, secretStoreType, secretStoreProtection);
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }

        return secretStore;
    }

    public synchronized KeyStore getEphemeralSecretStore() {
        if (ephSecretStore == null) {
            try {
                logger.debug("Creating new ephemeral trust store");
                ephSecretStore = KeyStore.Builder.newInstance(secretStoreType, null, EMPTY_PWD).getKeyStore();
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }

        return ephSecretStore;
    }

    // ------ Auxiliary

    private static KeyStore loadOrCreateStore(String loadPath, String storeType, ProtectionParameter protection)
            throws KeyStoreException {
        logger.debug("Loading (or creating) a key store from " + loadPath);
        File file = loadPath != null ? new File(loadPath) : null;
        return file != null && file.exists()
                ? KeyStore.Builder.newInstance(file, protection).getKeyStore()
                : KeyStore.Builder.newInstance(storeType, null, protection).getKeyStore();
    }

    /* -------------- General utilities -------------- */

    public byte[] generateIv(int size) {
        var iv = new byte[size];
        nonceRng.nextBytes(iv);
        return iv;
    }

    public IvParameterSpec generateIvParam(int size) {
        return new IvParameterSpec(generateIv(size));
    }

    public SecureRandom getSecureRandom() {
        return keyRng;
    }

    public KeyPair generateKeyPair() {
        KeyPairGenerator keyPairGen;
        try {
            keyPairGen = KeyPairGenerator.getInstance(asymKeyAlgorithm, PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            try {
                keyPairGen = KeyPairGenerator.getInstance(asymKeyAlgorithm);
            } catch (NoSuchAlgorithmException e1) {
                throw new AssertionError(e1); // Shouldn't happen
            }
        }

        try {
            if (asymKeyParameters != null)
                keyPairGen.initialize(asymKeyParameters, keyRng);
            else
                keyPairGen.initialize(asymKeyLength, keyRng);
        } catch (InvalidAlgorithmParameterException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
        return keyPairGen.generateKeyPair();
    }

    // ------ Private

    private char[] getPassword(ProtectionParameter protParam, String storeName)
            throws IOException, UnsupportedCallbackException {
        if (protParam instanceof PasswordProtection pwdProt) {
            return pwdProt.getPassword();
        } else if (protParam instanceof CallbackHandlerProtection callbackProt) {
            var callback = new PasswordCallback("Password for " + storeName, false);
            callbackProt.getCallbackHandler().handle(new Callback[] { callback });
            return callback.getPassword();
        } else {
            return null;
        }
    }

    /* ------- General cryptographic operations ------- */

    public boolean verifySignature(byte[] signature, PublicKey publicKey, byte[]... data)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = initVerifySignature(publicKey, data);
        return sig.verify(signature);
    }

    public boolean verifySignature(byte[] signature, PublicKey publicKey, ByteBuffer data)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = initVerifySignature(publicKey, data);
        return sig.verify(signature);
    }

    /**
     * signerId can't refer to myself
     * TODO or should it? And should I add my own certificates to my trust store?
     */
    public boolean verifySignature(byte[] signature, byte[] signerId, byte[]... data)
            throws NoSuchAlgorithmException, SignatureException, NoSuchElementException, InvalidKeyException {
        Certificate cert = getTrustedCertificate(signerId);
        if (cert == null)
            throw new NoSuchElementException("No known certificate for %s that can be used to verify the signature"
                    .formatted(PeerIdEncoder.encodeToString(signerId)));

        Signature sig = (cert instanceof X509Certificate x509Cert)
                ? initVerifySignature(x509Cert.getSigAlgName(), x509Cert.getPublicKey(), data)
                : initVerifySignature(cert.getPublicKey(), data);

        return sig.verify(signature);
    }

    /**
     * signerId can't refer to myself
     */
    public boolean verifySignature(byte[] signature, byte[] signerId, ByteBuffer data)
            throws NoSuchAlgorithmException, SignatureException, NoSuchElementException, InvalidKeyException {
        Certificate cert = getTrustedCertificate(signerId);
        if (cert == null)
            throw new NoSuchElementException("No known certificate for %s that can be used to verify the signature"
                    .formatted(PeerIdEncoder.encodeToString(signerId)));

        Signature sig = (cert instanceof X509Certificate x509Cert)
                ? initVerifySignature(x509Cert.getSigAlgName(), x509Cert.getPublicKey(), data)
                : initVerifySignature(cert.getPublicKey(), data);

        return sig.verify(signature);
    }

    public boolean verifySignature(String algorithm, byte[] signature, PublicKey publicKey, byte[]... data)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = initVerifySignature(algorithm, publicKey, data);
        return sig.verify(signature);
    }

    public boolean verifySignature(String algorithm, byte[] signature, PublicKey publicKey, ByteBuffer data)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = initVerifySignature(algorithm, publicKey, data);
        return sig.verify(signature);
    }

    /**
     * signerId can't refer to myself
     */
    public boolean verifySignature(String algorithm, byte[] signature, byte[] signerId, byte[]... data)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchElementException {
        Certificate cert = getTrustedCertificate(signerId);
        if (cert == null)
            throw new NoSuchElementException("No known certificate for %s that can be used to verify the signature"
                    .formatted(PeerIdEncoder.encodeToString(signerId)));

        Signature sig = initVerifySignature(algorithm, cert.getPublicKey(), data);
        return sig.verify(signature);
    }

    /**
     * signerId can't refer to myself
     */
    public boolean verifySignature(String algorithm, byte[] signature, byte[] signerId, ByteBuffer data)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchElementException {
        Certificate cert = getTrustedCertificate(signerId);
        if (cert == null)
            throw new NoSuchElementException("No known certificate for %s that can be used to verify the signature"
                    .formatted(PeerIdEncoder.encodeToString(signerId)));

        Signature sig = initVerifySignature(algorithm, cert.getPublicKey(), data);
        return sig.verify(signature);
    }

    /**
     * signerId can't refer to myself
     */
    public Signature initVerifySignature(byte[] signerId, byte[]... data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Certificate cert = getTrustedCertificate(signerId);
        if (cert == null)
            throw new NoSuchElementException("No known certificate for %s that can be used to verify the signature"
                    .formatted(PeerIdEncoder.encodeToString(signerId)));

        return (cert instanceof X509Certificate x509Cert)
                ? initVerifySignature(x509Cert.getSigAlgName(), x509Cert.getPublicKey(), data)
                : initVerifySignature(cert.getPublicKey(), data);
    }

    /**
     * signerId can't refer to myself
     */
    public Signature initVerifySignature(byte[] signerId, ByteBuffer data)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchElementException {
        Certificate cert = getTrustedCertificate(signerId);
        if (cert == null)
            throw new NoSuchElementException("No known certificate for %s that can be used to verify the signature"
                    .formatted(PeerIdEncoder.encodeToString(signerId)));

        return (cert instanceof X509Certificate x509Cert)
                ? initVerifySignature(x509Cert.getSigAlgName(), x509Cert.getPublicKey(), data)
                : initVerifySignature(cert.getPublicKey(), data);
    }

    public Signature initVerifySignature(PublicKey publicKey, byte[]... data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        return initVerifySignature(getSignatureAlgorithmFor(publicKey.getAlgorithm()), publicKey, data);
    }

    public Signature initVerifySignature(PublicKey publicKey, ByteBuffer data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        return initVerifySignature(getSignatureAlgorithmFor(publicKey.getAlgorithm()), publicKey, data);
    }

    /**
     * signerId can't refer to myself
     */
    public Signature initVerifySignature(String algorithm, byte[] signerId, byte[]... data)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchElementException {
        Certificate cert = getTrustedCertificate(signerId);
        if (cert == null)
            throw new NoSuchElementException("No known certificate for %s that can be used to verify the signature"
                    .formatted(PeerIdEncoder.encodeToString(signerId)));

        return initVerifySignature(algorithm, cert.getPublicKey(), data);
    }

    /**
     * signerId can't refer to myself
     */
    public Signature initVerifySignature(String algorithm, byte[] signerId, ByteBuffer data)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchElementException {
        Certificate cert = getTrustedCertificate(signerId);
        if (cert == null)
            throw new NoSuchElementException("No known certificate for %s that can be used to verify the signature"
                    .formatted(PeerIdEncoder.encodeToString(signerId)));

        return initVerifySignature(algorithm, cert.getPublicKey(), data);
    }

    public Signature initVerifySignature(String algorithm, PublicKey publicKey, byte[]... data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Signature sig;
        try {
            sig = Signature.getInstance(algorithm, PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            sig = Signature.getInstance(algorithm);
        }
        sig.initVerify(publicKey);
        try {
            for (byte[] part : data)
                sig.update(part);
        } catch (SignatureException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
        return sig;
    }

    public Signature initVerifySignature(String algorithm, PublicKey publicKey, ByteBuffer data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Signature sig;
        try {
            sig = Signature.getInstance(algorithm, PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            sig = Signature.getInstance(algorithm);
        }
        sig.initVerify(publicKey);
        try {
            sig.update(data);
        } catch (SignatureException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
        return sig;
    }

    /* ----------------- Identities ----------------- */

    // ------ Public methods

    public Pair<PrivateKeyEntry, String> deleteIdentity(byte[] identity) throws UnrecoverableEntryException {
        synchronized (idAliasMapper) {
            String alias = idAliasMapper.getAlias(identity);

            var result = deleteIdentity(alias);
            assert Arrays.equals(result.getRight(), identity);

            return Pair.of(result.getLeft(), alias);
        }
    }

    public Pair<PrivateKeyEntry, byte[]> deleteIdentity(String alias) throws UnrecoverableEntryException {
        synchronized (idAliasMapper) {
            try {
                var chosenPair = chooseKeyStore(store -> store.containsAlias(alias));
                KeyStore keyStore = chosenPair.getLeft();
                ProtectionParameter protection = chosenPair.getRight();
                try {
                    KeyStore.Entry entry = keyStore.getEntry(alias, protection);
                    if (entry instanceof PrivateKeyEntry deleted) {
                        keyStore.deleteEntry(alias);

                        byte[] removedId = idAliasMapper.removeAlias(alias);
                        return Pair.of(deleted, removedId);
                    } else {
                        return null;
                    }
                } catch (NoSuchAlgorithmException e) {
                    // Delete anyways
                    keyStore.deleteEntry(alias);
                    byte[] removedId = idAliasMapper.removeAlias(alias);
                    return Pair.of(null, removedId);
                }
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }
    }

    public IdentityCrypt generateIdentity(boolean persistOnDisk) {
        try {
            return addIdentity(persistOnDisk, identityGenerator.generateRandomCredentials());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("The defined IdentityGenerator failed to generate a new identity", e);
        } catch (CertificateException e) {
            throw new RuntimeException(
                    "Failed generating identity. Proabably the defined IdentityGenerator is incompatible with the defined IdFromCertExtractor", e);
        }
    }

    public IdentityCrypt generateIdentityWithAliasPrefix(boolean persistOnDisk, String aliasPrefix) {
        try {
            return addIdentityWithAliasPrefix(persistOnDisk, aliasPrefix,
                    identityGenerator.generateRandomCredentials());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    public IdentityCrypt generateIdentity(boolean persistOnDisk, String alias) {
        try {
            return addIdentity(persistOnDisk, alias, identityGenerator.generateRandomCredentials());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("The defined IdentityGenerator failed to generate a new identity", e);
        } catch (CertificateException e) {
            throw new RuntimeException(
                    "Failed generating identity. Proabably the defined IdentityGenerator is incompatible with the defined IdFromCertExtractor", e);
        }
    }

    public IdentityCrypt generateIdentity(boolean persistOnDisk, KeyPair keyPair) throws NoSuchAlgorithmException {
        try {
            return addIdentity(persistOnDisk, identityGenerator.generateCredentials(keyPair));
        } catch (CertificateException e) {
            throw new RuntimeException(
                    "Failed generating identity. Proabably the defined IdentityGenerator is incompatible with the defined IdFromCertExtractor", e);
        }
    }

    public IdentityCrypt generateIdentityWithAliasPrefix(boolean persistOnDisk, String aliasPrefix, KeyPair keyPair)
            throws NoSuchAlgorithmException {
        return addIdentityWithAliasPrefix(persistOnDisk, aliasPrefix, identityGenerator.generateCredentials(keyPair));
    }

    public IdentityCrypt generateIdentity(boolean persistOnDisk, String alias, KeyPair keyPair)
            throws NoSuchAlgorithmException {
        try {
            return addIdentity(persistOnDisk, alias, identityGenerator.generateCredentials(keyPair));
        } catch (CertificateException e) {
            throw new RuntimeException(
                    "Failed generating identity. Proabably the defined IdentityGenerator is incompatible with the defined IdFromCertExtractor", e);
        }
    }

    public IdentityCrypt addIdentity(boolean persistOnDisk, PrivateKeyEntry keyStoreEntry)
            throws NoSuchAlgorithmException, CertificateException {
        return addIdentity(persistOnDisk, null, keyStoreEntry);
    }

    public IdentityCrypt addIdentityWithAliasPrefix(boolean persistOnDisk, String aliasPrefix,
            PrivateKeyEntry keyStoreEntry) throws NoSuchAlgorithmException {
        byte[] id;
        try {
            id = identityExtractor.extractIdentity(keyStoreEntry.getCertificate());
        } catch (CertificateException e) {
            throw new RuntimeException(e); // Shouldn't happen
        }
        String alias = aliasPrefix + "." + PeerIdEncoder.encodeToString(id);
        addIdentity(persistOnDisk, alias, id, keyStoreEntry);
        return getIdentityCrypt(alias, id, keyStoreEntry);
    }

    public IdentityCrypt addIdentity(boolean persistOnDisk, String alias, PrivateKeyEntry keyStoreEntry)
            throws NoSuchAlgorithmException, CertificateException {
        byte[] id = identityExtractor.extractIdentity(keyStoreEntry.getCertificate());
        alias = alias == null ? PeerIdEncoder.encodeToString(id) : alias;
        addIdentity(persistOnDisk, alias, id, keyStoreEntry);
        return getIdentityCrypt(alias, id, keyStoreEntry);
    }

    public IdentityCrypt getDefaultIdentityCrypt()
            throws NoSuchAlgorithmException, UnrecoverableEntryException {
        IdentityPair idPair = idAliasMapper.getDefault();
        return idPair != null
                ? getIdentityCrypt(idPair.alias(), idPair.identity())
                : generateIdentity(false);
    }

    public IdentityCrypt getIdentityCrypt(String alias)
            throws NoSuchAlgorithmException, UnrecoverableEntryException {
        byte[] id = idAliasMapper.getId(alias);
        return id == null ? null : getIdentityCrypt(alias, id);
    }

    public IdentityCrypt getIdentityCrypt(byte[] identity)
            throws NoSuchAlgorithmException, UnrecoverableEntryException {
        String alias = idAliasMapper.getAlias(identity);
        return alias == null ? null : getIdentityCrypt(alias, identity);
    }

    public Set<IdentityPair> getAllIdentities() {
        try {
            Set<IdentityPair> ids = new HashSet<>(getKeyStore().size() + getEphemeralKeyStore().size());
            getKeyStore().aliases().asIterator()
                    .forEachRemaining(alias -> ids.add(new IdentityPair(alias, idAliasMapper.getId(alias))));
            getEphemeralKeyStore().aliases().asIterator()
                    .forEachRemaining(alias -> ids.add(new IdentityPair(alias, idAliasMapper.getId(alias))));
            return ids;
        } catch (KeyStoreException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    public Set<IdentityPair> getAllIdentitiesWithPrefix(String aliasPrefix) {
        try {
            Set<IdentityPair> ids = new HashSet<>(getKeyStore().size() + getEphemeralKeyStore().size());

            for (var enumeration : List.of(getKeyStore().aliases(), getEphemeralKeyStore().aliases())) {
                while (enumeration.hasMoreElements()) {
                    String alias = enumeration.nextElement();
                    if (alias.startsWith(aliasPrefix + "."))
                        ids.add(new IdentityPair(alias, idAliasMapper.getId(alias)));
                }
            }
            return ids;
        } catch (KeyStoreException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    public String getIdentityAlias(byte[] identity) {
        return idAliasMapper.getAlias(identity);
    }

    public byte[] getAliasIdentity(String alias) {
        return idAliasMapper.getId(alias);
    }

    public IdentityPair getDefaultIdentity() {
        getKeyStore(); // ensure keystore was initialized
        return idAliasMapper.getDefault();
    }

    // ----- Auxiliary methods

    private void addIdentity(boolean peristOnDisk, String alias, byte[] id, PrivateKeyEntry entry) {
        synchronized (idAliasMapper) {
            try {
                var chosenPair = chooseKeyStore(__ -> peristOnDisk);
                KeyStore store = chosenPair.getLeft();
                ProtectionParameter protection = chosenPair.getRight();
                idAliasMapper.put(alias, id);
                store.setEntry(alias, entry, protection);

                if (peristOnDisk && keyStoreWritePath != null)
                    store.store(new FileOutputStream(keyStoreWritePath),
                            getPassword(keyStoreProtection, keyStoreWritePath));

            } catch (NoSuchAlgorithmException | CertificateException | IOException | UnsupportedCallbackException e) {
                logger.error("Couldn't persist key store to {} after adding identity {}. Cause: {}",
                        keyStoreWritePath, alias, e);
            } catch (KeyStoreException e) {
                // From KeyStore.setEntry():
                // "KeyStoreException if the keystore has not been initialized (loaded), or
                // if this operation fails for **some other reason**"...
                throw new RuntimeException(e);
            }
        }
    }

    private IdentityCrypt getIdentityCrypt(String alias, byte[] id)
            throws NoSuchAlgorithmException, UnrecoverableEntryException {
        return getIdentityCrypt(alias, id, (String) null);
    }

    private IdentityCrypt getIdentityCrypt(String alias, byte[] id, String sigHashOrAlg)
            throws NoSuchAlgorithmException, UnrecoverableEntryException {
        try {
            var chosenPair = chooseKeyStore(store -> store.containsAlias(alias));
            KeyStore.Entry entry = chosenPair.getLeft().getEntry(alias, chosenPair.getRight());
            if (entry instanceof PrivateKeyEntry pkEntry) {
                return sigHashOrAlg == null
                        ? getIdentityCrypt(alias, id, pkEntry)
                        : getIdentityCrypt(alias, id, pkEntry, sigHashOrAlg);
            } else {
                return null;
            }
        } catch (KeyStoreException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    private IdentityCrypt getIdentityCrypt(String alias, byte[] id, PrivateKeyEntry entry)
            throws NoSuchAlgorithmException {
        return getIdentityCrypt(alias, id, entry, getSignatureAlgorithmFor(entry.getPrivateKey().getAlgorithm()));
    }

    private IdentityCrypt getIdentityCrypt(String alias, byte[] id, PrivateKeyEntry entry, String sigHashOrAlg)
            throws NoSuchAlgorithmException {
        var privKey = entry.getPrivateKey();
        var pubKey = entry.getCertificate().getPublicKey();
        var certChain = entry.getCertificateChain();
        return new IdentityCrypt(alias, id, privKey, pubKey, certChain, sigHashOrAlg);
    }

    public String getSignatureAlgorithmFor(String keyAlgorithm) throws NoSuchAlgorithmException {
        if (keyAlgorithm.equals("EdDSA"))
            return "EdDSA";
        String sigAlg = hashAlgorithm + "WITH" + keyAlgorithm;

        if (Security.getAlgorithms("Signature").contains(sigAlg.toUpperCase()))
            return sigAlg;
        else
            throw new NoSuchAlgorithmException("No such Signature algorithm: " + sigAlg);
    }

    private Pair<KeyStore, ProtectionParameter> chooseKeyStore(KeyStorePredicate shouldBePersistent)
            throws KeyStoreException {
        KeyStore persistent = getKeyStore();
        return shouldBePersistent.test(persistent)
                ? Pair.of(persistent, keyStoreProtection)
                : Pair.of(getEphemeralKeyStore(), EMPTY_PWD);
    }

    @FunctionalInterface
    private interface KeyStorePredicate {
        boolean test(KeyStore keyStore) throws KeyStoreException;
    }

    /* ------------------- Trust ------------------- */

    public void addTrustedCertificate(boolean peristOnDisk, Certificate certificate)
            throws KeyStoreException, CertificateException {
        KeyStore trustStore = peristOnDisk ? getTrustStore() : getEphemeralTrustStore();

        String alias = PeerIdEncoder.encodeToString(identityExtractor.extractIdentity(certificate));

        synchronized (trustStore) {
            trustStore.setCertificateEntry(alias, certificate);

            if (peristOnDisk && trustStoreWritePath != null) {
                try {
                    trustStore.store(new FileOutputStream(trustStoreWritePath),
                            getPassword(trustStoreProtection, trustStoreWritePath));
                } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
                        | UnsupportedCallbackException e) {
                    logger.error("Couldn't persist trust store to {} after adding trusted certificate {}. Cause: {}",
                            trustStoreWritePath, alias, e);
                }
            }
        }
    }

    public void addTrustedPeerIdentity(byte[] peerId) {
        getTrustManager().addTrustedId(peerId);
    }

    public void removeTrustedPeerIdentity(byte[] peerId) {
        getTrustManager().removeTrustedId(peerId);
    }

    public Certificate getTrustedCertificate(byte[] peerId) {
        Certificate persistent = getTrustedCertificateFrom(getTrustStore(), peerId);
        return persistent != null ? persistent : getTrustedCertificateFrom(getEphemeralTrustStore(), peerId);
    }

    /**
     * Sets trust manager policy to {@code newPolicy} if the trust manager is an
     * instance of {@link X509BabelTrustManager}. Else, does nothing.
     *
     * @param newPolicy The new policy to set for the trust manager.
     * @return {@code true} if this method had any effects.
     */
    public boolean setTrustManagerPolicy(X509BabelTrustManager.TrustPolicy newPolicy) {
        if (getTrustManager() instanceof X509BabelTrustManager trustMan) {
            trustMan.setTrustPolicy(newPolicy);
            return true;
        } else {
            return false;
        }
    }

    // ----- Auxiliary

    private Certificate getTrustedCertificateFrom(KeyStore trustStore, byte[] peerId) {
        try {
            String alias = PeerIdEncoder.encodeToString(peerId);
            Certificate cert = trustStore.getCertificate(alias);
            if (cert != null) {
                byte[] trustedId = identityExtractor.extractIdentity(cert);
                if (Arrays.equals(trustedId, peerId))
                    return cert;
            }
        } catch (CertificateException | KeyStoreException e) {
            // ignore
        }
        return null;
    }

    /* ------------------- Secrets ------------------- */

    // ------ Public methods

    // TODO Make a way to facilitate secret agreement? Might be terrible to make it
    // generalized

    public SecretCrypt generateSecretFromPasswordWithAliasPrefix(boolean persistOnDisk, String aliasPrefix,
            String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return addSecretWithAliasPrefix(persistOnDisk, aliasPrefix, applyPBKDF(password.toCharArray(), pbkdfSalt));
    }

    public SecretCrypt generateSecretFromPasswordWithAliasPrefix(boolean persistOnDisk, String aliasPrefix,
            String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return addSecretWithAliasPrefix(persistOnDisk, aliasPrefix, applyPBKDF(password.toCharArray(), salt));
    }

    public SecretCrypt generateSecretFromPassword(boolean persistOnDisk, String alias, String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        return addSecret(persistOnDisk, alias, applyPBKDF(password.toCharArray(), pbkdfSalt));
    }

    public SecretCrypt generateSecretFromPassword(boolean persistOnDisk, String alias, String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        return addSecret(persistOnDisk, alias, applyPBKDF(password.toCharArray(), salt));
    }

    public SecretCrypt generateSecret(boolean persistOnDisk) throws NoSuchAlgorithmException {
        return addSecret(persistOnDisk, generateSecretKey());
    }

    public SecretCrypt generateSecretWithAliasPrefix(boolean persistOnDisk, String aliasPrefix)
            throws NoSuchAlgorithmException {
        return addSecretWithAliasPrefix(persistOnDisk, aliasPrefix, generateSecretKey());
    }

    public SecretCrypt generateSecret(boolean persistOnDisk, String alias) throws NoSuchAlgorithmException {
        return addSecret(persistOnDisk, alias, generateSecretKey());
    }

    public SecretCrypt addSecretWithAliasPrefix(boolean peristOnDisk, String aliasPrefix, SecretKey secretKey)
            throws NoSuchAlgorithmException {
        return addSecret(peristOnDisk, aliasPrefix + "." + generateSecretAlias(secretKey), secretKey);
    }

    public SecretCrypt addSecret(boolean peristOnDisk, SecretKey secretKey) throws NoSuchAlgorithmException {
        return addSecret(peristOnDisk, generateSecretAlias(secretKey), secretKey);
    }

    public SecretCrypt addSecret(boolean peristOnDisk, String alias, SecretKey secretKey)
            throws NoSuchAlgorithmException {
        try {
            var chosenPair = chooseSecretStore(__ -> peristOnDisk);
            var store = chosenPair.getLeft();

            synchronized (store) {
                store.setEntry(alias, new KeyStore.SecretKeyEntry(secretKey), chosenPair.getRight());

                if (peristOnDisk && keyStoreWritePath != null)
                    store.store(new FileOutputStream(secretStoreWritePath),
                            getPassword(secretStoreProtection, secretStoreWritePath));
            }
        } catch (IOException | NoSuchAlgorithmException | CertificateException | UnsupportedCallbackException e) {
            logger.error("Couldn't persist secret store to {} after adding secret {}. Cause: {}",
                    secretStoreWritePath, alias, e);
        } catch (KeyStoreException e) {
            // From KeyStore.setEntry():
            // "KeyStoreException if the keystore has not been initialized (loaded), or
            // if this operation fails for **some other reason**"...
            throw new RuntimeException(e);
        }

        return getSecretCrypt(alias, secretKey);
    }

    public SecretCrypt getSecretCrypt(String alias) throws NoSuchAlgorithmException, UnrecoverableEntryException {
        try {
            var chosenPair = chooseSecretStore(store -> store.containsAlias(alias));
            KeyStore.Entry entry = chosenPair.getLeft().getEntry(alias, chosenPair.getRight());
            if (entry instanceof KeyStore.SecretKeyEntry secreteEntry) {
                return getSecretCrypt(alias, secreteEntry.getSecretKey());
            } else {
                return null;
            }
        } catch (KeyStoreException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    // ----- Auxiliary methods

    private SecretCrypt getSecretCrypt(String alias, SecretKey key) throws NoSuchAlgorithmException {
        var macAlgorithm = this.macAlgorithm == null ? "Hmac" + this.hashAlgorithm : this.macAlgorithm;
        return cipherTransform == null
                ? new SecretCrypt(alias, key, macAlgorithm, cipherMode, cipherPadding, cipherParameterSupplier)
                : new SecretCrypt(alias, key, macAlgorithm, cipherTransform, cipherParameterSupplier);
    }

    private SecretKey generateSecretKey() {
        try {
            var gen = KeyGenerator.getInstance(symKeyAlgorithm);
            gen.init(symKeyLength, keyRng);
            return gen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    private Pair<KeyStore, ProtectionParameter> chooseSecretStore(KeyStorePredicate shouldBePersistent)
            throws KeyStoreException {
        KeyStore persistent = getSecretStore();
        return shouldBePersistent.test(persistent)
                ? Pair.of(persistent, secretStoreProtection)
                : Pair.of(getEphemeralSecretStore(), EMPTY_PWD);
    }

    private String generateSecretAlias(SecretKey key) {
        try {
            MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);
            digest.update(key.getEncoded());
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    private SecretKey applyPBKDF(char[] password, byte[] salt) throws InvalidKeySpecException {
        SecretKeyFactory fac;
        try {
            fac = SecretKeyFactory.getInstance(pbkdfAlgorithm, PROVIDER);
        } catch (NoSuchAlgorithmException e1) {
            try {
                fac = SecretKeyFactory.getInstance(pbkdfAlgorithm);
            } catch (NoSuchAlgorithmException e2) {
                throw new AssertionError(e2); // Shouldn't happen
            }
        }

        SecretKey pbkdfKey = fac.generateSecret(new PBEKeySpec(password, salt, pbkdfIterations, pbkdfKeyLength));
        return new SecretKeySpec(pbkdfKey.getEncoded(), symKeyAlgorithm);
    }

    /* ------------------- Config ------------------- */

    /**
     * Called by Babel's load config.
     * <p>
     * <b>Can lead to unexpected behaviour if called after one of the many key
     * stores is loaded for the first time.</b>
     */
    synchronized void loadConfig(Properties config)
            throws pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException {
        if (keyStore != null || ephKeyStore != null || trustStore != null || ephTrustStore != null
                || secretStore != null || ephSecretStore != null) {
            logger.warn(
                    "Loading configuration after one of the key stores was already loaded. This might lead to unexpected behaviour.");
        }

        // key store
        keyStoreType = getAlgorithmParam("KeyStore", config, PAR_KEY_STORE_TYPE, keyStoreType);
        keyStoreLoadPath = config.getProperty(PAR_KEY_STORE_PATH, keyStoreLoadPath);

        String param = config.getProperty(PAR_KEY_STORE_WRITABLE);
        keyStoreWritePath = param == null || param.toUpperCase().equals("false")
                ? null
                : param.toUpperCase().equals("true")
                        ? keyStoreLoadPath
                        : param;

        param = config.getProperty(PAR_KEY_STORE_PWD);
        keyStoreProtection = param != null
                ? new PasswordProtection(param.toCharArray())
                : loadClassParam(config, PAR_KEY_STORE_PROTECTION, keyStoreProtection);

        param = config.getProperty(PAR_DEFAULT_ID);
        if (param != null)
            idAliasMapper.setDefaultAlias(param);

        asymKeyAlgorithm = getAlgorithmParam("KeyFactory", config, PAR_ASYM_KEY_ALG, asymKeyAlgorithm);

        var algParamParam = loadClassParam(AlgorithmParameterSpec.class, config, PAR_ASYM_KEY_PARAMS);
        if (algParamParam == null) {
            Integer keyLenParam = getObjectParam(config, PAR_ASYM_KEY_LEN, null, Integer::parseInt);
            if (keyLenParam != null) {
                try {
                    KeyPairGenerator.getInstance(asymKeyAlgorithm).initialize(keyLenParam);
                } catch (NoSuchAlgorithmException | InvalidParameterException e) {
                    throw getConfigParamException(PAR_ASYM_KEY_LEN, keyLenParam.toString(),
                            "Invalid parameter for asymmetric key algorithm " + asymKeyAlgorithm, e);
                }
                asymKeyParameters = null;
                asymKeyLength = keyLenParam;
            } else if (config.contains(PAR_ASYM_KEY_ALG)) {
                try {
                    KeyPairGenerator.getInstance(asymKeyAlgorithm).initialize(algParamParam);
                } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                    throw new pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException(
                            "Asymmetric key algorithm was set without setting proper corresponding algorithm parameters",
                            e);
                }
            }
        } else {
            try {
                KeyPairGenerator.getInstance(asymKeyAlgorithm).initialize(algParamParam);
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                throw getConfigParamException(PAR_ASYM_KEY_PARAMS, algParamParam.getClass().getName(),
                        "Invalid parameter spec for asymmetric key algorithm " + asymKeyAlgorithm, e);
            }
            asymKeyParameters = algParamParam;
        }

        // id extractor
        identityExtractor = loadClassParam(config, PAR_ID_EXTRACTOR, identityExtractor);

        identityGenerator = loadClassParam(config, PAR_ID_GENERATOR, identityGenerator);

        // trust store
        // Type defaults to same as keystore
        trustStoreType = getAlgorithmParam("KeyStore", config, PAR_TRUST_STORE_TYPE, keyStoreType);
        trustStoreLoadPath = config.getProperty(PAR_TRUST_STORE_PATH, trustStoreLoadPath);

        param = config.getProperty(PAR_TRUST_STORE_PWD);
        trustStoreProtection = param != null
                ? new PasswordProtection(param.toCharArray())
                : loadClassParam(config, PAR_TRUST_STORE_PROTECTION, trustStoreProtection);

        param = config.getProperty(PAR_TRUST_STORE_WRITABLE);
        trustStoreWritePath = param == null || param.toUpperCase().equals("false")
                ? null
                : param.toUpperCase().equals("true") ? trustStoreLoadPath : param;

        // trust man
        trustManagerPolicy = getObjectParamUpperCase(config, PAR_TRUST_MANAGER_POLICY, trustManagerPolicy,
                X509BabelTrustManager.TrustPolicy::valueOf);

        trustManagerSaveEncountered = getObjectParam(config, PAR_TRUST_MANAGER_SAVE_ENCOUNTERED,
                trustManagerSaveEncountered, Boolean::valueOf);
        if (trustManagerSaveEncountered) {
            trustManagerPersistCerts = getObjectParam(config, PAR_TRUST_MANAGER_PERSIST_DISCOVERED_CERTS,
                    trustManagerPersistCerts, Boolean::valueOf);
        }

        trustManagerUknownPeerCallback = loadClassParam(config, PAR_TRUST_MANAGER_UNKNOWN_PEER_CALLBACK,
                trustManagerUknownPeerCallback);
        trustManagerVerifySignatureCallback = loadClassParam(config, PAR_TRUST_MANAGER_VERIFY_SIGNATURE_CALLBACK,
                trustManagerVerifySignatureCallback);

        // secret store
        // Type defaults to same as keystore
        secretStoreType = getAlgorithmParam("KeyStore", config, PAR_SECRET_STORE_TYPE, keyStoreType);
        secretStoreLoadPath = config.getProperty(PAR_SECRET_STORE_PATH, secretStoreLoadPath);

        param = config.getProperty(PAR_SECRET_STORE_PWD);
        secretStoreProtection = param != null
                ? new PasswordProtection(param.toCharArray())
                : loadClassParam(config, PAR_SECRET_STORE_PROTECTION, secretStoreProtection);

        param = config.getProperty(PAR_SECRET_STORE_WRITABLE);
        keyStoreWritePath = param == null || param.toUpperCase().equals("false")
                ? null
                : param.toUpperCase().equals("true")
                        ? secretStoreLoadPath
                        : param;

        symKeyAlgorithm = getAlgorithmParam("SecretKeyFactory", config, PAR_SYM_KEY_ALG, symKeyAlgorithm);
        symKeyLength = getObjectParam(config, PAR_SYM_KEY_LEN, symKeyLength, Integer::parseInt);

        // pbkdf
        pbkdfAlgorithm = getAlgorithmParam("SecretKeyFactory", config, PAR_PBKDF_ALG, pbkdfAlgorithm);

        pbkdfSalt = getObjectParam(config, PAR_PBKDF_SALT, pbkdfSalt, Base64.getDecoder()::decode);
        pbkdfIterations = getObjectParam(config, PAR_PBKDF_ITERATIONS, pbkdfIterations, Integer::parseInt);
        pbkdfKeyLength = getObjectParam(config, PAR_PBKDF_KEY_LEN, pbkdfKeyLength, Integer::parseInt);

        if ((param = config.getProperty(PAR_PBKDF_PWD)) != null) {
            try {
                this.generateSecretFromPassword(false, STARTUP_PWD_DERIVED_KEY_ALIAS, param);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException(
                        STARTUP_PWD_DERIVED_KEY_ALIAS + ": Couldn't generate secret from password in config.",
                        e);
            }
        }

        // hash and/or mac
        if ((param = config.getProperty(PAR_HASH_ALG)) != null) {
            try {
                MessageDigest.getInstance(param); // Check availability
            } catch (NoSuchAlgorithmException e) {
                throw getConfigParamException(PAR_HASH_ALG, param, "No such algorithm available for MessageDigest.", e);
            }
            hashAlgorithm = param;
        }

        macAlgorithm = getAlgorithmParam("Mac", config, PAR_MAC_ALG);

        // cipher
        cipherTransform = getAlgorithmParam("Cipher", config, PAR_CIPHER_TRANSFORM);

        if (cipherTransform == null) {
            boolean changed = false;
            param = config.getProperty(PAR_CIPHER_MODE);
            if (param != null) {
                changed = true;
                cipherMode = param;
            }
            param = config.getProperty(PAR_CIPHER_PADDING);
            if (param != null) {
                changed = true;
                cipherPadding = param;
            }
            // Validate received parameters
            if (changed) {
                boolean modeFound = false;
                boolean paddingFound = false;
                for (String algorithm : Security.getAlgorithms("Cipher")) {
                    if (!modeFound && algorithm.contains("/" + cipherMode + "/")) {
                        if (paddingFound)
                            break;
                        modeFound = true;
                    }
                    if (!paddingFound && algorithm.endsWith("/" + cipherPadding)) {
                        if (modeFound)
                            break;
                        paddingFound = true;
                    }
                }
                if (!modeFound)
                    throw getConfigParamException(PAR_CIPHER_MODE, cipherMode, "No such cipher mode available.", null);
                if (!paddingFound)
                    throw getConfigParamException(PAR_CIPHER_PADDING, cipherPadding,
                            "No such cipher padding available.", null);
            }
        }

        if (config.contains(PAR_CIPHER_PARAM_SUPPLIER)) {
            cipherParameterSupplier = loadClassParam((Class<Supplier<AlgorithmParameterSpec>>) null, config,
                    PAR_CIPHER_PARAM_SUPPLIER);
        } else {
            Integer ivSize = getObjectParam(config, PAR_CIPHER_IV_SIZE, null, Integer::parseInt);
            if (ivSize != null)
                cipherParameterSupplier = () -> new IvParameterSpec(generateIv(ivSize));
        }

    }

    private static pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException getConfigParamException(
            String param, String value, String explanation, Throwable cause) {
        String msg = "%s: Invalid value \"%s\"".formatted(param, value)
                + (explanation == null ? "." : ": " + explanation);
        return cause == null
                ? new pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException(msg)
                : new pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException(msg, cause);
    }

    private static <T> T loadClassParam(Class<T> clazz, Properties props, String key)
            throws pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException {
        return loadClassParam(props, key, (T) null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T loadClassParam(Properties props, String key, T defaultValue)
            throws pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException {
        String className = props.getProperty(key);
        if (className == null)
            return defaultValue;
        try {
            return ((Class<? extends T>) Class.forName(className)).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw getConfigParamException(key, className, null, e);
        }
    }

    private static <T> T getObjectParam(Properties props, String key, T defaultValue, Function<String, T> parser)
            throws pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException {
        var param = props.getProperty(key);
        return param != null ? parser.apply(param) : defaultValue;
    }

    private static <T> T getObjectParamUpperCase(Properties props, String key, T defaultValue,
            Function<String, T> parser)
            throws pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException {
        var param = props.getProperty(key);
        try {
            return param != null ? parser.apply(param.toUpperCase()) : defaultValue;
        } catch (Exception e) {
            throw getConfigParamException(key, param, "Failed to parse Object", e);
        }
    }

    private static String getAlgorithmParam(String service, Properties props, String key)
            throws pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException {
        var alg = props.getProperty(key);
        if (alg != null) {
            alg = alg.toUpperCase();
            if (!Security.getAlgorithms(service).contains(alg))
                throw getConfigParamException(key, alg, "No such algorithm available for " + service, null);
        }
        return alg;
    }

    private static String getAlgorithmParam(String service, Properties props, String key, String defaultValue)
            throws pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException {
        var alg = props.getProperty(key, defaultValue).toUpperCase();
        if (!Security.getAlgorithms(service).contains(alg))
            throw getConfigParamException(key, alg, "No such algorithm available for " + service, null);
        return alg;
    }

}
