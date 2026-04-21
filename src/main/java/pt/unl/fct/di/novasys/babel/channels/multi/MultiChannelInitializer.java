package pt.unl.fct.di.novasys.babel.channels.multi;

import pt.unl.fct.di.novasys.babel.initializers.ChannelInitializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.util.Properties;

/**
 * {@link ChannelInitializer} implementation that returns the shared {@link MultiChannel} singleton,
 * registering the calling protocol as a listener. Intended to be passed to
 * {@code GenericProtocol.createChannel} when multiple protocols share a single TCP server socket.
 */
public class MultiChannelInitializer implements ChannelInitializer<IChannel<BabelMessage>> {

    /**
     * Returns (or creates) the {@link MultiChannel} singleton and registers {@code protoId}
     * as a listener, delegating to {@link MultiChannel#getInstance}.
     *
     * @param serializer the Babel message serializer
     * @param list       the channel-event listener for the calling protocol
     * @param properties channel configuration properties (see {@link MultiChannel#getInstance})
     * @param protoId    the numeric Babel protocol ID of the calling protocol
     * @return the shared {@code MultiChannel} instance
     * @throws IOException if the underlying server socket cannot be bound
     */
    @Override
    public MultiChannel initialize(ISerializer<BabelMessage> serializer, ChannelListener<BabelMessage> list,
                                   Properties properties, short protoId) throws IOException {
        return MultiChannel.getInstance(serializer, list, protoId, properties);
    }
}
