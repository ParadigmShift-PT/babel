package pt.unl.fct.di.novasys.babel.core;

import java.util.concurrent.BlockingQueue;

import pt.unl.fct.di.novasys.babel.internal.InternalEvent;
import pt.unl.fct.di.novasys.network.data.Host;

public abstract class DiscoverableProtocol extends GenericProtocol {

    private Host myself;

    public DiscoverableProtocol(String protoName, short protoId) {
        super(protoName, protoId);
        this.myself = null;
    }

    public DiscoverableProtocol(String protoName, short protoId, Host myself) {
        super(protoName, protoId);
        this.myself = myself;
    }

    public DiscoverableProtocol(String protoName, short protoId, BlockingQueue<InternalEvent> policy) {
        super(protoName, protoId, policy);
        this.myself = null;
    }

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
     * DiscoverabbleProtocol and it provides information to the Babel core if there
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

    protected final void setMyself(Host h) {
        this.myself = h;
    }

    public Host getMyself() {
        return this.myself;
    }
}
