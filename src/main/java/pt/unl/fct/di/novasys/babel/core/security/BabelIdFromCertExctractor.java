package pt.unl.fct.di.novasys.babel.core.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;
import pt.unl.fct.di.novasys.babel.internal.security.CryptUtils;

public class BabelIdFromCertExctractor implements IdFromCertExtractor {

    @Override
    public byte[] extractId(Certificate certificate) throws CertificateException {
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
