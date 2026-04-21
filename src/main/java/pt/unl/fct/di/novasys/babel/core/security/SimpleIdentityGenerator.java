package pt.unl.fct.di.novasys.babel.core.security;

import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;

import pt.unl.fct.di.novasys.babel.core.BabelSecurity;

/**
 * Convenience extension of {@link IdentityGenerator} for implementations that only need to wrap
 * an existing {@link java.security.KeyPair} into a credential.
 * <p>
 * {@link #generateRandomCredentials()} is provided as a default that generates a key pair via
 * {@link BabelSecurity#generateKeyPair()} and delegates to {@link #generateCredentials(KeyPair)},
 * so implementors only need to supply {@code generateCredentials}.
 */
@FunctionalInterface
public interface SimpleIdentityGenerator extends IdentityGenerator {
    /**
     * Generates a random identity by creating a fresh key pair through {@link BabelSecurity}
     * and delegating to {@link #generateCredentials(KeyPair)}.
     *
     * @return a {@link PrivateKeyEntry} for the newly generated key pair
     * @throws NoSuchAlgorithmException if the key generation algorithm is unavailable
     */
    @Override
    default PrivateKeyEntry generateRandomCredentials() throws NoSuchAlgorithmException {
        KeyPair keyPair = BabelSecurity.getInstance().generateKeyPair();
        return generateCredentials(keyPair);
    }
}
