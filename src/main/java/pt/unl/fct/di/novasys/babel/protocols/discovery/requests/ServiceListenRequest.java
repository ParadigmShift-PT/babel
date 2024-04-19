package pt.unl.fct.di.novasys.babel.protocols.discovery.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class ServiceListenRequest extends ProtoRequest {

    public static final short REQUEST_ID = 1603;

    private final String service;

    public ServiceListenRequest(String service) {
        super(REQUEST_ID);
        this.service = service;
    }
    
    public String getService() {
        return this.service;
    }
}
