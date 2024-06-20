package pt.unl.fct.di.novasys.babel.core;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import pt.unl.fct.di.novasys.babel.core.security.CredentialGenerator;
import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
import pt.unl.fct.di.novasys.babel.internal.security.IdAliasMapper;
import pt.unl.fct.di.novasys.babel.internal.security.PeerIdEncoder;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

// TODO docs
class SecurityCore {

    private static final Logger logger = LogManager.getLogger(SecurityCore.class);

    record StoreTrio(KeyStore store, ProtectionParameter prot, String path) {
    }

    private final Provider provider = new BouncyCastleProvider();

    private final StoreTrio keyStore;
    private final IdAliasMapper idAliasMapper;
    private final Map<Short, String> defaultProtoIds;

    private final StoreTrio trustStore;

    private final StoreTrio secretStore;
    private final Map<Short, String> defaultProtoSecrets;

    private final X509IKeyManager keyManager;
    private final X509ITrustManager trustManager;

    private final CredentialGenerator credentialGenerator;
    private final IdFromCertExtractor idExtractor;

    SecurityCore(KeyStore privKeyStore, ProtectionParameter privKeyStoreProt, String privKeyStorePath,
            KeyStore secretKeyStore, ProtectionParameter secretStoreProt, String secretStorePath, KeyStore trustStore,
            ProtectionParameter trustStoreProt, String trustStorePath, X509IKeyManager keyManager,
            X509ITrustManager trustManager, IdAliasMapper idAliasMapper, CredentialGenerator credentialGenerator,
            IdFromCertExtractor idExtractor) {
        this.keyStore = new StoreTrio(privKeyStore, privKeyStoreProt, privKeyStorePath);
        this.idAliasMapper = idAliasMapper;
        this.secretStore = new StoreTrio(secretKeyStore, secretStoreProt, secretStorePath);
        this.trustStore = new StoreTrio(trustStore, trustStoreProt, trustStorePath);

        this.keyManager = keyManager;
        this.trustManager = trustManager;

        this.credentialGenerator = credentialGenerator;
        this.idExtractor = idExtractor;

        this.defaultProtoIds = new ConcurrentHashMap<>();
        this.defaultProtoSecrets = new ConcurrentHashMap<>();
    }

    // -------- Standard getters

    KeyStore getKeyStore() {
        return keyStore.store;
    }

    String getKeyStorePath() {
        return keyStore.path;
    }

    IdAliasMapper getIdAliasMapper() {
        return idAliasMapper;
    }

    Map<Short, String> getDefaultProtoIds() {
        return defaultProtoIds;
    }

    KeyStore getTrustStore() {
        return trustStore.store;
    }

    String getTrustStorePath() {
        return trustStore.path;
    }

    KeyStore getSecretStore() {
        return secretStore.store;
    }

    String getSecretStorePath() {
        return secretStore.path;
    }

    Map<Short, String> getDefaultProtoSecrets() {
        return defaultProtoSecrets;
    }

    X509IKeyManager getKeyManager() {
        return keyManager;
    }

    X509ITrustManager getTrustManager() {
        return trustManager;
    }

    // --------- Key store

    Set<Entry<String, byte[]>> getAllIds() {
        return idAliasMapper.entrySet();
    }

    Pair<String, byte[]> getDefaultId() {
        return Pair.of(idAliasMapper.getDefaultEntry());
    }

    Pair<String, byte[]> getDefaultProtoId(short proto) {
        var alias = defaultProtoIds.get(proto);
        return alias != null ? Pair.of(alias, idAliasMapper.getId(alias)) : null;
    }

    void setDefaultId(String alias, byte[] id) {
        try {
            if (!keyStore.store.containsAlias(alias))
                throw new IllegalArgumentException("No such alias: " + alias);
        } catch (KeyStoreException e) {
            throw new AssertionError(e); // won't happen
        }
        idAliasMapper.putDefault(alias, id);
    }

    void setDefaultId(byte[] id) {
        idAliasMapper.setDefaultId(id);
    }

    void setDefaultId(String alias) {
        idAliasMapper.setDefaultAlias(alias);
    }

    void setDefaultProtoId(short proto, String alias) {
        defaultProtoIds.put(proto, alias);
    }

    void setDefaultProtoId(short proto, byte[] id) {
        defaultProtoIds.put(proto, idAliasMapper.getAlias(id));
    }

    void createRandomId() {
        createRandomId((short) -1);
    }

    void createRandomId(short proto) {
        createRandomId(proto, null);
    }

    void createRandomId(short proto, String alias) {
        var entry = credentialGenerator.generateRandomCredentials();
        putCredential(proto, alias, entry);
    }

    void putId(short proto, String alias, KeyPair keyPair) {
        var entry = credentialGenerator.generateCredentials(keyPair);
        putCredential(proto, alias, entry);
    }

    /**
     * @param proto Use -1 to not have this be related to any protocol
     * @param alias If null, will be based on the generated id
     */
    private void putCredential(short proto, String alias, PrivateKeyEntry entry) {
        synchronized (keyStore) {
            try {
                var id = idExtractor.extractId(entry.getCertificate());
                if (alias == null)
                    alias = PeerIdEncoder.encodeToString(id);
                keyStore.store.setEntry(alias, entry, keyStore.prot);
                idAliasMapper.put(alias, id);
                defaultProtoIds.putIfAbsent(proto, alias);
            } catch (CertificateException | KeyStoreException e) {
                logger.error(
                    "putId({}, {}, KeyPair): Couldn't create key store entry: {}\nPerhaps the used CredentialGenerator is incompatible with the used IdFromCertExtractor?",
                    proto, alias, e);
                e.printStackTrace();
            }
        }
    }

    // void putTrusted()

    void putSecret(short proto, String alias, SecretKey secret) {
        //TODO secretStore.setEntry(alias, secret, protPa)
    }

    // ------------ Actions
    // sign, etc.

}
