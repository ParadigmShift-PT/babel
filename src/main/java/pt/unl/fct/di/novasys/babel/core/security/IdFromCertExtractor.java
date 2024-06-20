package pt.unl.fct.di.novasys.babel.core.security;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

// TODO documentation
@FunctionalInterface
public interface IdFromCertExtractor {
    byte[] extractId(Certificate certificate) throws CertificateException;
}
