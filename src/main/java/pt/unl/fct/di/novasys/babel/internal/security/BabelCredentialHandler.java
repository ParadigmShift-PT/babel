package pt.unl.fct.di.novasys.babel.internal.security;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.X500NameStyle;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.BigIntegers;

import pt.unl.fct.di.novasys.babel.core.BabelSecurity;
import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
import pt.unl.fct.di.novasys.babel.core.security.SimpleIdentityGenerator;

public class BabelCredentialHandler implements SimpleIdentityGenerator, IdFromCertExtractor {
    public static final X500NameStyle CERT_X500_NAME_STYLE = RFC4519Style.INSTANCE;
    public static final ASN1ObjectIdentifier X500_PEER_ID_OID = BCStyle.UNIQUE_IDENTIFIER;
    private static final int DEFAULT_VALID_CERT_DAYS = 365;

    @Override
    public PrivateKeyEntry generateCredentials(KeyPair keyPair) throws NoSuchAlgorithmException {
        var peerId = PeerIdEncoder.stringFromPublicKey(keyPair.getPublic());
        var cert = createSelfSignedX509Certificate(keyPair, peerId, DEFAULT_VALID_CERT_DAYS);
        return new PrivateKeyEntry(keyPair.getPrivate(), new Certificate[] { cert });
    }

    @Override
    public byte[] extractIdentity(Certificate certificate) throws CertificateException {
        if (certificate instanceof X509Certificate cert) {
            X509CertificateHolder certHolder = new JcaX509CertificateHolder(cert);
            X500Name x500name = certHolder.getSubject();
            RDN name = x500name.getRDNs(X500_PEER_ID_OID)[0];

            String certIdString = IETFUtils.valueToString(name.getFirst().getValue());
            certIdString = PeerIdEncoder.withoutEscapeBackslashes(certIdString);
            byte[] pubKeyId = PeerIdEncoder.fromPublicKey(cert.getPublicKey());
            String pubKeyIdString = PeerIdEncoder.encodeToString(pubKeyId);

            if (!certIdString.equals(pubKeyIdString))
                throw new CertificateException(
                        "Id in certificate didn't match id derived from public key. Expected: %s Got: %s"
                                .formatted(pubKeyIdString, certIdString));
            return pubKeyId;
        } else {
            throw new CertificateException("Only knows how to extract id from X509 certificates.");
        }
    }

    private X509Certificate createSelfSignedX509Certificate(KeyPair keyPair, String identity, int validDays) throws NoSuchAlgorithmException {
        var bcProvider = new BouncyCastleProvider();
        var babelSec = BabelSecurity.getInstance();
        X500Name myName = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(X500_PEER_ID_OID, identity)
                .build();
        myName = X500Name.getInstance(CERT_X500_NAME_STYLE, myName); // Convert to chosen style
        var certBuilder = new JcaX509v1CertificateBuilder(
                myName,
                BigIntegers.createRandomBigInteger(64, babelSec.getSecureRandom()),
                calculateDate(0),
                calculateDate(validDays),
                myName,
                keyPair.getPublic());

        try {
            ContentSigner signer = new JcaContentSignerBuilder(
                    BabelSecurity.getInstance().getSignatureAlgorithmFor(keyPair.getPublic().getAlgorithm()))
                    .setProvider(bcProvider)
                    .setSecureRandom(babelSec.getSecureRandom())
                    .build(keyPair.getPrivate());
            X509CertificateHolder certHolder = certBuilder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider(bcProvider)
                    .getCertificate(certHolder);
            return cert;
        } catch (OperatorCreationException | CertificateException e) {
            throw new AssertionError(e); // Shouldn't happen
        }
    }

    private static Date calculateDate(int daysInFuture) {
        // TODO make this calculate a generalized date, to not reveal the signer's time zone
        long secs = System.currentTimeMillis() / 1000;
        return new Date((secs + (daysInFuture * 60 * 60 * 24)) * 1000);
    }

}
