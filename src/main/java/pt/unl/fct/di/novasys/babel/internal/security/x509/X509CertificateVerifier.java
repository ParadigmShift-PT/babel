package pt.unl.fct.di.novasys.babel.internal.security.x509;

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
import pt.unl.fct.di.novasys.channel.signed.CertificateVerifier;
import pt.unl.fct.di.novasys.channel.signed.exceptions.UntrustedCertificateEntityException;

public class X509CertificateVerifier implements CertificateVerifier {
    private static String CERT_TYPE = "X.509";

    @Override
    public void verify(Certificate certificate)
            throws SignatureException, InvalidKeyException, CertificateException, UntrustedCertificateEntityException {
        if (!certificate.getType().equals(CERT_TYPE))
            throw new CertificateException("Expected X.509 certificate, but got a "+certificate.getType()+" certificate.");

        X509Certificate x509Certificate = (X509Certificate) certificate;
        PublicKey pubKey = x509Certificate.getPublicKey();
        try {
            x509Certificate.verify(pubKey, new BouncyCastleProvider());
        } catch (NoSuchAlgorithmException e) {
            throw new CertificateException(e);
        }

        String certIdString = CryptUtils.getInstance().getX509CertificatePeerId(x509Certificate);
        String derivedIdString = PeerIdEncoder.stringFromPublicKey(x509Certificate.getPublicKey());
        if (!certIdString.equals(derivedIdString))
            throw new UntrustedCertificateEntityException(x509Certificate);
    }
}
