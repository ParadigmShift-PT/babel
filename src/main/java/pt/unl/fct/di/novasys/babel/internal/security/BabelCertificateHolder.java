package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

public interface BabelCertificateHolder<T extends Certificate> {
    public byte[] getPeerId() throws CertificateEncodingException;
    public T getCertificate();
}
