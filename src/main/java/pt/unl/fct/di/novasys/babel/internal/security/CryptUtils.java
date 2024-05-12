package pt.unl.fct.di.novasys.babel.internal.security;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
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
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * An utility class for generating and validating cryptographic material.
 * TODO This was created for getting the JCA out of the way while prototyping. Should probably replace for something better later.
 */
public class CryptUtils {
    /** The Bouncy Castle JCA provider */
    public static final String PROVIDER = "BC";
    public static final String PK_ALGO = "RSA";
    public static final String HASH_ALGO = "SHA256";
    public static final String SIG_ALGO = HASH_ALGO+"with"+PK_ALGO;
    public static final String SK_ALGO = "AES";

    /**
     * The style with which identity names will be encoded in X.50[09] certificates. <p>
     * RFC415 style is the IETF and most commonly used style. Differs from the BouncyCastle
     * style in the ordering of the RDNs.
     */
    public static final X500NameStyle CERT_X500_NAME_STYLE = RFC4519Style.INSTANCE;
    public static final ASN1ObjectIdentifier X500_PEER_ID_OID = BCStyle.UNIQUE_IDENTIFIER;

    // TODO the following two values were simply taken from Java Cryptography: Tools and Techniques, page 140.
    // Confirm their validity later
    private static final int RSA_KEY_SIZE = 2048;
    private static final BigInteger RSA_PUBLIC_EXPONENT = RSAKeyGenParameterSpec.F4;

    private final SecureRandom keyRnd;
    private final SecureRandom nonceAndIVRnd;

    private final RSAKeyGenParameterSpec rsaParamSpec;

    private static CryptUtils instance;
    private CryptUtils() {
        SecureRandom keyRnd = null;
        SecureRandom nonceAndIVRnd = null;
        try {
            keyRnd = SecureRandom.getInstance("DEFAULT", PROVIDER);
            nonceAndIVRnd = SecureRandom.getInstance("NonceAndIVRnd", PROVIDER);
        } catch (NoSuchAlgorithmException|NoSuchProviderException never) {}
        this.keyRnd = keyRnd;
        this.nonceAndIVRnd = nonceAndIVRnd;

        this.rsaParamSpec = new RSAKeyGenParameterSpec(RSA_KEY_SIZE, RSA_PUBLIC_EXPONENT);
    }
    public static CryptUtils getInstance() {
        if (instance == null)
            instance = new CryptUtils();
        return instance;
    }

    public KeyPair createRandomKeyPair() {
        KeyPair keyPair = null;
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(PK_ALGO, PROVIDER);
            keyPairGen.initialize(rsaParamSpec, keyRnd);
            keyPair = keyPairGen.generateKeyPair();
        } catch (NoSuchAlgorithmException|NoSuchProviderException|InvalidAlgorithmParameterException never) {}
        return keyPair;
    }

    public Certificate createSelfSignedCertificate(KeyPair keyPair, String identity, int validDays) {
        // TODO choose something less tied to names and addresses...
        // TODO I think I can create my own "Style". I.e., custom RelativeDistinguishedNames. See p. 200
        X500Name myName = new X500NameBuilder(BCStyle.INSTANCE)
            .addRDN(X500_PEER_ID_OID, identity)
            .build();
        myName = X500Name.getInstance(CERT_X500_NAME_STYLE, myName); // Convert to chosen style
        var certBuilder = new JcaX509v1CertificateBuilder(
            myName,
            calculateSerialNumber(),
            calculateDate(0),
            calculateDate(validDays),
            myName,
            keyPair.getPublic()
        );

        try {
            ContentSigner signer = new JcaContentSignerBuilder(SIG_ALGO)
                                        .setProvider(PROVIDER)
                                        .setSecureRandom(keyRnd)
                                        .build(keyPair.getPrivate());
            X509CertificateHolder certHolder = certBuilder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter()
                                        .setProvider(PROVIDER)
                                        .getCertificate(certHolder);
            return cert;
        } catch (OperatorCreationException|CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param cert A X509Certificate
     */
    public static String getX509CertificatePeerId(X509Certificate cert) {
        try {
            X509CertificateHolder certHolder = new JcaX509CertificateHolder(cert);
            X500Name x500name = certHolder.getSubject();
            RDN name = x500name.getRDNs(X500_PEER_ID_OID)[0];

            return IETFUtils.valueToString(name.getFirst().getValue());
        } catch (CertificateEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private static Date calculateDate(int daysInFuture) {
        // TODO make this calculate a generalized date, to not reveal the signer's time zone
        long secs = System.currentTimeMillis() / 1000;
        return new Date((secs + (daysInFuture * 60 * 60 * 24)) * 1000);
    }

    // TODO this is obviously not privacy preserving
    private long serialNumberBase = System.currentTimeMillis();
    /**
     * Calculate a serial number using a monotonically increasing value.
     * @return a BigInteger representing the next serial number in the sequence.
     */
    private synchronized BigInteger calculateSerialNumber() {
        return BigInteger.valueOf(serialNumberBase++);
    }
}
