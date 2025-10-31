package pt.unl.fct.di.novasys.channel.secure.auth;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;

public class AuthenticatedMessage {
    private final byte[] data;
    private final byte[] mac;

    public AuthenticatedMessage(byte[] msg, byte[] mac) {
        this.data = msg;
        this.mac = mac;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getMac() {
        return mac;
    }

    // TODO macLength will be agreed upon during the handshake... I don't think I can pass it to the message de/encoders before that...
    public static ISerializer<AuthenticatedMessage> getSerializer(int macLength) {
        return new ISerializer<AuthenticatedMessage>() {
            @Override
            public void serialize(AuthenticatedMessage msg, ByteBuf out) throws IOException {
                out.writeBytes(msg.mac)
                    .writeInt(msg.data.length)
                    .writeBytes(msg.data);
            }

            @Override
            public AuthenticatedMessage deserialize(ByteBuf in) throws IOException {
                byte[] mac = new byte[macLength];
                in.readBytes(mac);
                byte[] data = new byte[in.readInt()];
                in.readBytes(data);
                return new AuthenticatedMessage(data, mac);
            }

        };
    }

}
