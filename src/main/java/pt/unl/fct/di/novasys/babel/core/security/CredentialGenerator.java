package pt.unl.fct.di.novasys.babel.core.security;

import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;

public interface CredentialGenerator {
    PrivateKeyEntry generateRandomCredentials();

    PrivateKeyEntry generateCredentials(KeyPair keyPair);
}
