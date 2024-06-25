package pt.unl.fct.di.novasys.babel.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.CallbackHandlerProtection;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
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

    private static final String PAR_KEY_STORE_TYPE = PREFIX + ".keystore.type";
    private String keyStoreType = "PKCS12";
    private static final String PAR_KEY_STORE_PATH = PREFIX + ".keystore.path";
    private static final String DEF_KEY_STORE_PATH = "babelKeyStore.jks";
    private String keyStoreLoadPath = null;
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
    // TODO support KeyStore.LoadStorePrameter? (or at least DomainLoadStorePrameter?)

    private static final String PAR_ID_EXTRACTOR = PREFIX + ".keystore.identity_extractor";
    private IdFromCertExtractor identityExtractor = new BabelCredentialHandler();
    private static final String PAR_ID_GENERATOR = PREFIX + ".keystore.identity_generator";
    private IdentityGenerator identityGenerator = (BabelCredentialHandler) identityExtractor;

    private static final String PAR_TRUST_STORE_TYPE = PREFIX + ".truststore.type";
    private String trustStoreType = "PKCS12";
    private static final String PAR_TRUST_STORE_PWD = PREFIX + ".truststore.password";
    private static final String PAR_TRUST_STORE_PROTECTION = PREFIX + ".truststore.protection_handler";
    private ProtectionParameter trustStoreProtection = EMPTY_PWD;
    private static final String PAR_TRUST_STORE_PATH = PREFIX + ".truststore.path";
    private static final String DEF_TRUST_STORE_PATH = "babelTrustStore.jks";
    private String trustStoreLoadPath = null;
    /**
     * Can be a String (a path) or a boolean. If boolean and true, trustStoreWritePath
     * must be set to trustStoreLoadPath
     */
    private static final String PAR_TRUST_STORE_WRITABLE = PREFIX + ".truststore.writable";
    private String trustStoreWritePath = null;

    private static final String PAR_SECRET_STORE_TYPE = PREFIX + ".secretstore.type";
    private String secretStoreType = "PKCS12";
    private static final String PAR_SECRET_STORE_PWD = PREFIX + ".secretstore.password";
    private static final String PAR_SECRET_STORE_PROTECTION = PREFIX + ".secretstore.protection_handler";
    private ProtectionParameter secretStoreProtection = EMPTY_PWD;
    private static final String PAR_SECRET_STORE_PATH = PREFIX + ".secretstore.path";
    private static final String DEF_SECRET_STORE_PATH = "babelSecretStore.jks";
    private String secretStoreLoadPath = null;
    /**
     * Can be a String (a path) or a boolean. If boolean and true, secretStoreWritePath
     * must be set to secretStoreLoadPath
     */
    private static final String PAR_SECRET_STORE_WRITABLE = PREFIX + ".secretstore.writable";
    private String secretStoreWritePath = null;
    private static final String PAR_SECRET_ALG = PREFIX + ".secretkey_algorithm";
    private String secretKeyAlgorithm = "AES";
    private static final String PAR_SECRET_LEN = PREFIX + ".secretkey_length";
    private int secretKeyLength = 128;

    /** Algorithm for password based key derivation function */
    private static final String PAR_PBKDF_ALG = PREFIX + ".pbkdf.algorithm";
    private String pbkdfAlgorithm = "PBKDF2WithHmacSHA256";
    private static final String PAR_PBKDF_PWD = PREFIX + ".pbkdf.password";
    private String pbkdfPassword = null;
    /** Base64 encoded salt for the PBKDF */
    private static final String PAR_PBKDF_SALT = PREFIX + ".pbkdf.salt";
    private byte[] pbkdfSalt = "Babel sa(u)lt defa(u)lt! You (or I?) should change this!!!".getBytes();
    private static final String PAR_PBKDF_ITERATIONS = PREFIX + ".pbkdf.iterations";
    private int pbkdfIterations = 131072; // TODO find the biggest number that's fast enough on an RPi
    private static final String PAR_PBKDF_KEY_LEN = PREFIX + ".pbkdf.key_length";
    private int pbkdfKeyLength = 256;

    // See https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html
    public static final String PAR_HASH_ALG = PREFIX + ".hash_algorithm";
    private String hashAlgorithm = "SHA256";

    public static final String PAR_MAC_ALG = PREFIX + ".mac_algorithm";
    private String macAlgorithm = null;
    // The defaults loosely follow TLS 1.3 as described in https://www.rfc-editor.org/rfc/rfc5288
    public static final String PAR_CIPHER_TRANSFORM = PREFIX + ".cipher.transformation";
    private String cipherTransform = null;
    public static final String PAR_CIPHER_MODE = PREFIX + ".cipher.mode";
    private String cipherMode = "GCM";
    public static final String PAR_CIPHER_PADDING = PREFIX + ".cipher.padding";
    private String cipherPadding = "NoPadding";
    public static final String PAR_CIPHER_IV_SIZE = PREFIX + ".cipher.iv_size";
    public static final String PAR_CIPHER_PARAM_GEN = PREFIX + ".cipher.parameter_supplier";
    private Supplier<AlgorithmParameterSpec> cipherParameterSupplier = () -> new GCMParameterSpec(128, generateIv(12));
    //public static final String PAR_CIPHER_KEYWRAP_ALG = "kwyrap_cipher";
    /*
    public static final String PAR_... = PREFIX + ... ?
     */

    // TODO should this be a thing?
    /*
    // A protocol specific property map. These are specified as
    // "babel.security.proto.{protoId}.{prop}"
    // If the protocol name has spaces, they must be escaped
    private final Map<String, SecurityConfiguration> protoConfigs;
    */

    // Lazy loaded fields
    private KeyStore keyStore;
    private KeyStore ephKeyStore; // TODO make a keystore wrapper so both keystores can be used as one when needed
    private X509IKeyManager keyManager;

    private KeyStore trustStore;
    private KeyStore ephTrustStore; // TODO make a truststore wrapper so both keystores can be used as one when needed
    private X509ITrustManager trustManager;

    private KeyStore secretStore;
    private KeyStore ephSecretStore;

    // Fields to be instantiated at construction
    public final Provider PROVIDER = new BouncyCastleProvider();

    private final IdAliasMapper idAliasMapper;

    private final SecureRandom keyRng;
    private final SecureRandom nonceRng;

    private BabelSecurity() {
        Security.addProvider(PROVIDER);

        try {
            this.keyRng = SecureRandom.getInstance("DEFAULT", PROVIDER);
            this.nonceRng = SecureRandom.getInstance("NonceAndIV", PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }

        idAliasMapper = new IdAliasMapper();
    }

    // -------- Lazy loaders

    public KeyStore getKeyStore() {
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
                throw new AssertionError(e); // Shouldn't happen // TODO verify that the keystore type is available when loading config
            }
        }

        return keyStore;
    }

    public KeyStore getEphemeralKeyStore() {
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

    public X509IKeyManager getKeyManager() {
        if (keyManager == null) {
            try {
                // TODO get joined key store method and class (to use persistent and ephemeral stores simultaneously)
                keyManager = new X509BabelKeyManager(keyStoreProtection, idAliasMapper,
                        getKeyStore(), getEphemeralKeyStore());
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }

        return keyManager;
    }

    public KeyStore getTrustStore() {
        if (trustStore == null) {
            try {
                trustStore = loadOrCreateStore(trustStoreLoadPath, trustStoreType, trustStoreProtection);
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen // TODO verify that the keystore type is available when loading config
            }
        }

        return trustStore;
    }

    public KeyStore getEphemeralTrustStore() {
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

    public X509ITrustManager getTrustManager() {
        if (trustManager == null) {
            try {
                // TODO get joined trust store method and class (to use persistent and ephemeral stores simultaneously)
                trustManager = new X509BabelTrustManager(trustStoreProtection, identityExtractor,
                        getTrustStore(), getEphemeralTrustStore());
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }

        return trustManager;
    }

    public KeyStore getSecretStore() {
        if (secretStore == null) {
            try {
                secretStore = loadOrCreateStore(secretStoreLoadPath, secretStoreType, secretStoreProtection);
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen // TODO verify that the keystore type is available when loading config
            }
        }

        return secretStore;
    }

    public KeyStore getEphemeralSecretStore() {
        if (ephSecretStore == null) {
            try {
                logger.debug("Creating new ephemeral trust store");
                ephSecretStore = KeyStore.Builder.newInstance(secretStoreType, null, EMPTY_PWD).getKeyStore();
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen // TODO verify that the keystore type is available when loading config
            }
        }

        return ephSecretStore;
    }

    private static KeyStore loadOrCreateStore(String loadPath, String storeType, ProtectionParameter protection) throws KeyStoreException {
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

    public Signature initVerifySignature(PublicKey publicKey, byte[]... data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        return initVerifySignature(getSignatureAlgorithmFor(publicKey.getAlgorithm()), publicKey, data);
    }

    public Signature initVerifySignature(PublicKey publicKey, ByteBuffer data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        return initVerifySignature(getSignatureAlgorithmFor(publicKey.getAlgorithm()), publicKey, data);
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
        String alias = idAliasMapper.getAlias(identity);

        var result = deleteIdentity(alias);
        assert Arrays.equals(result.getRight(), identity);

        return Pair.of(result.getLeft(), alias);
    }

    public Pair<PrivateKeyEntry, byte[]> deleteIdentity(String alias) throws UnrecoverableEntryException {
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

    public IdentityCrypt generateIdentity(boolean peristToDisk) {
        return addIdentity(peristToDisk, identityGenerator.generateRandomCredentials());
    }

    public IdentityCrypt generateIdentityWithAliasPrefix(boolean peristToDisk, String aliasPrefix) {
        return addIdentityWithAliasPrefix(peristToDisk, aliasPrefix, identityGenerator.generateRandomCredentials());
    }

    public IdentityCrypt generateIdentity(boolean peristToDisk, String alias) {
        return addIdentity(peristToDisk, alias, identityGenerator.generateRandomCredentials());
    }

    public IdentityCrypt generateIdentity(boolean peristToDisk, KeyPair keyPair) {
        return addIdentity(peristToDisk, identityGenerator.generateCredentials(keyPair));
    }

    public IdentityCrypt generateIdentityWithAliasPrefix(boolean peristToDisk, String aliasPrefix, KeyPair keyPair) {
        return addIdentityWithAliasPrefix(peristToDisk, aliasPrefix, identityGenerator.generateCredentials(keyPair));
    }

    public IdentityCrypt generateIdentity(boolean peristToDisk, String alias, KeyPair keyPair) {
        return addIdentity(peristToDisk, alias, identityGenerator.generateCredentials(keyPair));
    }

    public IdentityCrypt addIdentity(boolean peristToDisk, PrivateKeyEntry keyStoreEntry) {
        return addIdentity(peristToDisk, null, keyStoreEntry);
    }

    public IdentityCrypt addIdentityWithAliasPrefix(boolean peristToDisk, String aliasPrefix, PrivateKeyEntry keyStoreEntry) {
        byte[] id;
        try {
            id = identityExtractor.extractIdentity(keyStoreEntry.getCertificate());
        } catch (CertificateException e) {
            throw new RuntimeException(e); // Shouldn't happen
        }
        String alias = aliasPrefix + "." + PeerIdEncoder.encodeToString(id);
        addIdentity(peristToDisk, alias, id, keyStoreEntry);
        try {
            return getIdentityCrypt(alias, id, keyStoreEntry);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Coludn't create IdentityCrypt from newly added id: " + e);
            return null;
        }
    }

    public IdentityCrypt addIdentity(boolean peristToDisk, String alias, PrivateKeyEntry keyStoreEntry) {
        byte[] id;
        try {
            id = identityExtractor.extractIdentity(keyStoreEntry.getCertificate());
        } catch (CertificateException e) {
            throw new RuntimeException(e); // Shouldn't happen
        }
        alias = alias == null ? PeerIdEncoder.encodeToString(id) : alias;
        addIdentity(peristToDisk, alias, id, keyStoreEntry);
        try {
            return getIdentityCrypt(alias, id, keyStoreEntry);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Coludn't create IdentityCrypt from newly added id: " + e);
            return null;
        }
    }

    public IdentityCrypt getDefaultIdentityCrypt()
            throws NoSuchAlgorithmException, UnrecoverableEntryException {
        IdentityPair idPair = idAliasMapper.getDefault();
        return idPair == null ? null : getIdentityCrypt(idPair.alias(), idPair.id());
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
            Set<IdentityPair> ids = new HashSet<>(keyStore.size() + ephKeyStore.size());
            keyStore.aliases().asIterator()
                    .forEachRemaining(alias -> ids.add(new IdentityPair(alias, idAliasMapper.getId(alias))));
            ephKeyStore.aliases().asIterator()
                    .forEachRemaining(alias -> ids.add(new IdentityPair(alias, idAliasMapper.getId(alias))));
            return ids;
        } catch (KeyStoreException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    public Set<IdentityPair> getAllIdentitiesWithPrefix(String aliasPrefix) {
        try {
            Set<IdentityPair> ids = new HashSet<>(keyStore.size() + ephKeyStore.size());

            for (var enumeration : List.of(keyStore.aliases(), ephKeyStore.aliases())) {
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
        return idAliasMapper.getDefault();
    }

    // TODO add trusted

    // ----- Auxiliary methods

    private void addIdentity(boolean peristOnDisk, String alias, byte[] id, PrivateKeyEntry entry) {
        try {
            var chosenPair = chooseKeyStore(__ -> peristOnDisk);
            KeyStore keyStore = chosenPair.getLeft();
            ProtectionParameter protection = chosenPair.getRight();
            idAliasMapper.put(alias, id);
            keyStore.setEntry(alias, entry, protection);

            if (peristOnDisk && keyStoreWritePath != null)
                keyStore.store(new FileOutputStream(keyStoreWritePath),
                        getPassword(keyStoreProtection, keyStoreWritePath));

        } catch (KeyStoreException e) {
            // From KeyStore.setEntry():
            // "KeyStoreException if the keystore has not been initialized (loaded), or
            // if this operation fails for **some other reason**"...
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException | CertificateException | IOException | UnsupportedCallbackException e) {
            // TODO deal with these
            throw new RuntimeException(e);
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

    private String getSignatureAlgorithmFor(String keyAlgorithm) {
        if (keyAlgorithm.equals("EdDSA"))
            return "EdDSA";
        return hashAlgorithm + "with" + keyAlgorithm;
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

    /* ------------------- Secrets ------------------- */

    // ------ Public methods

    // TODO Make a way to facilitate secret agreement? Might be terrible

    public SecretCrypt generateSecretFromPasswordWithAliasPrefix(boolean peristToDisk, String aliasPrefix,
                String password) {
        return addSecretWithAliasPrefix(peristToDisk, aliasPrefix, applyPBKDF(password.toCharArray(), pbkdfSalt));
    }

    public SecretCrypt generateSecretFromPasswordWithAliasPrefix(boolean peristToDisk, String aliasPrefix,
                String password, byte[] salt) {
        return addSecretWithAliasPrefix(peristToDisk, aliasPrefix, applyPBKDF(password.toCharArray(), salt));
    }

    public SecretCrypt generateSecretFromPassword(boolean peristToDisk, String alias, String password) {
        return addSecret(peristToDisk, alias, applyPBKDF(password.toCharArray(), pbkdfSalt));
    }

    public SecretCrypt generateSecretFromPassword(boolean peristToDisk, String alias, String password, byte[] salt) {
        return addSecret(peristToDisk, alias, applyPBKDF(password.toCharArray(), salt));
    }

    public SecretCrypt generateSecret(boolean peristToDisk) {
        return addSecret(peristToDisk, generateSecretKey());
    }

    public SecretCrypt generateSecretWithAliasPrefix(boolean peristToDisk, String aliasPrefix) {
        return addSecretWithAliasPrefix(peristToDisk, aliasPrefix, generateSecretKey());
    }

    public SecretCrypt generateSecret(boolean peristToDisk, String alias) {
        return addSecret(peristToDisk, alias, generateSecretKey());
    }

    public SecretCrypt addSecretWithAliasPrefix(boolean peristOnDisk, String aliasPrefix, SecretKey secretKey) {
        return addSecret(peristOnDisk, aliasPrefix + "." + generateSecretAlias(secretKey), secretKey);
    }

    public SecretCrypt addSecret(boolean peristOnDisk, SecretKey secretKey) {
        return addSecret(peristOnDisk, generateSecretAlias(secretKey), secretKey);
    }

    public SecretCrypt addSecret(boolean peristOnDisk, String alias, SecretKey secretKey) {
        try {
            var chosenPair = chooseSecretStore(__ -> peristOnDisk);
            KeyStore store = chosenPair.getLeft();
            ProtectionParameter protection = chosenPair.getRight();

            store.setEntry(alias, new KeyStore.SecretKeyEntry(secretKey), protection);

            if (peristOnDisk && keyStoreWritePath != null)
                store.store(new FileOutputStream(secretStoreWritePath),
                        getPassword(secretStoreProtection, secretStoreWritePath));
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("Couldn't store newly added secret: " + e);
        } catch (KeyStoreException | CertificateException | UnsupportedCallbackException e) {
            // From KeyStore.setEntry():
            // "KeyStoreException if the keystore has not been initialized (loaded), or
            // if this operation fails for **some other reason**"...
            throw new RuntimeException(e);
        }

        try {
            return getSecretCrypt(alias, secretKey);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Coludn't create SecretCrypt from newly added secret: " + e);
            return null;
        }
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
            var gen = KeyGenerator.getInstance(secretKeyAlgorithm);
            gen.init(secretKeyLength, keyRng);
            return gen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    private SecretCrypt getSecretCrypt(String alias, SecretKey key, String hashAlg, String cipherTransformation,
            Supplier<AlgorithmParameterSpec> cipherParameterSupplier)
            throws InvalidKeyException, NoSuchAlgorithmException {
        return new SecretCrypt(alias, key, hashAlg, cipherTransformation, cipherParameterSupplier);
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

    private SecretKey applyPBKDF(char[] password, byte[] salt) {
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

        try {
            SecretKey pbkdfKey = fac.generateSecret(
                    new PBEKeySpec(password, salt, pbkdfIterations, pbkdfKeyLength));
            return new SecretKeySpec(pbkdfKey.getEncoded(), secretKeyAlgorithm);
        } catch (InvalidKeySpecException e) {
            throw new AssertionError(e); // Shouldn't happen // TODO verify that it's valid when loading config 
        }
    }

    /* ------------------- Config ------------------- */

    // Called by Babel's loadConfig. I'm hoping these things get loaded before they
    // get used...
    void loadConfig(Properties config) {
        keyStoreType = config.getProperty(PAR_KEY_STORE_TYPE, keyStoreType);
        keyStoreLoadPath = config.getProperty(PAR_KEY_STORE_PATH);

        String param = config.getProperty(PAR_KEY_STORE_WRITABLE);
        keyStoreWritePath = param == null || param.equals("false")
                ? null
                : param.equals("true")
                        ? keyStoreLoadPath
                        : param;

        param = config.getProperty(PAR_KEY_STORE_PWD);
        keyStoreProtection = param != null
                ? new PasswordProtection(param.toCharArray())
                : loadClassParameter(ProtectionParameter.class, config,
                        PAR_KEY_STORE_PROTECTION, keyStoreProtection);

        param = config.getProperty(PAR_DEFAULT_ID);
        if (param != null)
            idAliasMapper.setDefaultAlias(param);

        identityExtractor = loadClassParameter(IdFromCertExtractor.class, config,
                PAR_ID_EXTRACTOR, identityExtractor);

        identityGenerator = loadClassParameter(IdentityGenerator.class, config,
                PAR_ID_GENERATOR, identityGenerator);

        // TODO rest of params
    }

    @SuppressWarnings("unchecked")
    private static <T> T loadClassParameter(Class<T> clazz, Properties props, String parameter, T defaultValue) {
        String className = props.getProperty(parameter);
        if (className == null)
            return defaultValue;
        try {
            return ((Class<? extends T>) Class.forName(className)).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error("Failed loading {} paremeter {}. Using default. Cause: {}", parameter, className, e);
            return defaultValue;
        }
    }
}
