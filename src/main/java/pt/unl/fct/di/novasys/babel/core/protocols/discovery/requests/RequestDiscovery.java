package pt.unl.fct.di.novasys.babel.core.protocols.discovery.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Request sent by a running protocol to the discovery protocol asking it to
 * search for (and optionally keep listening for) peers offering a named service.
 */
public class RequestDiscovery extends ProtoRequest {

    public static final short REQUEST_ID = 32301;
    private final String serviceName;
    private final boolean listen;
    private final Host myself;
    private final String protoName;

    /**
     * Creates a discovery request.
     *
     * @param serviceName the name of the service to discover
     * @param myself      the network address of the requesting protocol (advertised to peers)
     * @param protoName   the protocol name used to match incoming service announcements
     * @param listen      {@code true} to keep receiving {@link FoundServiceReply} replies
     *                    as new peers are found; {@code false} to stop listening
     */
    public RequestDiscovery(String serviceName, Host myself, String protoName, boolean listen) {
        super(REQUEST_ID);
        this.serviceName = serviceName;
        this.listen = listen;
        this.myself = myself;
        this.protoName = protoName;
    }

    /**
     * Returns the name of the service being discovered.
     *
     * @return the service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns whether the requesting protocol wants continuous discovery notifications.
     *
     * @return {@code true} if the protocol registered to keep receiving found-service replies
     */
    public boolean getListen() {
        return listen;
    }

    /**
     * Returns the network address of the requesting protocol.
     *
     * @return the requester's host address
     */
    public Host getMyself() {
        return myself;
    }

    /**
     * Returns the protocol name used to match incoming service announcements.
     *
     * @return the protocol name
     */
    public String getProtoName() {
        return protoName;
    }

}
