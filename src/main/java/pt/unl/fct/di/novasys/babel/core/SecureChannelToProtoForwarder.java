package pt.unl.fct.di.novasys.babel.core;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.babel.internal.MessageFailedEvent;
import pt.unl.fct.di.novasys.babel.internal.MessageInEvent;
import pt.unl.fct.di.novasys.babel.internal.MessageSentEvent;
import pt.unl.fct.di.novasys.channel.secure.SecureChannelListener;
import pt.unl.fct.di.novasys.network.data.Host;

public class SecureChannelToProtoForwarder extends ChannelToProtoForwarder
        implements SecureChannelListener<BabelMessage> {

    private static final Logger logger = LogManager.getLogger(ChannelToProtoForwarder.class);

    public SecureChannelToProtoForwarder(int channelId) {
        super(channelId);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException if {@code consumer} is not a secure protocol.
     */
    @Override
    public void addConsumer(short protoId, GenericProtocol consumer) {
        if (consumer.isSecureProtocol())
            super.addConsumer(protoId, consumer);
        else
            throw new IllegalArgumentException(
                    "Tried to to add a non-secure protcol as a listener for a secure channel. Consider adding the @SecureProtocol annotation to your protocol.");
    }

    @Override
    public void deliverMessage(BabelMessage message, Host host, byte[] peerId) {
        GenericProtocol channelConsumer;
        if (message.getDestProto() == -1 && consumers.size() == 1)
            channelConsumer = consumers.values().iterator().next();
        else
            channelConsumer = consumers.get(message.getDestProto());

        if (channelConsumer == null) {
            logger.error("Channel " + channelId + " received message to protoId " +
                    message.getDestProto() + " which is not registered in channel");
            throw new AssertionError("Channel " + channelId + " received message to protoId " +
                    message.getDestProto() + " which is not registered in channel");
        }
        channelConsumer.deliverMessageIn(new MessageInEvent(message, host, peerId, channelId));
    }

    @Override
    public void messageSent(BabelMessage addressedMessage, Host host, byte[] peerId) {
        consumers.values()
                .forEach(c -> c.deliverMessageSent(new MessageSentEvent(addressedMessage, host, peerId, channelId)));
    }

    @Override
    public void messageFailed(BabelMessage addressedMessage, Optional<Host> hostOpt, byte[] peerId, Throwable cause) {
        consumers.values().forEach(c -> c.deliverMessageFailed(
                new MessageFailedEvent(addressedMessage, hostOpt.orElse(null), peerId, cause, channelId)));
    }

}
