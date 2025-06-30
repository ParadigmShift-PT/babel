package pt.unl.fct.di.novasys.babel.metrics.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.network.ISerializer;

public class SendMetricsMessage extends ProtoMessage {

    public final static short ID = 2929;
    private NodeSample sample;

    public SendMetricsMessage(NodeSample sample) {
        super(ID);
        this.sample = sample;
    }

    public NodeSample getSample() {
        return this.sample;
    }

    @Override
    public String toString() {
        return sample.toString();
    }

    public static final ISerializer<SendMetricsMessage> serializer = new ISerializer<SendMetricsMessage>() {
        @Override
        public void serialize(SendMetricsMessage m, ByteBuf out) {
            out.writeBytes(m.getSample().toByteArray());
        }

        @Override
        public SendMetricsMessage deserialize(ByteBuf in) {
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);
            NodeSample sample = NodeSample.fromByteArray(bytes);
            return new SendMetricsMessage(sample);
        }
    };

}