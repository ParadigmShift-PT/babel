package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.accrual.AccrualChannel;
import pt.unl.fct.di.novasys.channel.ackos.AckosChannel;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * {@link ChannelInitializer} that creates an {@link AccrualChannel}, a channel that uses
 * an accrual failure-detector to track peer liveness.
 */
public class AccrualChannelInitializer implements ChannelInitializer<AccrualChannel<BabelMessage>> {

    /**
     * Creates and returns a new {@link AccrualChannel} configured with the given properties.
     *
     * @param serializer the serializer for {@link BabelMessage}s
     * @param list       the channel event listener
     * @param properties channel configuration properties
     * @param protoId    the owning protocol's numeric ID
     * @return the initialized accrual channel
     * @throws IOException if the channel cannot bind or start
     */
    @Override
    public AccrualChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer,
                                                 ChannelListener<BabelMessage> list,
                                                 Properties properties, short protoId) throws IOException {
        return new AccrualChannel<>(serializer, list, properties);
    }
}
