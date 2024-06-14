package pt.unl.fct.di.novasys.babel.core.protocols.discovery.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

public class RequestDiscovery extends ProtoRequest {

    public static final short REQUEST_ID = 32301;
    private final String serviceName;
    private final boolean listen;
    private final Host myself;
    private final String protoName;

    public RequestDiscovery(String serviceName, Host myself, String protoName, boolean listen) {
        super(REQUEST_ID);
        this.serviceName = serviceName;
        this.listen = listen;
        this.myself = myself;
        this.protoName = protoName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public boolean getListen() {
        return listen;
    }

    public Host getMyself() {
        return myself;
    }

    public String getProtoName() {
        return protoName;
    }

}
