package pt.unl.fct.di.novasys.channel.secure.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;

public enum X509CertificateSerializer implements ISerializer<X509Certificate> {
    INSTANCE;

    private final CertificateFactory certFactory;

    private X509CertificateSerializer() {
        CertificateFactory fac;
        try {
            fac = CertificateFactory.getInstance("X.509", new BouncyCastleProvider());
        } catch (CertificateException e) {
            // Won't happen
            e.printStackTrace();
            fac = null;
        }
        this.certFactory = fac;
    }

    @Override
    public void serialize(X509Certificate cert, ByteBuf out) throws IOException {
        try {
            byte[] encodedCert = cert.getEncoded();
            out.writeInt(encodedCert.length);
            out.writeBytes(encodedCert);
        } catch (CertificateEncodingException e) {
            throw new IOException(e);
        }
    }

    @Override
    public X509Certificate deserialize(ByteBuf in) throws IOException {
        byte[] encodedCert = new byte[in.readInt()];
        in.readBytes(encodedCert);

        try {
            return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(encodedCert));
        } catch (CertificateException e) {
            throw new IOException(e);
        }
    }
}
