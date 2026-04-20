package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.util.Properties;

/**
 * Factory interface for creating and configuring a Babel channel.
 *
 * <p>Implementations are passed to {@code Babel.createChannel} to instantiate a
 * specific channel type (TCP, TLS, auth, etc.) with the given serializer, listener,
 * and properties. Concrete implementations are provided for all built-in channel
 * types in the {@code pt.unl.fct.di.novasys.babel.initializers} package.
 *
 * @param <T> the concrete {@link IChannel} type produced by this initializer
 */
public interface ChannelInitializer<T extends IChannel<BabelMessage>> {

    /**
     * Creates and returns an initialized channel instance.
     *
     * @param serializer the message serializer used to encode/decode {@link BabelMessage}s
     * @param list       the listener that receives channel events and delivered messages
     * @param properties channel-specific configuration properties
     * @param protoId    the protocol ID of the owning protocol
     * @return the initialized channel
     * @throws IOException if the channel cannot bind or connect
     */
    T initialize(ISerializer<BabelMessage> serializer, ChannelListener<BabelMessage> list,
                 Properties properties, short protoId) throws IOException;
}
