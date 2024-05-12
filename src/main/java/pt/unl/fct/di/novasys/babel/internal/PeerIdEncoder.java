package pt.unl.fct.di.novasys.babel.internal;

import java.util.Base64;

public class PeerIdEncoder {
    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final Base64.Decoder decoder = Base64.getDecoder();

    public static String encodeToString(byte[] peerId) {
        return encoder.encodeToString(peerId);
    }

    public static byte[] decodeString(String peerId) {
        return decoder.decode(peerId);
    }
}
