package pt.unl.fct.di.novasys.babel.core.security;

import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;

/**
 * Strategy interface for creating Babel identity credentials.
 * Implementations decide how to produce a {@link PrivateKeyEntry} (private key +
 * certificate chain) from either a freshly generated or a caller-supplied key pair.
 */
public interface IdentityGenerator {
    /**
     * Generates a completely new identity using an internally created key pair.
     *
     * @return a {@link PrivateKeyEntry} containing the new private key and its certificate chain
     * @throws NoSuchAlgorithmException if the key generation algorithm is unavailable
     */
    PrivateKeyEntry generateRandomCredentials() throws NoSuchAlgorithmException;

    /**
     * Creates an identity credential wrapping the supplied key pair.
     *
     * @param keyPair the asymmetric key pair to wrap into a credential
     * @return a {@link PrivateKeyEntry} built around the given key pair
     * @throws NoSuchAlgorithmException if the required algorithm is unavailable
     */
    PrivateKeyEntry generateCredentials(KeyPair keyPair) throws NoSuchAlgorithmException;
}
