package pt.unl.fct.di.novasys.babel.core.protocols.discovery.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Reply sent by the discovery protocol to a running protocol when a peer
 * offering the requested service is found on the network.
 */
public class FoundServiceReply extends ProtoReply {

    public final static short REPLY_ID = 32302;
    private final Host serviceHost;

    /**
     * Creates a reply carrying the network address of the discovered service.
     *
     * @param serviceHost the host address at which the discovered service is reachable
     */
    public FoundServiceReply(Host serviceHost) {
        super(REPLY_ID);
        this.serviceHost = serviceHost;
    }

    /**
     * Returns the host address of the discovered service.
     *
     * @return the service host
     */
    public Host getServiceHost() {
        return serviceHost;
    }

}
