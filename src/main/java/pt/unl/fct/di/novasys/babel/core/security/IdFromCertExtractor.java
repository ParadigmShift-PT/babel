package pt.unl.fct.di.novasys.babel.core.security;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;

// TODO documentation
@FunctionalInterface
public interface IdFromCertExtractor {
    byte[] extractId(Certificate certificate) throws CertificateException;

    default String idToString(byte[] id) {
        return PeerIdEncoder.encodeToString(id);
    }

    default String idToString(Certificate certificate) throws CertificateException {
        return idToString(extractId(certificate));
    }
}
