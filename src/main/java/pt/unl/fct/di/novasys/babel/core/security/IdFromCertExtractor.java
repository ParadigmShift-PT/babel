package pt.unl.fct.di.novasys.babel.core.security;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * Functional interface for deriving a node's byte identity from its certificate.
 * Implementations define the mapping from certificate contents (e.g. public key bytes,
 * a subject field, or an extension value) to the opaque identity used throughout Babel.
 */
@FunctionalInterface
public interface IdFromCertExtractor {
    /**
     * Extracts the Babel node identity from the supplied certificate.
     *
     * @param certificate the certificate to extract the identity from
     * @return the raw identity bytes
     * @throws CertificateException if the certificate is malformed or the identity cannot be extracted
     */
    byte[] extractIdentity(Certificate certificate) throws CertificateException;
}
