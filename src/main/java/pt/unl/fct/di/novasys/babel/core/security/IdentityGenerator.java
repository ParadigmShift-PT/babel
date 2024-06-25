package pt.unl.fct.di.novasys.babel.core.security;

import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;

public interface IdentityGenerator {
    PrivateKeyEntry generateRandomCredentials();

    PrivateKeyEntry generateCredentials(KeyPair keyPair);
}
