package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.KeyStore;
import java.security.KeyStore.ProtectionParameter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

// TODO change the name of this class to reflect that it accepts all ids, and isn't specific
public class X509BabelTrustManager extends X509ITrustManager {
    private final static Logger logger = LogManager.getLogger(X509BabelTrustManager.class);

    // TODO store and make getter for all previously accepted (and refused)
    // identities and their certificates.

    // TODO make a separate TrustManager that only accepts ids from a given
    // trustStore (like a regular trust manager), instead of all valid peer ids.

    // TODO does nothing for now
    private KeyStore trustStore;
    private ProtectionParameter protParam;

    private IdFromCertExtractor idExtractor;

    public X509BabelTrustManager() {
        this(null, null);
    }

    public X509BabelTrustManager(KeyStore trustStore, ProtectionParameter protParam) {
        this(trustStore, protParam, new BabelIdFromCertExctractor());
    }

    public X509BabelTrustManager(KeyStore trustStore, ProtectionParameter protParam, IdFromCertExtractor idExtractor) {
        this.trustStore = trustStore;
        this.protParam = protParam;
        this.idExtractor = idExtractor;
    }

    private void checkTrusted(X509Certificate cert, byte[] expectedId) throws CertificateException {
        byte[] id = extractIdFromCertificate(cert);
        if (!Arrays.equals(id, expectedId))
            throw new CertificateException("Expected id: %s Got: %s"
                    .formatted(idExtractor.idToString(expectedId), idExtractor.idToString(id)));
    }

    private void checkTrusted(X509Certificate cert) throws CertificateException {
        extractIdFromCertificate(cert);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0)
            throw new CertificateException("No certificate.");
        checkTrusted(chain[0]);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkServerTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, byte[] expectedId, String authType)
            throws CertificateException {
        if (chain == null || chain.length == 0)
            throw new CertificateException("No certificate.");
        checkTrusted(chain[0], expectedId);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, byte[] expectedId, String authType)
            throws CertificateException {
        checkServerTrusted(chain, expectedId, authType);
    }

    @Override
    public byte[] extractIdFromCertificate(X509Certificate certificate) throws CertificateException {
        return idExtractor.extractId(certificate);
    }

    @Override
    public Set<byte[]> getTrustedIds() {
        return Set.of();
    }

    @Override
    public boolean addTrustedId(byte[] id) {
        return false;
    }

    @Override
    public boolean removeTrustedId(byte[] id) {
        return false;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

}
