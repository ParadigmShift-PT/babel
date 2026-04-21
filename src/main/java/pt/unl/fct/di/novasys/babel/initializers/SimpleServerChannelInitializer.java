package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.simpleclientserver.SimpleServerChannel;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.net.UnknownHostException;
import java.util.Properties;

/**
 * {@link ChannelInitializer} that creates a {@link SimpleServerChannel}, a server-side only
 * channel that listens for inbound connections and does not initiate outbound ones.
 */
public class SimpleServerChannelInitializer implements ChannelInitializer<SimpleServerChannel<BabelMessage>> {

    /**
     * Creates and returns a new {@link SimpleServerChannel} configured with the given properties.
     *
     * @param serializer the serializer for {@link BabelMessage}s
     * @param list       the channel event listener
     * @param properties channel configuration properties (must include the bind address/port)
     * @param protoId    the owning protocol's numeric ID
     * @return the initialized server channel
     * @throws UnknownHostException if the configured bind address cannot be resolved
     */
    @Override
    public SimpleServerChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer,
                                                        ChannelListener<BabelMessage> list,
                                                        Properties properties, short protoId) throws UnknownHostException {
        return new SimpleServerChannel<>(serializer, list, properties);
    }
}
