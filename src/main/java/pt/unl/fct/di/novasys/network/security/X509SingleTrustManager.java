package pt.unl.fct.di.novasys.network.security;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;

import pt.unl.fct.di.novasys.network.data.Bytes;

class X509SingleTrustManager extends X509ITrustManager {
    private final X509ITrustManager man;
    private final byte[] trustedId;

    X509SingleTrustManager(X509ITrustManager wrappedManager, byte[] trustedId) {
        this.man = wrappedManager;
        this.trustedId = trustedId;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        man.checkClientTrusted(chain, trustedId, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        man.checkServerTrusted(chain, trustedId, authType);
    }

    @Override
    public byte[] extractIdFromCertificate(X509Certificate certificate) throws CertificateException {
        return man.extractIdFromCertificate(certificate);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return man.getAcceptedIssuers();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, byte[] expectedId, String authType)
            throws CertificateException {
        if (!Arrays.equals(trustedId, expectedId))
            throw new CertificateException(
                    "ITrustManager singleTrustManager: expectedId differs from trustedId.");
        man.checkClientTrusted(chain, expectedId, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, byte[] expectedId, String authType)
            throws CertificateException {
        if (!Arrays.equals(trustedId, expectedId))
            throw new CertificateException(
                    "expectedId differs from SingleTrustManager's trustedId.");
        man.checkClientTrusted(chain, expectedId, authType);
    }

    @Override
    public Set<Bytes> getTrustedIds() {
        return Set.of(Bytes.of(trustedId));
    }

    @Override
    public void addTrustedId(byte[] id) {
        if (!Arrays.equals(trustedId, id))
            throw new UnsupportedOperationException("Can't add a new trusted id to a single trust manager.");
    }

    @Override
    public void removeTrustedId(byte[] id) {
        if (Arrays.equals(trustedId, id))
            throw new UnsupportedOperationException("Can't remove trusted id from a single trust manager.");
    }

    @Override
    public X509ITrustManager singleTrustManager(byte[] trustedId) {
        if (!Arrays.equals(this.trustedId, trustedId))
            throw new UnsupportedOperationException("Can't make a single trust manager for a different id");
        return this;
    }

}
