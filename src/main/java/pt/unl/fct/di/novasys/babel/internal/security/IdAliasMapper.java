package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
import pt.unl.fct.di.novasys.babel.core.security.IdentityPair;
import pt.unl.fct.di.novasys.network.data.Bytes;

public class IdAliasMapper {

    private static final Logger logger = LogManager.getLogger(IdAliasMapper.class);

    private final Map<String, byte[]> aliasToId;
    private final Map<Bytes, String> idToAlias;

    private String defaultAlias;

    public IdAliasMapper() {
        aliasToId = new ConcurrentHashMap<>();
        idToAlias = new ConcurrentHashMap<>();
    }

    public IdAliasMapper(String defaultAlias) {
        this();
        this.defaultAlias = defaultAlias;
    }

    public IdAliasMapper(String defaultAlias, byte[] defaultId) {
        this();
        putDefault(defaultAlias, defaultId);
    }

    public IdAliasMapper populateFromPrivateKeyStore(KeyStore keyStore, ProtectionParameter protParam,
            IdFromCertExtractor idExtractor) throws KeyStoreException {
        var it = keyStore.aliases().asIterator();
        while (it.hasNext()) {
            String alias = it.next();
            try {
                KeyStore.Entry entry = keyStore.getEntry(alias, protParam);
                if (entry instanceof PrivateKeyEntry privEntry) {
                    byte[] id = idExtractor.extractIdentity(privEntry.getCertificateChain()[0]);
                    this.put(alias, id);
                }
            } catch (UnrecoverableEntryException | NoSuchAlgorithmException | CertificateException e) {
                // Ignore this entry and continue
                logger.error("Couldn't read entry with alias. Ignoring. Cause: {}", alias, e);
            }
        }

        return this;
    }

    public String getAlias(byte[] id) {
        return idToAlias.get(Bytes.of(id));
    }

    public byte[] getId(String alias) {
        return aliasToId.get(alias);
    }

    public String getDefaultAlias() {
        return defaultAlias;
    }

    public byte[] getDefaultId() {
        return getId(defaultAlias);
    }

    public IdentityPair getDefault() {
        return defaultAlias != null ? new IdentityPair(defaultAlias, getId(defaultAlias))
                                    : null;
    }

    public synchronized String setDefaultAlias(String alias) {
        String old = defaultAlias;
        defaultAlias = alias;
        return old;
    }

    public synchronized void putDefault(String alias, byte[] id) {
        defaultAlias = alias;
        idToAlias.put(Bytes.of(id), alias);
        aliasToId.put(alias, id);
    }

    public synchronized void setDefaultId(byte[] id) {
        defaultAlias = getAlias(id);
    }

    public synchronized void put(String alias, byte[] id) {
        if (defaultAlias == null)
            defaultAlias = alias;

        idToAlias.put(Bytes.of(id), alias);
        aliasToId.put(alias, id);
    }

    public synchronized String removeId(byte[] id) {
        String alias = idToAlias.remove(Bytes.of(id));
        if (defaultAlias == alias && !aliasToId.isEmpty())
            defaultAlias = aliasToId.keySet().iterator().next();

        aliasToId.remove(alias);
        return alias;
    }

    public synchronized byte[] removeAlias(String alias) {
        if (defaultAlias == alias && !aliasToId.isEmpty())
            defaultAlias = aliasToId.keySet().iterator().next();

        byte[] id = aliasToId.remove(alias);
        idToAlias.remove(Bytes.of(id));
        return id;
    }

    public Set<Entry<String, byte[]>> entrySet() {
        return aliasToId.entrySet();
    }

    public Set<String> aliasSet() {
        return aliasToId.keySet();
    }

    public Set<Bytes> idSet() {
        return idToAlias.keySet();
    }

}
