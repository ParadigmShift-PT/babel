package pt.unl.fct.di.novasys.babel.core;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.util.Map;

/**
 * Multiplexing serializer for {@link BabelMessage} frames.
 * Writes and reads a 6-byte header (source protocol id, destination protocol id, message type id)
 * followed by the payload produced by the per-message-type {@link ISerializer} registered for that id.
 */
public class BabelMessageSerializer implements ISerializer<BabelMessage> {

    /** Map from message type id to the serializer responsible for encoding/decoding that message type. */
    Map<Short, ISerializer<? extends ProtoMessage>> serializers;

    /**
     * Creates a serializer backed by the given serializer map.
     *
     * @param serializers a mutable map that will be populated via {@link #registerProtoSerializer}
     *                    and consulted during {@link #serialize} and {@link #deserialize}
     */
    public BabelMessageSerializer(Map<Short, ISerializer<? extends ProtoMessage>> serializers) {
        this.serializers = serializers;
    }

    /**
     * Registers a serializer for the given message type id.
     * Throws if a serializer for that id is already registered.
     *
     * @param msgCode        the numeric message type id
     * @param protoSerializer the serializer to associate with {@code msgCode}
     * @throws AssertionError if a serializer for {@code msgCode} is already registered
     */
    public void registerProtoSerializer(short msgCode, ISerializer<? extends ProtoMessage> protoSerializer) {
        if (serializers.putIfAbsent(msgCode, protoSerializer) != null)
            throw new AssertionError("Trying to re-register serializer in Babel: " + msgCode);
    }

    /**
     * Serializes a {@link BabelMessage} into the given buffer by writing the source protocol id,
     * destination protocol id, message type id, and then delegating payload encoding to the
     * registered serializer for that message type.
     *
     * @param msg     the message to serialize
     * @param byteBuf the target buffer
     * @throws IOException    if the payload serializer throws
     * @throws AssertionError if no serializer is registered for the message type id
     */
    @Override
    public void serialize(BabelMessage msg, ByteBuf byteBuf) throws IOException {
        byteBuf.writeShort(msg.getSourceProto());
        byteBuf.writeShort(msg.getDestProto());
        byteBuf.writeShort(msg.getMessage().getId());
        ISerializer iSerializer = serializers.get(msg.getMessage().getId());
        if(iSerializer == null){
            throw new AssertionError("No serializer found for message id " + msg.getMessage().getId());
        }
        iSerializer.serialize(msg.getMessage(), byteBuf);
    }

    /**
     * Deserializes a {@link BabelMessage} from the given buffer by reading the source protocol id,
     * destination protocol id, and message type id, then delegating payload decoding to the
     * registered serializer for that message type.
     *
     * @param byteBuf the source buffer
     * @return the reconstructed {@link BabelMessage}
     * @throws IOException    if the payload deserializer throws
     * @throws AssertionError if no deserializer is registered for the message type id read from the buffer
     */
    @Override
    public BabelMessage deserialize(ByteBuf byteBuf) throws IOException {
        short source = byteBuf.readShort();
        short dest = byteBuf.readShort();
        short id = byteBuf.readShort();
        ISerializer<? extends ProtoMessage> iSerializer = serializers.get(id);
        if(iSerializer == null){
            throw new AssertionError("No deserializer found for message id " + id);
        }
        ProtoMessage deserialize = iSerializer.deserialize(byteBuf);
        return new BabelMessage(deserialize, source, dest);
    }
}
