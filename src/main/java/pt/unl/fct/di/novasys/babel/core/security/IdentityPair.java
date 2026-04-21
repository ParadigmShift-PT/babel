package pt.unl.fct.di.novasys.babel.core.security;

import pt.unl.fct.di.novasys.network.data.Bytes;

/**
 * Pairs a key-store alias with the corresponding raw Babel identity bytes.
 * Used as a lightweight reference when the full {@link IdentityCrypt} is not required.
 */
public record IdentityPair(String alias, byte[] identity) {

    /**
     * Creates an {@code IdentityPair} from an alias and a {@link Bytes} identity wrapper.
     *
     * @param alias    the key-store alias
     * @param identity the identity bytes wrapped in a {@link Bytes} object
     */
    public IdentityPair(String alias, Bytes identity) {
        this(alias, identity.array());
    }

    /**
     * Creates an {@code IdentityPair} from an existing {@link IdentityCrypt}.
     *
     * @param identityCrypt the identity handle to extract the alias and identity bytes from
     */
    public IdentityPair(IdentityCrypt identityCrypt) {
        this(identityCrypt.getAlias(), identityCrypt.getIdentity());
    }

}
