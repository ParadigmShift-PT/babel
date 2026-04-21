package pt.unl.fct.di.novasys.babel.core.security;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;

/**
 * Immutable container pairing cipher-text bytes with the {@link AlgorithmParameters}
 * (e.g. IV or GCM tag length) needed to initialise a matching decryption cipher.
 * A static {@link #serializer} handles Netty {@link ByteBuf} encoding and decoding.
 */
public record CipherData(byte[] encryptedData, AlgorithmParameters parameters) {

    private static final Logger logger = LogManager.getLogger(CipherData.class);

    public static ISerializer<CipherData> serializer = new ISerializer<CipherData>() {

        @Override
        public void serialize(CipherData cData, ByteBuf out) throws IOException {
            String algorithm = cData.parameters().getAlgorithm();
            out.writeByte(algorithm.length())
                    .writeCharSequence(algorithm, Charset.defaultCharset());

            byte[] encodedParams = cData.parameters().getEncoded();
            out.writeInt(encodedParams.length)
                    .writeBytes(encodedParams);

            out.writeBytes(cData.encryptedData());
        }

        @Override
        public CipherData deserialize(ByteBuf in) throws IOException {
            String algorithm = in.readCharSequence(in.readByte(), Charset.defaultCharset()).toString();

            byte[] encodedParams = new byte[in.readInt()];
            in.readBytes(encodedParams);

            AlgorithmParameters parameters;
            try {
                parameters = AlgorithmParameters.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                logger.warn("Tried to deserialize cipher data with unknown parameters algorithm: " + algorithm);
                throw new IOException(e);
            }
            parameters.init(encodedParams);

            byte[] encryptedData = new byte[in.readableBytes()];
            in.readBytes(encryptedData);

            return new CipherData(encryptedData, parameters);
        }

    };

}
