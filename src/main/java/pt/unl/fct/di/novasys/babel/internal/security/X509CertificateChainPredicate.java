package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Functional interface for evaluating an X.509 certificate chain together with the peer
 * identity derived from its leaf certificate.
 */
@FunctionalInterface
public interface X509CertificateChainPredicate {

    /**
     * Tests whether the given certificate chain and peer identity satisfy a security condition.
     *
     * @param certificateChain the peer's certificate chain, with the leaf at index 0
     * @param idInRootCert     raw peer identity bytes extracted from the leaf certificate
     * @return {@code true} if the condition is satisfied (e.g. the chain is trusted)
     * @throws CertificateException if a certificate processing error occurs during evaluation
     */
    boolean test(X509Certificate[] certificateChain, byte[] idInRootCert) throws CertificateException;
}
