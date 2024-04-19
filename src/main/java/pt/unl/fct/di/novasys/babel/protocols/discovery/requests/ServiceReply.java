package pt.unl.fct.di.novasys.babel.protocols.discovery.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.network.data.Host;

public class ServiceReply extends ProtoReply {

    public static final short REPLY_ID = 2603;

    private String serviceName;
    private Host serviceHost;

    public ServiceReply(String serviceName, Host serviceHost) {
        super(REPLY_ID);
        this.serviceHost = serviceHost;
        this.serviceName = serviceName;
    }

    public Host getServiceHost() {
        return serviceHost;
    }

    public String getServiceName() {
        return serviceName;
    }
}
