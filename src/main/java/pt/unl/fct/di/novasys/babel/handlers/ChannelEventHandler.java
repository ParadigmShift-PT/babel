package pt.unl.fct.di.novasys.babel.handlers;

import pt.unl.fct.di.novasys.channel.ChannelEvent;

/**
 * Functional interface for handling channel-level events.
 *
 * <p>Registered via {@code GenericProtocol.registerChannelEventHandler}. Invoked on
 * this protocol's thread when the channel emits an event of type {@code T} — for
 * example a connection-up or connection-down notification.
 *
 * @param <T> the concrete channel event type
 */
@FunctionalInterface
public interface ChannelEventHandler<T extends ChannelEvent> {

    /**
     * Called when a channel event of type {@code T} is delivered to this protocol.
     *
     * @param event     the channel event
     * @param channelId the channel that emitted the event
     */
    void handleEvent(T event, int channelId);

}
