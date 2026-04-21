package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.simpleclientserver.SimpleClientChannel;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.net.UnknownHostException;
import java.util.Properties;

/**
 * {@link ChannelInitializer} that creates a {@link SimpleClientChannel}, a client-side only
 * channel that connects outward to a server and does not accept inbound connections.
 */
public class SimpleClientChannelInitializer implements ChannelInitializer<SimpleClientChannel<BabelMessage>> {

    /**
     * Creates and returns a new {@link SimpleClientChannel} configured with the given properties.
     *
     * @param serializer the serializer for {@link BabelMessage}s
     * @param list       the channel event listener
     * @param properties channel configuration properties (must include the server address)
     * @param protoId    the owning protocol's numeric ID
     * @return the initialized client channel
     * @throws UnknownHostException if the configured server host cannot be resolved
     */
    @Override
    public SimpleClientChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer,
                                                     ChannelListener<BabelMessage> list,
                                                     Properties properties, short protoId) throws UnknownHostException {
        return new SimpleClientChannel<>(serializer, list, properties);
    }
}
