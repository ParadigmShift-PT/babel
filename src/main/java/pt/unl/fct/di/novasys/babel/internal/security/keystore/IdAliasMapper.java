package pt.unl.fct.di.novasys.babel.internal.security.keystore;

import pt.unl.fct.di.novasys.network.data.Bytes;

/**
 * Utility to be used by IKeyStores and ITrustManagers to help them interact with
 * Key and Trust Stores.
 */
public interface IdAliasMapper {
    public String getAlias(byte[] id);

    public String getAlias(Bytes id);

    public String getDefaultAlias();

    public byte[] getId(String alias);

}
