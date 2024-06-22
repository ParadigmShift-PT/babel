package pt.unl.fct.di.novasys.babel.core.security;

import pt.unl.fct.di.novasys.network.data.Bytes;

public record IdPair(String alias, byte[] id) {
    public IdPair(String alias, Bytes id) {
        this(alias, id.array());
    }
}
