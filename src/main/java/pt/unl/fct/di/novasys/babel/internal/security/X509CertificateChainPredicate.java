package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@FunctionalInterface
public interface X509CertificateChainPredicate {
    boolean test(X509Certificate[] certificateChain, byte[] idInRootCert) throws CertificateException;
}
