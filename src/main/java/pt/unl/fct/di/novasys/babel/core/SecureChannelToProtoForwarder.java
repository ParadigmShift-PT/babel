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

    @Override
    public void addConsumer(short protoId, GenericProtocol consumer) {
        super.addConsumer(protoId, consumer);
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
        channelConsumer.deliverInternalEvent(new MessageInEvent(message, host, peerId, channelId));
    }

    @Override
    public void messageSent(BabelMessage addressedMessage, Host host, byte[] peerId) {
        consumers.values()
                .forEach(c -> c.deliverInternalEvent(new MessageSentEvent(addressedMessage, host, peerId, channelId)));
    }

    @Override
    public void messageFailed(BabelMessage addressedMessage, Optional<Host> hostOpt, byte[] peerId, Throwable cause) {
        consumers.values().forEach(c -> c.deliverInternalEvent(
                new MessageFailedEvent(addressedMessage, hostOpt.orElse(null), peerId, cause, channelId)));
    }

}
