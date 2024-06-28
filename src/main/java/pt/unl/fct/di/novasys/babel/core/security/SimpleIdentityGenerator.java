package pt.unl.fct.di.novasys.babel.core.security;

import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;

import pt.unl.fct.di.novasys.babel.core.BabelSecurity;

@FunctionalInterface
public interface SimpleIdentityGenerator extends IdentityGenerator {
    @Override
    default PrivateKeyEntry generateRandomCredentials() throws NoSuchAlgorithmException {
        KeyPair keyPair = BabelSecurity.getInstance().generateKeyPair();
        return generateCredentials(keyPair);
    }
}
