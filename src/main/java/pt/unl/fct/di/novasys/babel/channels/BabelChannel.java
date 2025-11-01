package pt.unl.fct.di.novasys.babel.channels;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

/**
 * Interface representing a channel in the Babel framework. Each protocol will
 * have a dedicated <code>BabelChannel</code> instance that corresponds to its
 * interests. This interface can be extended by classes that have multiple
 * instances of <code>BabelChannel</code>.
 */
public interface BabelChannel {

    /**
     * Sends a message through this channel to the Peer
     * 
     * @param destination
     * @param message
     */
    void sendMessage(Peer destination, BabelMessage message);

    /**
     * A <code>BabelChannel</code> can be composed of many other
     * <code>BabelChannels</code>. If that's the case for this specific
     * implementation, then this method should return true. If this implementation
     * can only send messages through one and only one communication channel, then
     * this should return false.
     * 
     * @return true if this channel is composed of multiple channels, false
     *         otherwise.
     */
    boolean isComposed();
}
