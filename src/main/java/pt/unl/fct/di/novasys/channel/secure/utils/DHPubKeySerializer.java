package pt.unl.fct.di.novasys.channel.secure.utils;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.interfaces.DHPublicKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;

public enum DHPubKeySerializer implements ISerializer<DHPublicKey> {
    INSTANCE;

    private final KeyFactory keyFactory;

    private DHPubKeySerializer() {
        KeyFactory fac;
        try {
            fac = KeyFactory.getInstance("DH", new BouncyCastleProvider());
        } catch (NoSuchAlgorithmException e) {
            // Won't happen
            e.printStackTrace();
            fac = null;
        }
        this.keyFactory = fac;
    }

    @Override
    public void serialize(DHPublicKey dhPubKey, ByteBuf out) {
        byte[] encodedKey = dhPubKey.getEncoded();
        out.writeInt(encodedKey.length);
        out.writeBytes(encodedKey);
    }

    @Override
    public DHPublicKey deserialize(ByteBuf in) throws IOException {
        byte[] encodedKey = new byte[in.readInt()];
        in.readBytes(encodedKey);

        var keySpec = new X509EncodedKeySpec(encodedKey);

        try {
            return (DHPublicKey) keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException | ClassCastException e) {
            throw new IOException(e);
        }
    }

}
