package pt.unl.fct.di.novasys.babel.core;

import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.SecretKeyEntry;
import java.security.spec.AlgorithmParameterSpec;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.function.Supplier;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.internal.asn1.cms.GCMParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import pt.unl.fct.di.novasys.babel.core.security.CredentialGenerator;
import pt.unl.fct.di.novasys.babel.core.security.IdentityCrypt;
import pt.unl.fct.di.novasys.babel.core.security.SecretCrypt;
import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
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

    /* ---------- Identities ---------- */

    // ------ Public methods

    public void storeCredential(boolean peristToDisk, String alias, byte[] id, PrivateKeyEntry entry) {
        KeyStore keyStore;
        ProtectionParameter protection;
        if (peristToDisk) {
            keyStore = getKeyStore();
            protection = this.keyStoreProtection;
        } else {
            keyStore = getEphKeyStore();
            protection = EMPTY_PWD;
        }
        idAliasMapper.put(alias, id);
        try {
            keyStore.setEntry(alias, entry, protection);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public IdentityCrypt generateId(boolean peristToDisk) {
        return generateId(peristToDisk, null);
    }

    public IdentityCrypt generateId(boolean peristToDisk, String alias) {
        var keyEntry = credentialGenerator.generateRandomCredentials();
        byte[] id;
        try {
            id = idFromCertExtractor.extractId(keyEntry.getCertificate());
        } catch (CertificateException e) {
            throw new RuntimeException(e); // Shouldn't happen
        }
        alias = alias == null ? PeerIdEncoder.encodeToString(id) : alias;
        storeCredential(peristToDisk, alias, id, keyEntry);
        try {
            return getIdCrypt(alias, id, keyEntry);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Coludn't create IdentityCrypt from newly generated id: " + e);
            return null;
        }
    }

    // TODO more...

    // ----- Auxiliary methods

    private IdentityCrypt getIdCrypt(String alias, byte[] id, PrivateKeyEntry entry) throws NoSuchAlgorithmException {
        String sigAlg = hashAlgorithm;
        if (entry.getPrivateKey().getAlgorithm().equals("EdDSA"))
            sigAlg = "EdDSA";
        return getIdCrypt(alias, id, entry, sigAlg);
    }

    private IdentityCrypt getIdCrypt(String alias, byte[] id, PrivateKeyEntry entry, String sigHashOrAlg)
            throws NoSuchAlgorithmException {
        var privKey = entry.getPrivateKey();
        var pubKey = entry.getCertificate().getPublicKey();
        var certChain = entry.getCertificateChain();
        return new IdentityCrypt(alias, id, privKey, pubKey, certChain, sigHashOrAlg);
    }

    /* ---------- Secrets ---------- */

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
