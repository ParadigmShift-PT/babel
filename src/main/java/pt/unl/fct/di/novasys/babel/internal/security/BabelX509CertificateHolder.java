package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.cert.X509Certificate;

import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;

public class BabelX509CertificateHolder implements BabelCertificateHolder<X509Certificate> {
    private final X509Certificate certificate;

    public BabelX509CertificateHolder(X509Certificate certificate) {
        this.certificate = certificate;
    }

    @Override
    public byte[] getPeerId() {
        //TODO I am right to do this, or should I use X500 names??
        String idString = CryptUtils.getX509CertificatePeerId(certificate);
        return PeerIdEncoder.decodeString(idString);
    }

    @Override
    public X509Certificate getCertificate() {
        return this.certificate;
    }
}
