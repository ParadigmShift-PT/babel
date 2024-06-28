package pt.unl.fct.di.novasys.babel.core.security;

import pt.unl.fct.di.novasys.network.data.Bytes;

public record IdentityPair(String alias, byte[] identity) {

    public IdentityPair(String alias, Bytes identity) {
        this(alias, identity.array());
    }

    public IdentityPair(IdentityCrypt identityCrypt) {
        this(identityCrypt.getAlias(), identityCrypt.getIdentity());
    }

}
