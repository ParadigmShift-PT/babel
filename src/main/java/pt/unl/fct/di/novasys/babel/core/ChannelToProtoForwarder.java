package pt.unl.fct.di.novasys.babel.core;

import pt.unl.fct.di.novasys.babel.internal.*;
import pt.unl.fct.di.novasys.channel.ChannelEvent;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.network.data.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges a Babel channel to one or more protocol instances by forwarding channel events
 * (incoming messages, send confirmations, failures, and custom events) into each
 * registered protocol's internal event queue.
 */
public class ChannelToProtoForwarder implements ChannelListener<BabelMessage> {

    private static final Logger logger = LogManager.getLogger(ChannelToProtoForwarder.class);

    final int channelId;
    final Map<Short, GenericProtocol> consumers;

    /**
     * Creates a forwarder for the channel identified by {@code channelId}.
     *
     * @param channelId the numeric identifier of the channel this forwarder is attached to
     */
    public ChannelToProtoForwarder(int channelId) {
        this.channelId = channelId;
        consumers = new ConcurrentHashMap<>();
    }

    /**
     * Registers a protocol as a consumer of messages arriving on this channel.
     * Each protocol ID may only be registered once per channel.
     *
     * @param protoId  the short protocol identifier used to route incoming messages
     * @param consumer the protocol instance that will receive the forwarded events
     * @throws AssertionError if a consumer with the same {@code protoId} is already registered
     */
    public void addConsumer(short protoId, GenericProtocol consumer) {
        if (consumers.putIfAbsent(protoId, consumer) != null)
            throw new AssertionError("Consumer with protoId " + protoId + " already exists in channel");
    }

    /**
     * Dispatches an incoming message to the consumer registered for the message's destination
     * protocol ID, or to the sole consumer when the destination is the wildcard {@code -1}.
     *
     * @param message the received Babel message
     * @param host    the remote host that sent the message
     * @throws AssertionError if no consumer is registered for the message's destination protocol
     */
    @Override
    public void deliverMessage(BabelMessage message, Host host) {
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
        channelConsumer.deliverInternalEvent(new MessageInEvent(message, host, channelId));
    }

    /**
     * Notifies all registered consumers that a message was successfully sent to {@code host}.
     *
     * @param addressedMessage the message that was sent
     * @param host             the remote host the message was delivered to
     */
    @Override
    public void messageSent(BabelMessage addressedMessage, Host host) {
        consumers.values().forEach(c -> c.deliverInternalEvent(new MessageSentEvent(addressedMessage, host, channelId)));
    }

    /**
     * Notifies all registered consumers that sending a message to {@code host} failed.
     *
     * @param addressedMessage the message that could not be delivered
     * @param host             the remote host the message was addressed to
     * @param throwable        the cause of the failure
     */
    @Override
    public void messageFailed(BabelMessage addressedMessage, Host host, Throwable throwable) {
        consumers.values().forEach(c ->
                c.deliverInternalEvent(new MessageFailedEvent(addressedMessage, host, throwable, channelId)));
    }

    /**
     * Forwards a custom channel event to all registered consumers.
     *
     * @param channelEvent the channel-specific event to propagate
     */
    @Override
    public void deliverEvent(ChannelEvent channelEvent) {
        consumers.values().forEach(v -> v.deliverInternalEvent(new CustomChannelEvent(channelEvent, channelId)));
    }
}
