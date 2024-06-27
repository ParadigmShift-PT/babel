package pt.unl.fct.di.novasys.babel.core.security;

import pt.unl.fct.di.novasys.network.data.Bytes;

public record IdentityPair(String alias, byte[] id) {

    public IdentityPair(String alias, Bytes id) {
        this(alias, id.array());
    }

    public IdentityPair(IdentityCrypt identityCrypt) {
        this(identityCrypt.getAlias(), identityCrypt.getId());
    }

}
