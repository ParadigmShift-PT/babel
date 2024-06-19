package pt.unl.fct.di.novasys.babel.core.security;

import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;

import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;
import pt.unl.fct.di.novasys.babel.internal.security.CryptUtils;

public class BabelCredentialGenerator implements SimpleCredentialGenerator {
    private static final int DEFAULT_VALID_CERT_DAYS = 365;

    @Override
    public PrivateKeyEntry generateCredentials(KeyPair keyPair) {
        var utils = CryptUtils.getInstance();
        var peerId = PeerIdEncoder.encodeToString(keyPair.getPublic().getEncoded());
        var cert = utils.createSelfSignedX509Certificate(keyPair, peerId, DEFAULT_VALID_CERT_DAYS);
        return new PrivateKeyEntry(keyPair.getPrivate(), new Certificate[] { cert });
    }

}
