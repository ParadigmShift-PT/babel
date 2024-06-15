package pt.unl.fct.di.novasys.babel.internal.security.keystore;

import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;

public class PeerIdAliasMapper implements IdAliasMapper {

    private String defaultAlias;
    private byte[] defaultId;

    public PeerIdAliasMapper() {
    }

    public PeerIdAliasMapper(String defaultAlias) {
        setDefaultAlias(defaultAlias);
    }

    public PeerIdAliasMapper(byte[] defaultId) {
        setDefaultId(defaultId);
    }

    public PeerIdAliasMapper(String defaultAlias, byte[] defaultId) {
        setDefaultAliasAndId(defaultAlias, defaultId);
    }

    @Override
    public String getAlias(byte[] id) {
        return PeerIdEncoder.encodeToString(id);
    }

    @Override
    public byte[] getId(String alias) {
        if (alias.equals(defaultAlias))
            return defaultId;
        else
            return PeerIdEncoder.decode(alias);
    }

    @Override
    public String getDefaultAlias() {
        return defaultAlias;
    }

    @Override
    public byte[] getDefaultId() {
        return defaultId;
    }

    @Override
    public void setDefaultAlias(String alias) {
        defaultId = getId(alias);
        defaultAlias = alias;
    }

    @Override
    public void setDefaultId(byte[] id) {
        defaultAlias = getAlias(id);
        defaultId = id;
    }

    @Override
    public void setDefaultAliasAndId(String alias, byte[] id) {
        defaultAlias = alias;
        defaultId = id;
    }

}
