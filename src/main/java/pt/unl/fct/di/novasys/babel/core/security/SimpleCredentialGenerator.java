package pt.unl.fct.di.novasys.babel.core.security;

import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;

import pt.unl.fct.di.novasys.babel.internal.security.CryptUtils;

@FunctionalInterface
public interface SimpleCredentialGenerator extends CredentialGenerator {
    @Override
    default PrivateKeyEntry generateRandomCredentials() {
        KeyPair keyPair = CryptUtils.getInstance().createRandomKeyPair();
        return generateCredentials(keyPair);
    }
}
