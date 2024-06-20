package pt.unl.fct.di.novasys.babel.core.protocols.discovery.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.network.data.Host;

public class FoundServiceReply extends ProtoReply {

    public final static short REPLY_ID = 32302;
    private final Host serviceHost;

    public FoundServiceReply(Host serviceHost) {
        super(REPLY_ID);
        this.serviceHost = serviceHost;
    }

    public Host getServiceHost() {
        return serviceHost;
    }
    
}
