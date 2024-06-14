package pt.unl.fct.di.novasys.babel.core.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;
import pt.unl.fct.di.novasys.babel.internal.security.CryptUtils;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

// TODO change the name of this class to reflect that it accepts all ids, and isn't specific
public class X509BabelTrustManager extends X509ITrustManager {
    private final static Logger logger = LogManager.getLogger(X509BabelTrustManager.class);

    // TODO store and make getter for all previously accepted (and refused)
    // identities and their certificates.

    // TODO make a separate TrustManager that only accepts ids from a given
    // trustStore (like a regular trust manager), instead of all valid peer ids.

    private void checkTrusted(X509Certificate cert, byte[] expectedId) throws CertificateException {
        byte[] id = extractIdFromCertificate(cert);
        if (!Arrays.equals(id, expectedId))
            throw new CertificateException("Expected id: %s Got: %s"
                    .formatted(PeerIdEncoder.encodeToString(expectedId), PeerIdEncoder.encodeToString(id)));
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
        PublicKey pubKey = certificate.getPublicKey();
        try {
            certificate.verify(pubKey, new BouncyCastleProvider());
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new CertificateException(e);
        }

        String certIdString = CryptUtils.getInstance().getX509CertificatePeerId(certificate);
        certIdString = PeerIdEncoder.withoutEscapeBackslashes(certIdString);
        byte[] pubKeyId = PeerIdEncoder.fromPublicKey(certificate.getPublicKey());
        String pubKeyIdString = PeerIdEncoder.encodeToString(pubKeyId);

        if (!certIdString.equals(pubKeyIdString))
            throw new CertificateException(
                    "Id in certificate didn't match id derived from public key. Expected: %s Got: %s"
                            .formatted(pubKeyIdString, certIdString));
        return pubKeyId;
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
