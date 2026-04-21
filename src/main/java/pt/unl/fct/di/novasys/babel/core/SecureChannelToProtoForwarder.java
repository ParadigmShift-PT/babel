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

/**
 * A {@link ChannelToProtoForwarder} variant for secure channels that additionally carries
 * the authenticated peer identity ({@code byte[] peerId}) on each forwarded event.
 */
public class SecureChannelToProtoForwarder extends ChannelToProtoForwarder
        implements SecureChannelListener<BabelMessage> {

    private static final Logger logger = LogManager.getLogger(ChannelToProtoForwarder.class);

    /**
     * Creates a secure forwarder for the channel identified by {@code channelId}.
     *
     * @param channelId the numeric identifier of the secure channel this forwarder is attached to
     */
    public SecureChannelToProtoForwarder(int channelId) {
        super(channelId);
    }

    /**
     * Registers a protocol as a consumer of messages arriving on this secure channel.
     *
     * @param protoId  the short protocol identifier used to route incoming messages
     * @param consumer the protocol instance that will receive the forwarded events
     */
    @Override
    public void addConsumer(short protoId, GenericProtocol consumer) {
        super.addConsumer(protoId, consumer);
    }

    /**
     * Dispatches an incoming message to the consumer registered for the message's destination
     * protocol ID, propagating the authenticated peer identity alongside the message.
     *
     * @param message the received Babel message
     * @param host    the remote host that sent the message
     * @param peerId  the authenticated identifier of the remote peer
     * @throws AssertionError if no consumer is registered for the message's destination protocol
     */
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

    /**
     * Notifies all registered consumers that a message was successfully sent to the authenticated peer.
     *
     * @param addressedMessage the message that was sent
     * @param host             the remote host the message was delivered to
     * @param peerId           the authenticated identifier of the remote peer
     */
    @Override
    public void messageSent(BabelMessage addressedMessage, Host host, byte[] peerId) {
        consumers.values()
                .forEach(c -> c.deliverInternalEvent(new MessageSentEvent(addressedMessage, host, peerId, channelId)));
    }

    /**
     * Notifies all registered consumers that sending a message to a peer failed.
     * The host may be absent if the failure occurred before a host address was resolved.
     *
     * @param addressedMessage the message that could not be delivered
     * @param hostOpt          the remote host, if known at the time of failure
     * @param peerId           the authenticated identifier of the intended remote peer
     * @param cause            the cause of the failure
     */
    @Override
    public void messageFailed(BabelMessage addressedMessage, Optional<Host> hostOpt, byte[] peerId, Throwable cause) {
        consumers.values().forEach(c -> c.deliverInternalEvent(
                new MessageFailedEvent(addressedMessage, hostOpt.orElse(null), peerId, cause, channelId)));
    }

}
