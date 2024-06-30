package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
import pt.unl.fct.di.novasys.network.data.Bytes;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

public class X509BabelTrustManager extends X509ITrustManager {

    private static final Logger logger = LogManager.getLogger(X509BabelTrustManager.class);

    public enum TrustPolicy {
        UNKNOWN, KNOWN_ID, KNOWN_CERT; // TODO CERTIFICATE_AUTHORITIES?
    }

    private TrustPolicy trustPolicy;
    private final Lock policyWriteLock;
    private final Lock policyReadLock;

    // TODO store and make getter for all previously accepted (and refused)
    // identities and their certificates? Make parameter to set how many
    // certificates are saved?

    private final Collection<KeyStore> trustStores;
    private final KeyStore targetTrustStore;
    private final Set<Bytes> expectedIds;

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
    private X509CertificateChainPredicate verifyCertificateSignature;

    private final IdFromCertExtractor idExtractor;

    public X509BabelTrustManager(IdFromCertExtractor idExtractor, Collection<KeyStore> trustStores,
            TrustPolicy trustPolicy, X509CertificateChainPredicate trustUnknownPeerCallback,
            KeyStore targetTrustStore, X509CertificateChainPredicate verifyCertificateSignature)
            throws KeyStoreException {
        // Trigger KeyStoreException early if KeyStore was not initialized.
        for (KeyStore store : trustStores)
            store.size();
        targetTrustStore.size();

        ReadWriteLock policyLock = new ReentrantReadWriteLock();
        this.policyReadLock = policyLock.readLock();
        this.policyWriteLock = policyLock.writeLock();

        this.trustStores = trustStores;
        this.targetTrustStore = targetTrustStore;
        this.expectedIds = Collections.synchronizedSet(new HashSet<>());
        this.idExtractor = idExtractor;
        this.trustUnknownPeerCallback = trustUnknownPeerCallback;
        this.verifyCertificateSignature = verifyCertificateSignature;
        setTrustPolicy(trustPolicy);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkServerTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkServerTrusted(chain, null, authType);
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

        byte[] idInCert = idExtractor.extractIdentity(chain[0]);
        if (expectedId != null && !Arrays.equals(idInCert, expectedId))
            throw new CertificateException("Expected id: %s Got: %s"
                    .formatted(PeerIdEncoder.encodeToString(expectedId), PeerIdEncoder.encodeToString(idInCert)));

        policyReadLock.lock();
        try {
            if (!verifyCertificateSignature.test(chain, idInCert))
                throw new CertificateException("Certificate signature verification failed for peer %s"
                        .formatted(PeerIdEncoder.encodeToString(idInCert)));

            boolean expected = expectedIds.remove(Bytes.of(idInCert));

            if (expected || trustConsistentCertificate.test(chain, idInCert)) {
                try {
                    String alias = PeerIdEncoder.encodeToString(idInCert);
                    logger.debug("Saved peer certificate to trust store: {}", alias);
                    targetTrustStore.setCertificateEntry(alias, chain[0]);
                } catch (KeyStoreException e) {
                    // Shouldn't happen
                    logger.error(e);
                }
            } else {
                String msg = "Didn't trust certificate with identity %s. Trust manager policy was %s."
                        .formatted(PeerIdEncoder.encodeToString(idInCert), trustPolicy);
                logger.debug(msg);
                throw new CertificateException(msg);
            }
        } finally {
            policyReadLock.unlock();
        }
    }

    @Override
    public byte[] extractIdFromCertificate(X509Certificate certificate) throws CertificateException {
        return idExtractor.extractIdentity(certificate);
    }

    @Override
    public Set<Bytes> getTrustedIds() {
        HashSet<Bytes> ids = new HashSet<>(expectedIds);
        for (KeyStore store : trustStores) {
            try {
                synchronized (store) {
                    Enumeration<String> aliases = store.aliases();
                    while (aliases.hasMoreElements()) {
                        String alias = aliases.nextElement();
                        Certificate entry = store.getCertificate(alias);
                        byte[] trustedId = idExtractor.extractIdentity(entry);
                        ids.add(Bytes.of(trustedId));
                    }
                }
            } catch (KeyStoreException | CertificateException e) {
                // ignored. Continue
                logger.warn(e);
            }
        }
        return ids;
    }

    @Override
    public void addTrustedId(byte[] id) {
        expectedIds.add(Bytes.of(id));
    }

    @Override
    public void removeTrustedId(byte[] id) {
        expectedIds.remove(Bytes.of(id));
        for (KeyStore store : trustStores) {
            try {
                String alias = PeerIdEncoder.encodeToString(id);
                Certificate entry = store.getCertificate(alias);
                byte[] trustedId = idExtractor.extractIdentity(entry);
                if (Arrays.equals(trustedId, id))
                    store.deleteEntry(alias);
            } catch (KeyStoreException | CertificateException e) {
                // ignored. Continue
                logger.warn(e);
            }
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    public TrustPolicy getTrustPolicy() {
        return trustPolicy;
    }

    public void setTrustPolicy(TrustPolicy newPolicy) {
        policyWriteLock.lock();
        try {
            trustPolicy = newPolicy;

            trustConsistentCertificate = switch (trustPolicy) {
                case UNKNOWN -> (chain, id) -> true;
                case KNOWN_CERT -> this::knownCertPolicyTrustConsistentCertificate;
                case KNOWN_ID -> this::knownIdPolicyTrustConsistentCertificate;
            };
        } finally {
            policyWriteLock.unlock();
        }
    }

    public void setCertificateSignatureVerifier(X509CertificateChainPredicate verifyCertificateSignature) {
        policyWriteLock.lock();
        try {
            this.verifyCertificateSignature = verifyCertificateSignature;
        } finally {
            policyWriteLock.unlock();
        }
    }


    public void setTrustUnknownPeerCallback(X509CertificateChainPredicate trustUnknownCertCallback) {
        policyWriteLock.lock();
        try {
            this.trustUnknownPeerCallback = trustUnknownCertCallback;
        } finally {
            policyWriteLock.unlock();
        }
    }

    private boolean knownIdPolicyTrustConsistentCertificate(X509Certificate[] certificateChain, byte[] idInRootCert)
            throws CertificateException {
        if (getPeerCertificate(idInRootCert) != null) {
            return true;
        } else {
            logger.debug("Unknown identity received. Calling \"trust unkown\" callback handler.");
            return trustUnknownPeerCallback.test(certificateChain, idInRootCert);
        }
    }

    private boolean knownCertPolicyTrustConsistentCertificate(X509Certificate[] certificateChain, byte[] idInRootCert)
            throws CertificateException {
        Certificate knownCertificate = getPeerCertificate(idInRootCert);
        if (knownCertificate != null && Arrays.equals(knownCertificate.getEncoded(), certificateChain[0].getEncoded())) {
            return true;
        } else {
            logger.debug("Unknown certificate received. Calling \"trust unkown certificate\" callback handler.");
            return trustUnknownPeerCallback.test(certificateChain, idInRootCert);
        }
    }

    private Certificate getPeerCertificate(byte[] peerId) {
        for (KeyStore trustStore : trustStores) {
            try {
                Certificate cert = trustStore.getCertificate(PeerIdEncoder.encodeToString(peerId));
                byte[] certIdentity = idExtractor.extractIdentity(cert);
                if (Arrays.equals(peerId, certIdentity))
                    return cert;
            } catch (KeyStoreException | CertificateException e) {
                // ignore
            }
        }
        return null;
    }

}
