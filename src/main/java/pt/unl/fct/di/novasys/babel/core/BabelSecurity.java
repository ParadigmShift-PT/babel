package pt.unl.fct.di.novasys.babel.core;

import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.spec.AlgorithmParameterSpec;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.lang3.Functions;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import pt.unl.fct.di.novasys.babel.core.security.CredentialGenerator;
import pt.unl.fct.di.novasys.babel.core.security.IdentityCrypt;
import pt.unl.fct.di.novasys.babel.core.security.SecretCrypt;
import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
import pt.unl.fct.di.novasys.babel.core.security.IdentityPair;
import pt.unl.fct.di.novasys.babel.internal.security.IdAliasMapper;
import pt.unl.fct.di.novasys.babel.internal.security.PeerIdEncoder;
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

    // See https://docs.oracle.com/en/java/javase/21/docs/specs/security/standard-names.html
    public static final String PAR_HASH_ALG = PREFIX+"hash_algorithm";
    private String hashAlgorithm = "SHA256";

    public static final String PAR_MAC_ALG = PREFIX+"mac_algorithm";
    private String macAlgorithm = null;
    // The defaults loosely follow TLS 1.3 as described in https://www.rfc-editor.org/rfc/rfc5288
    public static final String PAR_CIPHER_ALG = PREFIX+"cipher.algorithm";
    private String cipherAlgorithm = null;
    public static final String PAR_CIPHER_MODE = PREFIX+"cipher.mode";
    private String cipherMode = "GCM";
    public static final String PAR_CIPHER_PADDING = PREFIX+"cipher.padding";
    private String cipherPadding = "NoPadding";
    public static final String PAR_CIPHER_PARAM_GEN = PREFIX+"cipher.parameter_supplier";
    private Supplier<AlgorithmParameterSpec> cipherParameterSupplier = () -> new GCMParameterSpec(128, generateIv(12));
    //public static final String PAR_CIPHER_IV_SIZE = PREFIX+"cipher.iv_size"; // bytes
    //private int cipherIvSize = 12;
    //public static final String PAR_CIPHER_GCM_TAG_LEN = PREFIX+"cipher.gcm_tag_length"; // bits
    //private int cipherGcmTagLength = 128;
    //public static final String PAR_CIPHER_KEYWRAP = "kwyrap_cipher";
    /*
    public static final String PAR_ = PREFIX +
     */

    //private final SecurityConfiguration config;

    // TODO should this be a thing?
    /*
    // A protocol specific property map. These are specified as
    // "babel.security.proto.{protoId}.{prop}"
    // If the protocol name has spaces, they must be escaped
    private final Map<String, SecurityConfiguration> protoConfigs;
    */

    private KeyStore keyStore;
    private ProtectionParameter keyStoreProtection;
    private KeyStore ephKeyStore; // TODO make Babel key manager support two key stores for this
    private X509IKeyManager keyManager;
    private IdAliasMapper idAliasMapper;

    private KeyStore trustStore;
    private ProtectionParameter trustStoreProtection;
    private KeyStore ephTrustStore; // TODO make Babel trust manager support two key stores for this
    private X509ITrustManager trustManager;

    private CredentialGenerator credentialGenerator;
    private IdFromCertExtractor idFromCertExtractor;

    public final Provider PROVIDER = new BouncyCastleProvider();

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

        //this.config = new SecurityConfiguration();
        // TODO
    }

    // Called by Babel's loadConfig. I'm hoping these things get loaded before they
    // get used...
    void loadConfig(Properties config) {
        /*
        Map<String, Map<String, String>> toBeParsedProtoProps = new HashMap<>();
        for (var prop : config.entrySet()) {
            String key = (String) prop.getKey();
            if (key.startsWith(PROTO)) {
                String[] split = key.split("babel\\.security\\.proto\\.|\\.");
                if (split.length != 3) // expected: { "", protoName, parameter }
                    continue;
                String protoName = split[1];
                String parameter = split[2];

                toBeParsedProtoProps.computeIfAbsent(protoName, __ -> new HashMap<>())
                        .put(parameter, (String) prop.getValue());
            }
        }
        */
        // TODO global security config
    }

    KeyStore getKeyStore() {
        throw new UnsupportedOperationException("TODO");
    }

    KeyStore getEphKeyStore() {
        throw new UnsupportedOperationException("TODO");
    }

    X509IKeyManager getKeyManager() {
        throw new UnsupportedOperationException("TODO");
    }

    X509IKeyManager getSingleKeyManager(String alias) {
        throw new UnsupportedOperationException("TODO");
    }

    X509ITrustManager getTrustManager() {
        throw new UnsupportedOperationException("TODO");
    }

    /* ---------- General public utilities ---------- */

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

    /* ----------------- Identities ----------------- */

    // ------ Public methods

    public void storeIdentity(boolean peristToDisk, String alias, byte[] id, PrivateKeyEntry entry) {
        try {
            var chosenPair = chooseKeyStore(__ -> peristToDisk);
            KeyStore keyStore = chosenPair.getLeft();
            ProtectionParameter protection = chosenPair.getRight();
            idAliasMapper.put(alias, id);
            keyStore.setEntry(alias, entry, protection);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

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
        return generateIdentity(peristToDisk, null);
    }

    public IdentityCrypt generateIdWithAliasPrefix(boolean peristToDisk, String aliasPrefix) {
        var keyEntry = credentialGenerator.generateRandomCredentials();
        byte[] id;
        try {
            id = idFromCertExtractor.extractIdentity(keyEntry.getCertificate());
        } catch (CertificateException e) {
            throw new RuntimeException(e); // Shouldn't happen
        }
        String alias = aliasPrefix + "." + PeerIdEncoder.encodeToString(id);
        storeIdentity(peristToDisk, alias, id, keyEntry);
        try {
            return getIdentityCrypt(alias, id, keyEntry);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Coludn't create IdentityCrypt from newly generated id: " + e);
            return null;
        }
    }

    public IdentityCrypt generateIdentity(boolean peristToDisk, String alias) {
        var keyEntry = credentialGenerator.generateRandomCredentials();
        byte[] id;
        try {
            id = idFromCertExtractor.extractIdentity(keyEntry.getCertificate());
        } catch (CertificateException e) {
            throw new RuntimeException(e); // Shouldn't happen
        }
        alias = alias == null ? PeerIdEncoder.encodeToString(id) : alias;
        storeIdentity(peristToDisk, alias, id, keyEntry);
        try {
            return getIdentityCrypt(alias, id, keyEntry);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Coludn't create IdentityCrypt from newly generated id: " + e);
            return null;
        }
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

    // TODO more...?

    // ----- Auxiliary methods

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
        String sigAlg = hashAlgorithm;
        if (entry.getPrivateKey().getAlgorithm().equals("EdDSA"))
            sigAlg = "EdDSA";
        return getIdentityCrypt(alias, id, entry, sigAlg);
    }

    private IdentityCrypt getIdentityCrypt(String alias, byte[] id, PrivateKeyEntry entry, String sigHashOrAlg)
            throws NoSuchAlgorithmException {
        var privKey = entry.getPrivateKey();
        var pubKey = entry.getCertificate().getPublicKey();
        var certChain = entry.getCertificateChain();
        return new IdentityCrypt(alias, id, privKey, pubKey, certChain, sigHashOrAlg);
    }

    private Pair<KeyStore, ProtectionParameter> chooseKeyStore(KeyStorePredicate shouldBePersistent)
            throws KeyStoreException {
        KeyStore persistent = getKeyStore();
        return shouldBePersistent.test(persistent)
                ? Pair.of(persistent, keyStoreProtection)
                : Pair.of(getEphKeyStore(), EMPTY_PWD);
    }

    @FunctionalInterface
    private interface KeyStorePredicate {
        boolean test(KeyStore keyStore) throws KeyStoreException;
    }

    /* ------------------- Secrets ------------------- */

    // ------ Public methods
    // TODO...

    // ----- Auxiliary methods

    private SecretCrypt getSecretCrypt(String alias, SecretKey key) throws NoSuchAlgorithmException {
        var macAlgorithm = this.macAlgorithm == null ? "Hmac" + this.hashAlgorithm : this.macAlgorithm;
        return cipherAlgorithm == null
                ? new SecretCrypt(alias, key, macAlgorithm, cipherMode, cipherPadding, cipherParameterSupplier)
                : new SecretCrypt(alias, key, macAlgorithm, cipherAlgorithm, cipherParameterSupplier);
    }

    private SecretCrypt getSecretCrypt(String alias, SecretKey key, String hashAlg, String cipherTransformation,
            Supplier<AlgorithmParameterSpec> cipherParameterSupplier)
            throws InvalidKeyException, NoSuchAlgorithmException {
        return new SecretCrypt(alias, key, hashAlg, cipherTransformation, cipherParameterSupplier);
    }

}
