package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.util.Properties;

/**
 * {@link ChannelInitializer} that creates a standard {@link TCPChannel}, a single-threaded
 * bidirectional TCP channel suitable for most protocol use cases.
 */
public class TCPChannelInitializer implements ChannelInitializer<TCPChannel<BabelMessage>> {

    /**
     * Creates and returns a new {@link TCPChannel} configured with the given properties.
     *
     * @param serializer the serializer for {@link BabelMessage}s
     * @param list       the channel event listener
     * @param properties channel configuration properties
     * @param protoId    the owning protocol's numeric ID
     * @return the initialized TCP channel
     * @throws IOException if the channel cannot bind or start
     */
    @Override
    public TCPChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer,
                                                     ChannelListener<BabelMessage> list,
                                                     Properties properties, short protoId) throws IOException {
        return new TCPChannel<>(serializer, list, properties);
    }
}
