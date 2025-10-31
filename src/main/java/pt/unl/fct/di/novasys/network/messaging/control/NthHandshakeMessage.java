package pt.unl.fct.di.novasys.network.messaging.control;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.data.Attributes;

import java.io.IOException;

public class NthHandshakeMessage extends ControlMessage {

    static ControlMessageSerializer serializer = new ControlMessageSerializer<NthHandshakeMessage>() {

        public void serialize(NthHandshakeMessage msg, ByteBuf out) throws IOException {
            out.writeInt(msg.magicNumber);
            out.writeByte(msg.handshakeStep);
            Attributes.serializer.serialize(msg.attributes, out);
        }

        public NthHandshakeMessage deserialize(ByteBuf in) throws IOException {
            int magicNumber = in.readInt();
            if (magicNumber != MAGIC_NUMBER)
                throw new RuntimeException("Invalid magic number: " + magicNumber);
            byte handshakeStep = in.readByte();
            Attributes attributes = Attributes.serializer.deserialize(in);
            return new NthHandshakeMessage(magicNumber, attributes, handshakeStep);
        }
    };

    public final int magicNumber;
    public final Attributes attributes;
    public final byte handshakeStep;

    public NthHandshakeMessage(Attributes attributes, int handshakeStep) {
        this(attributes, (byte) handshakeStep);
    }

    public NthHandshakeMessage(Attributes attributes, byte handshakeStep) {
        this(MAGIC_NUMBER, attributes, handshakeStep);
    }

    public NthHandshakeMessage(int magicNumber, Attributes attributes, int handshakeStep) {
        this(magicNumber, attributes, (byte) handshakeStep);
    }

    public NthHandshakeMessage(int magicNumber, Attributes attributes, byte handshakeStep) {
        super(Type.NTH_HS);
        this.attributes = attributes;
        this.magicNumber = magicNumber;
        this.handshakeStep = handshakeStep;
    }

    @Override
    public String toString() {
        return "NthHandshakeMessage{" +
                "attributes=" + attributes +
                ", magicNumber=" + magicNumber +
                ", handshakeStep=" + handshakeStep +
                '}';
    }
}
