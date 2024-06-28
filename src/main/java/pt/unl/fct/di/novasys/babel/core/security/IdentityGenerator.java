package pt.unl.fct.di.novasys.babel.core.security;

import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;

public interface IdentityGenerator {
    PrivateKeyEntry generateRandomCredentials() throws NoSuchAlgorithmException;

    PrivateKeyEntry generateCredentials(KeyPair keyPair) throws NoSuchAlgorithmException;
}
