package pt.unl.fct.di.novasys.babel.internal.security;

import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;

/**
 * Babel-specific {@link X509IKeyManager} that selects private keys and certificate chains
 * from one or more keystores, using an {@link IdAliasMapper} to map between cryptographic
 * peer identities (raw bytes) and keystore aliases.
 */
public class X509BabelKeyManager extends X509IKeyManager {
    private final static Logger logger = LogManager.getLogger(X509BabelKeyManager.class);

    private final IdAliasMapper idAliasMapper;

    private final List<KeyStore> keyStores;
    private final ProtectionParameter protParam;

    /**
     * @throws KeyStoreException if the keystore has not been initialized (loaded).
     */
    public X509BabelKeyManager(ProtectionParameter protParam, IdFromCertExtractor idExtractor, KeyStore... keyStores)
            throws KeyStoreException {
        this(protParam, new IdAliasMapper(), keyStores);

        for (KeyStore store : keyStores)
            idAliasMapper.populateFromPrivateKeyStore(store, protParam, idExtractor);
    }

    /**
     * @throws KeyStoreException if the keystore has not been initialized (loaded).
     */
    public X509BabelKeyManager(ProtectionParameter protParam, IdAliasMapper idAliasMapper, KeyStore... keyStores)
            throws KeyStoreException {
        // Trigger KeyStoreException early if keyStore was not initialized.
        for (KeyStore ks : keyStores)
            ks.size();

        this.keyStores = List.of(keyStores);
        this.protParam = protParam;
        this.idAliasMapper = idAliasMapper;
    }

    /**
     * Returns all registered keystore aliases as server-side alias candidates.
     *
     * @param keyType unused; all registered aliases are returned regardless of key type
     * @param issuers unused
     * @return array of all known aliases
     */
    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        // This assumes idAliasMapper was correctly initialized and updated
        Set<String> aliases = idAliasMapper.aliasSet();
        return aliases.toArray(new String[aliases.size()]);
    }

    /**
     * Returns all registered keystore aliases as client-side alias candidates.
     * Delegates to {@link #getServerAliases}.
     *
     * @param keyType unused
     * @param issuers unused
     * @return array of all known aliases
     */
    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return getServerAliases(keyType, issuers);
    }

    /**
     * Chooses the default alias to present as the server identity during TLS handshake.
     *
     * @param keyType unused
     * @param issuers unused
     * @param socket  unused
     * @return the default alias from the identity mapper
     */
    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return idAliasMapper.getDefaultAlias();
    }

    /**
     * Chooses the default alias to present as the client identity during TLS handshake.
     *
     * @param keyType unused
     * @param issuers unused
     * @param socket  unused
     * @return the default alias from the identity mapper
     */
    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return idAliasMapper.getDefaultAlias();
    }

    /**
     * Returns the X.509 certificate chain for the given alias, or {@code null} if not found
     * or the chain cannot be cast to {@link X509Certificate}.
     *
     * @param alias the keystore alias to look up
     * @return the certificate chain, or {@code null}
     */
    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        try {
            Certificate[] chain = keyStoreWithAlias(alias).getCertificateChain(alias);

            var x509Chain = new X509Certificate[chain.length];
            for (int i = 0; i < chain.length; i++)
                x509Chain[i] = (X509Certificate) chain[i];

            return x509Chain;
        } catch (ClassCastException e) {
            logger.error("getCertificateChain(%s): Couldn't cast Certificate[] to X509Certificate[]", alias);
            return null;
        } catch (NullPointerException e) {
            return null;
        } catch (KeyStoreException e) {
            throw new RuntimeException(e); // Won't happen
        }
    }

    /**
     * Returns the private key for the given alias, or {@code null} if the alias is not
     * found, does not refer to a private-key entry, or the entry cannot be recovered.
     *
     * @param alias the keystore alias to look up
     * @return the private key, or {@code null}
     */
    @Override
    public PrivateKey getPrivateKey(String alias) {
        try {
            return ((PrivateKeyEntry) keyStoreWithAlias(alias).getEntry(alias, protParam)).getPrivateKey();
        } catch (ClassCastException e) {
            logger.error("getPrivateKey({}) failed because the alias didn't refer to a private key entry", alias);
            return null;
        } catch (UnrecoverableEntryException | NoSuchAlgorithmException e) {
            logger.error("getPrivateKey({}) failed with exception: {}", alias, e);
            return null;
        } catch (NullPointerException e) {
            return null;
        } catch (KeyStoreException never) {
            throw new AssertionError(never); // Won't happen
        }
    }

    /**
     * Returns the X.509 certificate chain for the peer with the given raw identity bytes.
     *
     * @param id raw peer identity bytes
     * @return the certificate chain, or {@code null} if the identity is unknown
     */
    @Override
    public X509Certificate[] getCertificateChain(byte[] id) {
        return getCertificateChain(idAliasMapper.getAlias(id));
    }

    /**
     * Returns the private key for the peer with the given raw identity bytes.
     *
     * @param id raw peer identity bytes
     * @return the private key, or {@code null} if the identity is unknown
     */
    @Override
    public PrivateKey getPrivateKey(byte[] id) {
        return getPrivateKey(idAliasMapper.getAlias(id));
    }

    /**
     * Returns the keystore alias corresponding to the given raw peer identity bytes.
     *
     * @param id raw peer identity bytes
     * @return the alias, or {@code null} if not mapped
     */
    @Override
    public String getIdAlias(byte[] id) {
        return idAliasMapper.getAlias(id);
    }

    /**
     * Returns the raw peer identity bytes corresponding to the given keystore alias.
     *
     * @param alias keystore alias
     * @return the raw identity bytes, or {@code null} if not mapped
     */
    @Override
    public byte[] getAliasId(String alias) {
        return idAliasMapper.getId(alias);
    }

    private KeyStore keyStoreWithAlias(String alias) {
        try {
            for (var ks : keyStores) {
                if (ks.containsAlias(alias))
                    return ks;
            }
            return null;
        } catch (KeyStoreException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

}
