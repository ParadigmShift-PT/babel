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

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;

public enum ECPubKeySerializer implements ISerializer<ECPublicKey> {
    INSTANCE;

    private final KeyFactory keyFactory;

    private ECPubKeySerializer() {
        KeyFactory fac;
        try {
            fac = KeyFactory.getInstance("EC", new BouncyCastleProvider());
        } catch (NoSuchAlgorithmException e) {
            // Won't happen
            e.printStackTrace();
            fac = null;
        }
        this.keyFactory = fac;
    }

    @Override
    public void serialize(ECPublicKey dhPubKey, ByteBuf out) {
        byte[] encodedKey = dhPubKey.getEncoded();
        out.writeInt(encodedKey.length);
        out.writeBytes(encodedKey);
    }

    @Override
    public ECPublicKey deserialize(ByteBuf in) throws IOException {
        byte[] encodedKey = new byte[in.readInt()];
        in.readBytes(encodedKey);

        var keySpec = new X509EncodedKeySpec(encodedKey);

        try {
            return (ECPublicKey) keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException | ClassCastException e) {
            throw new IOException(e);
        }
    }

}
