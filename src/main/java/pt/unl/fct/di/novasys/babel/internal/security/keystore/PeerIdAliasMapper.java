package pt.unl.fct.di.novasys.babel.internal.security.keystore;

import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;
import pt.unl.fct.di.novasys.network.data.Bytes;

public class PeerIdAliasMapper implements IdAliasMapper {

    private static final String DEFAULT_ALIAS = "default";

    @Override
    public String getAlias(byte[] id) {
        return PeerIdEncoder.encodeToString(id);
    }

    @Override
    public String getAlias(Bytes id) {
        return PeerIdEncoder.encodeToString(id.array());
    }

    @Override
    public String getDefaultAlias() {
        return DEFAULT_ALIAS;
    }

    @Override
    public byte[] getId(String alias) {
        return PeerIdEncoder.decode(alias);
    }

}
