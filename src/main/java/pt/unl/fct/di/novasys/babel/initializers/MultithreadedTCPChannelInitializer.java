package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.tcp.MultithreadedTCPChannel;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.util.Properties;

/**
 * {@link ChannelInitializer} that creates a {@link MultithreadedTCPChannel}, a TCP channel
 * that uses multiple I/O threads to handle concurrent connections.
 */
public class MultithreadedTCPChannelInitializer implements ChannelInitializer<MultithreadedTCPChannel<BabelMessage>> {

    /**
     * Creates and returns a new {@link MultithreadedTCPChannel} configured with the given properties.
     *
     * @param serializer the serializer for {@link BabelMessage}s
     * @param list       the channel event listener
     * @param properties channel configuration properties
     * @param protoId    the owning protocol's numeric ID
     * @return the initialized multithreaded TCP channel
     * @throws IOException if the channel cannot bind or start
     */
    @Override
    public MultithreadedTCPChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer,
                                                            ChannelListener<BabelMessage> list,
                                                            Properties properties, short protoId) throws IOException {
        return new MultithreadedTCPChannel<>(serializer, list, properties);
    }
}
