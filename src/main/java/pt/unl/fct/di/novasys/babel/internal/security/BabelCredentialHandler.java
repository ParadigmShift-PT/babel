package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
import pt.unl.fct.di.novasys.babel.core.security.SimpleCredentialGenerator;

// TODO probably should be merged with idfromcert extractor?
public class BabelCredentialHandler implements SimpleCredentialGenerator, IdFromCertExtractor {
    private static final int DEFAULT_VALID_CERT_DAYS = 365;

    @Override
    public PrivateKeyEntry generateCredentials(KeyPair keyPair) {
        var utils = CryptUtils.getInstance();
        var peerId = PeerIdEncoder.stringFromPublicKey(keyPair.getPublic());
        var cert = utils.createSelfSignedX509Certificate(keyPair, peerId, DEFAULT_VALID_CERT_DAYS);
        return new PrivateKeyEntry(keyPair.getPrivate(), new Certificate[] { cert });
    }

    @Override
    public byte[] extractIdentity(Certificate certificate) throws CertificateException {
        if (certificate instanceof X509Certificate cert) {

            PublicKey pubKey = cert.getPublicKey();
            try {
                cert.verify(pubKey, new BouncyCastleProvider());
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                throw new CertificateException(e);
            }

            String certIdString = CryptUtils.getInstance().getX509CertificatePeerId(cert);
            certIdString = PeerIdEncoder.withoutEscapeBackslashes(certIdString);
            byte[] pubKeyId = PeerIdEncoder.fromPublicKey(cert.getPublicKey());
            String pubKeyIdString = PeerIdEncoder.encodeToString(pubKeyId);

            if (!certIdString.equals(pubKeyIdString))
                throw new CertificateException(
                        "Id in certificate didn't match id derived from public key. Expected: %s Got: %s"
                                .formatted(pubKeyIdString, certIdString));
            return pubKeyId;
        } else {
            throw new CertificateException("Only knows how to extract id from X509 certificates.");
        }
    }

}
