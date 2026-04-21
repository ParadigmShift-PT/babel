package pt.unl.fct.di.novasys.babel.metrics.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.network.ISerializer;

//TODO: Message should contain host identifier -> nodeid string
/**
 * Babel protocol message that carries a {@link NodeSample} from a metrics-reporting node to the
 * {@link pt.unl.fct.di.novasys.babel.metrics.monitor.SimpleMonitor}.
 * Serialisation/deserialisation delegates directly to {@link NodeSample#toByteArray()}.
 */
public class SendMetricsMessage extends ProtoMessage {

    public final static short ID = 2929;
    private NodeSample sample;

    /**
     * Constructs a new {@code SendMetricsMessage} wrapping the given node sample.
     *
     * @param sample the metrics snapshot to send
     */
    public SendMetricsMessage(NodeSample sample) {
        super(ID);
        this.sample = sample;
    }

    /**
     * Returns the metrics snapshot carried by this message.
     *
     * @return the {@link NodeSample}
     */
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