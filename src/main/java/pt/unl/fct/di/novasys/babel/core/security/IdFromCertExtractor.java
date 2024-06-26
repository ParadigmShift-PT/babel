package pt.unl.fct.di.novasys.babel.core.security;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

// TODO documentation
@FunctionalInterface
public interface IdFromCertExtractor {
    /**
     * @throws CertificateException if the certificate's signature i
     */
    byte[] extractIdentity(Certificate certificate) throws CertificateException;
}
