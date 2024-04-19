package pt.unl.fct.di.novasys.babel.protocols.discovery.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

public class ServiceAnounceRequest extends ProtoRequest {

    public static final short REQUEST_ID = 3603;

    private final String serviceName;
    private final Host serviceHost;

    public ServiceAnounceRequest(String serviceName, Host serviceHost) {
        super(REQUEST_ID);
        this.serviceName = serviceName;
        this.serviceHost = serviceHost;
    }
    
    public String getServiceName() {
        return this.serviceName;
    }

    public Host getServiceHost() {
        return this.serviceHost;
    }
}
