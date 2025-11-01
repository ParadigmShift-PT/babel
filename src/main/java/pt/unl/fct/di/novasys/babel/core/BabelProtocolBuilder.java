package pt.unl.fct.di.novasys.babel.core;

import java.util.SortedSet;

import pt.unl.fct.di.novasys.babel.channels.BabelChannel;

/**
 * Class that all the protocol builders must extend. This class functions as an
 * interface. The reason it is an abstract class instead is to have some
 * protected classes.
 */
public abstract class BabelProtocolBuilder {

    /**
     * All protocols must show what types of channel they are interested in and in
     * what order. Once {@link #build(BabelRuntime runtime, BabelChannel channel)
     * build} is called, the {@link BabelChannel BabelChannel} received will follow
     * the specifications returned by this method.
     * 
     * @return a {@link SortedSet sorted set} with the channels which the protocol
     *         is interested in
     */
    protected abstract SortedSet<String> getChannelTypeInterest();

    /**
     * Builds the <code>BabelProtocol</code> assigned to this builder. This is only
     * called by the <code>Babel</code> class
     * 
     * @param runtime the <code>Babel</code> object that called this method
     * @param channel channel with the desired properties
     * @return the protocol running already
     * @see BabelRuntime
     */
    protected abstract BabelProtocol build(BabelRuntime runtime, BabelChannel channel);
}
