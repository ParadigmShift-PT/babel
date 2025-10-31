package pt.unl.fct.di.novasys.network.security;

import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;

/**
 * A {@link javax.net.ssl.X509ExtendedKeyManager} that gets and chooses keys and
 * certificates based on an implementation defined way to translate binary ids
 * to aliases.
 */
public abstract class X509IKeyManager extends X509ExtendedKeyManager {

    /**
     * @see X509KeyManager#getCertificateChain(String)
     */
    abstract public X509Certificate[] getCertificateChain(byte[] id);

    /**
     * @see X509KeyManager#getPrivateKey(String)
     */
    abstract public PrivateKey getPrivateKey(byte[] id);

    /**
     * @return The alias associated with the specified identity. {@code null} if
     *         there was no alias associated with the specified identity.
     */
    abstract public String getIdAlias(byte[] id);

    /**
     * @return The identity associated with the specified alias. {@code null} if
     *         there was no identity associated with the specified alias.
     */
    abstract public byte[] getAliasId(String alias);

    /**
     * Calls {@link #chooseClientAlias(String[], Principal[], java.net.Socket)} by
     * default. Override this implementation if needed.
     * <p>
     * <b>From {@link X509ExtendedKeyManager}:</b> <br>
     * {@inheritDoc}
     */
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return chooseClientAlias(keyType, issuers, null);
    }

    /**
     * Calls {@link #chooseServerAlias(String[], Principal[], java.net.Socket)} by
     * default. Override this implementation if needed.
     * <p>
     * <b>From {@link X509ExtendedKeyManager}:</b> <br>
     * {@inheritDoc}
     */
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return chooseServerAlias(keyType, issuers, null);
    }

    /**
     * Builds and returns an {@link X509IKeyManager} that always uses the specified
     * alias from the underlying key store.
     */
    public X509IKeyManager subKeyManagerWithAliases(String... aliases) {
        var aliasList = Arrays.asList(aliases);
        return new X509SubKeyManager(this,
                aliasList,
                aliasList.stream().map(this::getAliasId).collect(Collectors.toList()));
    }

    /**
     * Builds and returns an {@link X509IKeyManager} that always uses the specified
     * id.
     */
    public X509IKeyManager subKeyManagerWithIds(byte[]... id) {
        var idList = Arrays.asList(id);
        return new X509SubKeyManager(this,
                idList.stream().map(this::getIdAlias).collect(Collectors.toList()),
                idList);
    }

    /**
     * Builds and returns an {@link X509IKeyManager} that only uses the specified
     * aliases from the underlying key store.
     */
    public X509IKeyManager subKeyManagerWithAliases(Collection<String> aliases) {
        return new X509SubKeyManager(this,
                aliases,
                aliases.stream().map(this::getAliasId).collect(Collectors.toList()));
    }

    /**
     * Builds and returns an {@link X509IKeyManager} that only uses the specified
     * ids.
     */
    public X509IKeyManager subKeyManagerWithIds(Collection<byte[]> ids) {
        return new X509SubKeyManager(this,
                ids.stream().map(this::getIdAlias).collect(Collectors.toList()),
                ids);
    }
}
