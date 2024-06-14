package pt.unl.fct.di.novasys.babel.core.protocols.discovery.requests;


import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class RequestDiscovery extends ProtoRequest {

    public static final short REQUEST_ID = 32301;
    private final String serviceName;
    private final boolean listen;

    public RequestDiscovery(String serviceName, boolean listen) {
        super(REQUEST_ID);
        this.serviceName = serviceName;
        this.listen = listen;
    }

    public String getServiceName() {
        return serviceName;
    }

    public boolean getListen() {
        return listen;
    }
    
}
