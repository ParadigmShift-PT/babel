package pt.unl.fct.di.novasys.babel.internal.security.x509;

import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;
import pt.unl.fct.di.novasys.babel.internal.security.BabelCertificateHolder;
import pt.unl.fct.di.novasys.babel.internal.security.CryptUtils;

public class BabelX509CertificateHolder implements BabelCertificateHolder<X509Certificate> {
    private final X509Certificate certificate;

    public static BabelX509CertificateHolder withNewCertificate(KeyPair keyPair, String identity, int validDays) {
        String idString = PeerIdEncoder.stringFromPublicKey(keyPair.getPublic());

        return new BabelX509CertificateHolder(
            CryptUtils.getInstance().createSelfSignedX509Certificate(keyPair, idString, validDays)
        );
    }

    public BabelX509CertificateHolder(X509Certificate certificate) {
        this.certificate = certificate;
    }

    @Override
    public byte[] getPeerId() throws CertificateEncodingException {
        String idString = CryptUtils.getInstance().getX509CertificatePeerId(certificate);
        return PeerIdEncoder.decode(idString);
    }

    @Override
    public X509Certificate getCertificate() {
        return this.certificate;
    }
}
