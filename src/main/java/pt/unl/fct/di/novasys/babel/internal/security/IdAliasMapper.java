package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
import pt.unl.fct.di.novasys.babel.core.security.IdentityPair;
import pt.unl.fct.di.novasys.network.data.Bytes;

/**
 * Bidirectional mapping between keystore alias strings and raw peer identity bytes,
 * with support for a designated default alias used when no specific identity is requested.
 * All mutating methods are thread-safe.
 */
public class IdAliasMapper {

    private static final Logger logger = LogManager.getLogger(IdAliasMapper.class);

    // TODO maybe have a Map<byte[], Map<String, String>> to support multiple key types per identity?
    private final Map<String, byte[]> aliasToId;
    private final Map<Bytes, String> idToAlias;

    private String defaultAlias;

    /**
     * Constructs an empty mapper with no default alias.
     */
    public IdAliasMapper() {
        aliasToId = new ConcurrentHashMap<>();
        idToAlias = new ConcurrentHashMap<>();
    }

    /**
     * Constructs an empty mapper with the given alias pre-selected as the default.
     *
     * @param defaultAlias the alias to use as the default (no ID is registered yet)
     */
    public IdAliasMapper(String defaultAlias) {
        this();
        this.defaultAlias = defaultAlias;
    }

    /**
     * Constructs a mapper pre-populated with a single alias/ID pair set as the default.
     *
     * @param defaultAlias the alias to register as the default
     * @param defaultId    the raw identity bytes corresponding to {@code defaultAlias}
     */
    public IdAliasMapper(String defaultAlias, byte[] defaultId) {
        this();
        putDefault(defaultAlias, defaultId);
    }

    /**
     * Scans all private-key entries in the given keystore and registers each alias/identity pair
     * discovered by the provided {@code idExtractor}, then returns {@code this} for chaining.
     *
     * @param keyStore    the keystore to scan
     * @param protParam   protection parameter used to retrieve each private-key entry
     * @param idExtractor extracts the raw peer identity bytes from a certificate
     * @return this mapper (for fluent use)
     * @throws KeyStoreException if the keystore has not been initialized
     */
    public synchronized IdAliasMapper populateFromPrivateKeyStore(KeyStore keyStore, ProtectionParameter protParam,
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

    /**
     * Returns the alias associated with the given raw identity bytes, or {@code null} if unknown.
     *
     * @param id raw peer identity bytes
     * @return the corresponding alias, or {@code null}
     */
    public String getAlias(byte[] id) {
        return id == null ? null : idToAlias.get(Bytes.of(id));
    }

    /**
     * Returns the raw identity bytes associated with the given alias, or {@code null} if unknown.
     *
     * @param alias keystore alias
     * @return the corresponding raw ID bytes, or {@code null}
     */
    public byte[] getId(String alias) {
        return alias == null ? null : aliasToId.get(alias);
    }

    /**
     * Returns the currently designated default alias.
     *
     * @return the default alias, or {@code null} if none is set
     */
    public String getDefaultAlias() {
        return defaultAlias;
    }

    /**
     * Returns the raw identity bytes for the default alias.
     *
     * @return raw ID bytes of the default identity, or {@code null} if no default is set
     */
    public byte[] getDefaultId() {
        return getId(defaultAlias);
    }

    /**
     * Returns the default alias and its associated identity as an {@link IdentityPair}.
     *
     * @return the default {@link IdentityPair}, or {@code null} if no default alias is set
     */
    public IdentityPair getDefault() {
        return defaultAlias == null ? null : new IdentityPair(defaultAlias, getId(defaultAlias));
    }

    /**
     * Sets the default alias to {@code alias} and returns the previous default alias.
     *
     * @param alias the new default alias
     * @return the previous default alias, or {@code null}
     */
    public synchronized String setDefaultAlias(String alias) {
        String old = defaultAlias;
        defaultAlias = alias;
        return old;
    }

    /**
     * Registers {@code alias}/{@code id} as the default entry, replacing any previous default.
     * Both the alias-to-ID and ID-to-alias mappings are updated atomically.
     *
     * @param alias the alias to set as default
     * @param id    the raw identity bytes to associate with {@code alias}
     */
    public synchronized void putDefault(String alias, byte[] id) {
        defaultAlias = alias;
        if (alias != null && id != null) {
            idToAlias.put(Bytes.of(id), alias);
            aliasToId.put(alias, id);
        }
    }

    /**
     * Sets the default alias to the alias currently mapped to the given raw identity bytes.
     *
     * @param id raw identity bytes whose mapped alias becomes the new default
     */
    public synchronized void setDefaultId(byte[] id) {
        defaultAlias = getAlias(id);
    }

    /**
     * Registers an alias/ID pair; if no default has been set yet, this pair becomes the default.
     * Does nothing if either argument is {@code null}.
     *
     * @param alias keystore alias to register
     * @param id    raw identity bytes to associate with {@code alias}
     */
    public synchronized void put(String alias, byte[] id) {
        if (alias == null || id == null)
            return;

        if (defaultAlias == null)
            defaultAlias = alias;

        idToAlias.put(Bytes.of(id), alias);
        aliasToId.put(alias, id);
    }

    /**
     * Removes the entry identified by raw identity bytes and returns its alias.
     * If the removed entry was the default, a new default is chosen arbitrarily from the remaining entries.
     *
     * @param id raw identity bytes to remove
     * @return the alias that was associated with {@code id}, or {@code null} if not found
     */
    public synchronized String removeId(byte[] id) {
        if (id == null)
            return null;

        String alias = idToAlias.remove(Bytes.of(id));
        if (defaultAlias == alias)
            defaultAlias = aliasToId.isEmpty() ? null : aliasToId.keySet().iterator().next();

        aliasToId.remove(alias);
        return alias;
    }

    /**
     * Removes the entry identified by alias and returns its raw identity bytes.
     * If the removed entry was the default, a new default is chosen arbitrarily from the remaining entries.
     *
     * @param alias keystore alias to remove
     * @return the raw identity bytes that were associated with {@code alias}, or {@code null} if not found
     */
    public synchronized byte[] removeAlias(String alias) {
        if (alias == null)
            return null;

        if (defaultAlias == alias)
            defaultAlias = aliasToId.isEmpty() ? null : aliasToId.keySet().iterator().next();

        byte[] id = aliasToId.remove(alias);
        idToAlias.remove(Bytes.of(id));
        return id;
    }

    /**
     * Returns a view of all alias-to-ID entries in this mapper.
     *
     * @return set of {@code alias → id} map entries
     */
    public Set<Entry<String, byte[]>> entrySet() {
        return aliasToId.entrySet();
    }

    /**
     * Returns the set of all registered aliases.
     *
     * @return set of alias strings
     */
    public Set<String> aliasSet() {
        return aliasToId.keySet();
    }

    /**
     * Returns the set of all registered raw identity byte arrays (wrapped as {@link Bytes}).
     *
     * @return set of identity wrappers
     */
    public Set<Bytes> idSet() {
        return idToAlias.keySet();
    }

}
