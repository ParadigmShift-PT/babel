package pt.unl.fct.di.novasys.network.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.messaging.NetworkMessage;
import pt.unl.fct.di.novasys.network.messaging.control.ControlMessage;

import java.io.IOException;
import java.util.List;

public class MessageDecoder<T> extends ByteToMessageDecoder {

    public static final String NAME = "MessageDecoder";

    private final ISerializer<T> serializer;

    private long receivedAppBytes;
    private long receivedControlBytes;
    private long receivedAppMessages;
    private long receivedControlMessages;

    public MessageDecoder(ISerializer<T> serializer) {
        this.serializer = serializer;
        this.receivedAppBytes = 0;
        this.receivedAppMessages = 0;
        this.receivedControlBytes = 0;
        this.receivedControlMessages = 0;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws IOException {
        if (in.readableBytes() < Integer.BYTES) return;

        int msgSize = in.getInt(in.readerIndex());
        if (in.readableBytes() < msgSize + Integer.BYTES)
            return;

        in.skipBytes(4);

        // Get a window of the original buffer to disallow bad deserializers from
        // reading more than they should
        int startIndex = in.readerIndex();
        ByteBuf slice = in.slice(startIndex, msgSize);

        byte code = slice.readByte();
        Object payload;
        switch (code){
            case NetworkMessage.CTRL_MSG:
                payload = ControlMessage.serializer.deserialize(slice);
                receivedControlMessages++;
                receivedControlBytes += 4 + msgSize;
                break;
            case NetworkMessage.APP_MSG:
                payload = serializer.deserialize(slice);
                receivedAppBytes += 4 + msgSize;
                receivedAppMessages++;
                break;
            default:
                throw new AssertionError("Unknown msg code in decoder: " + code);
        }
        out.add(new NetworkMessage(code, payload));

        // Advance original buffer reader index
        in.skipBytes(msgSize);
    }

    public long getReceivedAppBytes() {
        return receivedAppBytes;
    }

    public long getReceivedAppMessages() {
        return receivedAppMessages;
    }

    public long getReceivedControlBytes() {
        return receivedControlBytes;
    }

    public long getReceivedControlMessages() {
        return receivedControlMessages;
    }
}
