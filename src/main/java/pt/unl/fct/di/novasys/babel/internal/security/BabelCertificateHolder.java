package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.cert.Certificate;

public interface BabelCertificateHolder<T extends Certificate> {
    public byte[] getPeerId();
    public T getCertificate();
}
