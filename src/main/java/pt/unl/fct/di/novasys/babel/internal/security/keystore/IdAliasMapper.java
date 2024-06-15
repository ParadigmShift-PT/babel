package pt.unl.fct.di.novasys.babel.internal.security.keystore;

/**
 * Utility to be used by IKeyStores and ITrustManagers to help them interact with
 * Key and Trust Stores.
 */
// TODO make possible for users to create and defina a default mapper using reflection
public interface IdAliasMapper {
    public String getAlias(byte[] id);

    public byte[] getId(String alias);

    public String getDefaultAlias();

    public byte[] getDefaultId();

    public void setDefaultAlias(String alias);

    public void setDefaultId(byte[] id);

    public void setDefaultAliasAndId(String alias, byte[] id);

}
