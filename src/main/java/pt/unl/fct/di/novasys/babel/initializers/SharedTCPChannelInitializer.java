package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.tcp.SharedTCPChannel;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.util.Properties;

/**
 * {@link ChannelInitializer} that creates a {@link SharedTCPChannel}, a TCP channel that
 * multiplexes multiple protocols over a single underlying connection to each peer.
 */
public class SharedTCPChannelInitializer implements ChannelInitializer<SharedTCPChannel<BabelMessage>> {

    /**
     * Creates and returns a new {@link SharedTCPChannel} configured with the given properties.
     *
     * @param iSerializer     the serializer for {@link BabelMessage}s
     * @param channelListener the channel event listener
     * @param properties      channel configuration properties
     * @param protoId         the owning protocol's numeric ID
     * @return the initialized shared TCP channel
     * @throws IOException if the channel cannot bind or start
     */
    @Override
    public SharedTCPChannel<BabelMessage> initialize(ISerializer<BabelMessage> iSerializer, ChannelListener<BabelMessage> channelListener,
                                                     Properties properties, short protoId) throws IOException {
        return new SharedTCPChannel<>(iSerializer, channelListener, properties);
    }
}
