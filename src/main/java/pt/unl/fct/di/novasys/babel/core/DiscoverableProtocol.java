package pt.unl.fct.di.novasys.babel.core;

import java.util.concurrent.BlockingQueue;

import pt.unl.fct.di.novasys.babel.internal.InternalEvent;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * A {@link GenericProtocol} extension for protocols that participate in peer discovery.
 * Subclasses declare whether they still need contacts from the network and supply
 * the local host identity used when advertising themselves to other nodes.
 */
public abstract class DiscoverableProtocol extends GenericProtocol {

    private Host myself;

    /**
     * Creates a discoverable protocol with no pre-known local host address.
     *
     * @param protoName human-readable name for this protocol
     * @param protoId   unique short identifier for this protocol
     */
    public DiscoverableProtocol(String protoName, short protoId) {
        super(protoName, protoId);
        this.myself = null;
    }

    /**
     * Creates a discoverable protocol with a pre-known local host address.
     *
     * @param protoName human-readable name for this protocol
     * @param protoId   unique short identifier for this protocol
     * @param myself    the local host address to advertise during discovery
     */
    public DiscoverableProtocol(String protoName, short protoId, Host myself) {
        super(protoName, protoId);
        this.myself = myself;
    }

    /**
     * Creates a discoverable protocol with no pre-known local host address and a custom event queue policy.
     *
     * @param protoName human-readable name for this protocol
     * @param protoId   unique short identifier for this protocol
     * @param policy    the blocking queue used as the internal event queue
     */
    public DiscoverableProtocol(String protoName, short protoId, BlockingQueue<InternalEvent> policy) {
        super(protoName, protoId, policy);
        this.myself = null;
    }

    /**
     * Creates a discoverable protocol with a pre-known local host address and a custom event queue policy.
     *
     * @param protoName human-readable name for this protocol
     * @param protoId   unique short identifier for this protocol
     * @param myself    the local host address to advertise during discovery
     * @param policy    the blocking queue used as the internal event queue
     */
    public DiscoverableProtocol(String protoName, short protoId, Host myself, BlockingQueue<InternalEvent> policy) {
        super(protoName, protoId, policy);
        this.myself = myself;
    }

    /**
     * Starts the protocol. Should be implemented to start communication once all
     * the parameters have been defined.
     * 
     * Do not evoke directly.
     */
    public abstract void start();

    /**
     * This method is mandatory to be implemented by any protocol that extends the
     * DiscoverableProtocol and it provides information to the Babel core if there
     * is enough information within the protocol to allow the core to start the
     * protocol execution thread. No event will be processed by the protocol
     * until this becomes true.
     * 
     * After becoming true. this function cannot return false again.
     * 
     * @return true if protocol can start execution
     */
    public abstract boolean readyToStart();

    /**
     * This method must be implemented by any protocol that extends the
     * DiscoverableProtocol. It should return true while the protocol is still
     * requesting to get information still needs to obtain contacts form the
     * network. After changing to false it cannot change back to true
     * 
     * @return true if requires contacts to be obtained from the network
     */
    public abstract boolean needsDiscovery();

    /**
     * This method is used by the DiscoveryProtocol to provide information about
     * contacts (i.e., other nodes that are running this protocol) in the
     * network/Internet. While the discovery protocol should
     * never provide the identifier of the local host to this protocol, it might
     * provide the same host over time, and hence the protocol should be prepared to
     * deal with that.
     * 
     * @param host
     */
    public abstract void addContact(Host host);
    
    /**
     * This method is used by the DiscoveryProtocol to understand if it should announce
     * this host/protocol instance in reply to a discovery request.
     * The default behavior is to announce one self, but the method can be overloaded
     * by a protocol that has a different logic to govern this decision.
     * 
     * @return turn if this protocol should be announced, false other wise
     */
    public boolean isDiscoverable() {
    	return true;
    }

    /**
     * Gets any contact that this protocol has
     * 
     * @return a host representing the contact
     */
    public abstract Host getContact();

    /**
     * Sets the local host address used when advertising this protocol to discovery peers.
     *
     * @param h the local host address
     */
    protected final void setMyself(Host h) {
        this.myself = h;
    }

    /**
     * Returns the local host address associated with this protocol instance, or {@code null}
     * if it has not been set yet.
     *
     * @return the local {@link Host}, or {@code null}
     */
    public Host getMyself() {
        return this.myself;
    }
}
