package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.ackos.AckosChannel;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.net.UnknownHostException;
import java.util.Properties;

/**
 * {@link ChannelInitializer} that creates an {@link AckosChannel}, a channel with
 * explicit acknowledgement-based reliability.
 */
public class AckosChannelInitializer implements ChannelInitializer<AckosChannel<BabelMessage>> {

    /**
     * Creates and returns a new {@link AckosChannel} configured with the given properties.
     *
     * @param serializer the serializer for {@link BabelMessage}s
     * @param list       the channel event listener
     * @param properties channel configuration properties
     * @param protoId    the owning protocol's numeric ID
     * @return the initialized ackos channel
     * @throws UnknownHostException if a host address in the properties cannot be resolved
     */
    @Override
    public AckosChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer,
                                                 ChannelListener<BabelMessage> list,
                                                 Properties properties, short protoId) throws UnknownHostException {
        return new AckosChannel<>(serializer, list, properties);
    }
}
