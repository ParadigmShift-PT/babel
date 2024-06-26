package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.BabelSecurity;
import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

public class X509BabelTrustManager extends X509ITrustManager {

    private static final Logger logger = LogManager.getLogger(X509BabelTrustManager.class);

    public enum TrustPolicy {
        UNKNOWN, KNOWN_ID, KNOWN_CERT; // TODO CERTIFICATE_AUTHORITY?
    }

    private TrustPolicy trustPolicy;

    // TODO store and make getter for all previously accepted (and refused)
    // identities and their certificates? Make parameter to set how many
    // certificates are saved?

    private final Collection<KeyStore> trustStores;

    /**
     * Checks if the certificate (already verified to be consistent in id) should be
     * trusted.
     */
    private X509CertificateChainPredicate trustConsistentCertificate;

    /**
     * Defines what to do when a consistent unknown certificate is received when
     * {@code policy} is {@value TrustPolicy#KNOWN}.
     * <p>
     * An example is a callback that prompts the user what to do, and if they choose
     * to accept it, return {@code true} and add it to the trust store via
     * {@code BabelSecurity.getInstance().addTrustedCertificate(cert)}.
     */
    private X509CertificateChainPredicate trustUnknownPeerCallback;

    private final IdFromCertExtractor idExtractor;

    public X509BabelTrustManager(IdFromCertExtractor idExtractor, Collection<KeyStore> trustStores,
            TrustPolicy trustPolicy, X509CertificateChainPredicate trustUnknownPeerCallback)
            throws KeyStoreException {
        // Trigger KeyStoreException early if KeyStore was not initialized.
        for (KeyStore store : trustStores)
            store.size();

        this.trustStores = trustStores;
        this.idExtractor = idExtractor;
        this.trustUnknownPeerCallback = trustUnknownPeerCallback;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkServerTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0)
            throw new CertificateException("No certificate.");

        verifySelfSignedCertificate(chain[0]);
        byte[] identity = checkConsistentId(chain[0]);

        if (!trustConsistentCertificate.test(chain, identity)) {
            String msg = "Didn't trust certificate with identity %s. Trust manager policy was %s."
                    .formatted(PeerIdEncoder.encodeToString(identity), trustPolicy);
            logger.info(msg);
            throw new CertificateException(msg);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, byte[] expectedId, String authType)
            throws CertificateException {
        checkServerTrusted(chain, expectedId, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, byte[] expectedId, String authType)
            throws CertificateException {
        if (chain == null || chain.length == 0)
            throw new CertificateException("No certificate.");

        verifySelfSignedCertificate(chain[0]);
        byte[] identity = checkConsistentId(chain[0], expectedId);

        if (!trustConsistentCertificate.test(chain, identity)) {
            String msg = "Didn't trust certificate with identity %s. Trust manager policy was %s."
                    .formatted(PeerIdEncoder.encodeToString(identity), trustPolicy);
            logger.info(msg);
            throw new CertificateException(msg);
        }
    }

    @Override
    public byte[] extractIdFromCertificate(X509Certificate certificate) throws CertificateException {
        return idExtractor.extractIdentity(certificate);
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

    public TrustPolicy getTrustPolicy() {
        return trustPolicy;
    }

    public void setTrustPolicy(TrustPolicy newPolicy) {
        trustPolicy = newPolicy;

        trustConsistentCertificate = switch (trustPolicy) {
            case UNKNOWN -> (chain, id) -> true;
            case KNOWN_CERT -> this::knownCertPolicyTrustConsistentCertificate;
            case KNOWN_ID -> this::knownIdPolicyTrustConsistentCertificate;
        };
    }

    public void setTrustUnknownPeerCallback(X509CertificateChainPredicate trustUnknownCertCallback) {
        this.trustUnknownPeerCallback = trustUnknownCertCallback;
    }

    private boolean knownIdPolicyTrustConsistentCertificate(X509Certificate[] certificateChain, byte[] idInRootCert)
            throws CertificateException {
        for (KeyStore store : trustStores) {
            try {
                Enumeration<String> aliases = store.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    X509Certificate entry = (X509Certificate) store.getCertificate(alias);
                    byte[] trustedId = extractIdFromCertificate(entry);
                    if (Arrays.equals(idInRootCert, trustedId))
                        return true;
                }
            } catch (NullPointerException | ClassCastException ignore) {
                // Continue loop
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }
        logger.debug("Unknown identity received. Calling \"trust unkown\" callback handler.");
        return trustUnknownPeerCallback.test(certificateChain, idInRootCert);
    }

    private boolean knownCertPolicyTrustConsistentCertificate(X509Certificate[] certificateChain, byte[] idInRootCert)
            throws CertificateException {
        for (KeyStore store : trustStores) {
            try {
                Enumeration<String> aliases = store.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Certificate entry = store.getCertificate(alias);
                    if (Arrays.equals(entry.getEncoded(), certificateChain[0].getEncoded()))
                        return true;
                }
            } catch (KeyStoreException e) {
                throw new AssertionError(e); // Shouldn't happen
            }
        }
        logger.debug("Unknown certificate received. Calling \"trust unkown certificate\" callback handler.");
        return trustUnknownPeerCallback.test(certificateChain, idInRootCert);
    }

    private byte[] checkConsistentId(X509Certificate cert, byte[] expectedId) throws CertificateException {
        byte[] id = extractIdFromCertificate(cert);
        if (!Arrays.equals(id, expectedId))
            throw new CertificateException("Expected id: %s Got: %s"
                    .formatted(PeerIdEncoder.encodeToString(expectedId), PeerIdEncoder.encodeToString(id)));
        return id;
    }

    private byte[] checkConsistentId(X509Certificate cert) throws CertificateException {
        return extractIdFromCertificate(cert);
    }

    private void verifySelfSignedCertificate(X509Certificate cert) throws CertificateException {
        try {
            cert.verify(cert.getPublicKey(), BabelSecurity.getInstance().PROVIDER);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new CertificateException(e);
        }
    }

}
