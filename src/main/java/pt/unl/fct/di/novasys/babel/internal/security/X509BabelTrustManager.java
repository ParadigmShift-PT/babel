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

/**
 * Babel-specific {@link X509ITrustManager} that enforces configurable peer-certificate trust
 * policies and exposes a live set of trusted peer identities that can be updated at runtime.
 *
 * <p>Three policies are supported:
 * <ul>
 *   <li>{@link TrustPolicy#UNKNOWN} — accept any certificate whose identity is internally consistent.</li>
 *   <li>{@link TrustPolicy#KNOWN_ID} — accept only peers whose identity (public-key hash) is already
 *       known in the trust store; unknown identities are passed to the {@code trustUnknownPeerCallback}.</li>
 *   <li>{@link TrustPolicy#KNOWN_CERT} — accept only peers whose exact certificate is already known;
 *       changed certificates for a known identity are passed to the callback.</li>
 * </ul>
 */
public class X509BabelTrustManager extends X509ITrustManager {

    private static final Logger logger = LogManager.getLogger(X509BabelTrustManager.class);

    /**
     * Determines how the trust manager evaluates an incoming peer certificate.
     */
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

    /**
     * Constructs an {@code X509BabelTrustManager} with the specified trust stores and policy.
     *
     * @param idExtractor                  extracts the raw peer identity from a certificate
     * @param trustStores                  keystore(s) holding trusted peer certificates
     * @param trustPolicy                  initial trust evaluation policy
     * @param trustUnknownPeerCallback     predicate invoked when an incoming certificate/identity is not
     *                                     yet known; returning {@code true} accepts the peer
     * @param verifyCertificateSignature   predicate that performs cryptographic signature verification
     *                                     of the certificate chain
     * @param targetTrustStore             keystore where newly trusted certificates are persisted;
     *                                     must have been initialised before construction
     * @throws KeyStoreException if any of the provided keystores has not been initialized
     */
    public X509BabelTrustManager(IdFromCertExtractor idExtractor, Collection<KeyStore> trustStores,
            TrustPolicy trustPolicy, X509CertificateChainPredicate trustUnknownPeerCallback,
            X509CertificateChainPredicate verifyCertificateSignature, KeyStore targetTrustStore)
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

    /**
     * Validates the client certificate chain using the same logic as server-certificate validation.
     *
     * @param chain    the client's certificate chain
     * @param authType the authentication type
     * @throws CertificateException if the chain is not trusted
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkServerTrusted(chain, authType);
    }

    /**
     * Validates the server certificate chain without a pre-expected peer identity.
     *
     * @param chain    the server's certificate chain
     * @param authType the authentication type
     * @throws CertificateException if the chain is not trusted
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkServerTrusted(chain, null, authType);
    }

    /**
     * Validates the client certificate chain and verifies it belongs to the expected peer identity.
     *
     * @param chain      the client's certificate chain
     * @param expectedId raw identity bytes the peer is expected to present, or {@code null} to skip check
     * @param authType   the authentication type
     * @throws CertificateException if the chain is not trusted or the identity does not match
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, byte[] expectedId, String authType)
            throws CertificateException {
        checkServerTrusted(chain, expectedId, authType);
    }

    /**
     * Validates the server certificate chain, optionally asserting it belongs to {@code expectedId},
     * and applies the current {@link TrustPolicy} to decide acceptance.
     *
     * @param chain      the server's certificate chain
     * @param expectedId raw identity bytes the peer is expected to present, or {@code null} to skip check
     * @param authType   the authentication type
     * @throws CertificateException if the chain is empty, the identity mismatches, signature verification
     *                              fails, or the current policy does not permit the certificate
     */
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
                    if (targetTrustStore != null) {
                        targetTrustStore.setCertificateEntry(alias, chain[0]);
                        logger.debug("Saved peer certificate to trust store: {}", alias);
                    }
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

    /**
     * Extracts the raw peer identity bytes from the given X.509 certificate.
     *
     * @param certificate the certificate from which to extract the identity
     * @return raw peer identity bytes
     * @throws CertificateException if the identity cannot be extracted
     */
    @Override
    public byte[] extractIdFromCertificate(X509Certificate certificate) throws CertificateException {
        return idExtractor.extractIdentity(certificate);
    }

    /**
     * Returns the union of all explicitly expected IDs and all identities found in the trust stores.
     *
     * @return set of trusted peer identity wrappers
     */
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

    /**
     * Marks a peer identity as expected, allowing its next certificate to be accepted regardless
     * of whether it already appears in the trust stores.
     *
     * @param id raw peer identity bytes to pre-authorise
     */
    @Override
    public void addTrustedId(byte[] id) {
        expectedIds.add(Bytes.of(id));
    }

    /**
     * Removes a peer identity from the set of expected IDs and deletes its certificate entry from
     * all configured trust stores.
     *
     * @param id raw peer identity bytes to remove
     */
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

    /**
     * Returns an empty array; Babel does not restrict peers by issuer CA.
     *
     * @return an empty {@link X509Certificate} array
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    /**
     * Returns the currently active trust evaluation policy.
     *
     * @return the current {@link TrustPolicy}
     */
    public TrustPolicy getTrustPolicy() {
        return trustPolicy;
    }

    /**
     * Atomically replaces the trust policy and updates the internal certificate-evaluation predicate.
     *
     * @param newPolicy the new trust policy to apply
     */
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

    /**
     * Replaces the predicate used to perform cryptographic signature verification of incoming certificate chains.
     *
     * @param verifyCertificateSignature the new signature-verification predicate
     */
    public void setCertificateSignatureVerifier(X509CertificateChainPredicate verifyCertificateSignature) {
        policyWriteLock.lock();
        try {
            this.verifyCertificateSignature = verifyCertificateSignature;
        } finally {
            policyWriteLock.unlock();
        }
    }


    /**
     * Replaces the callback invoked when an incoming certificate belongs to an unrecognised peer.
     * Returning {@code true} from the predicate accepts the peer; {@code false} rejects it.
     *
     * @param trustUnknownCertCallback the new callback predicate
     */
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
