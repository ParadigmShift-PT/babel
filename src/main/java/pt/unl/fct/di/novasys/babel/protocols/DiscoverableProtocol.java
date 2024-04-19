package pt.unl.fct.di.novasys.babel.protocols;

import pt.unl.fct.di.novasys.babel.protocols.discovery.requests.ServiceReply;

public interface DiscoverableProtocol {

    void uponContactReceived(ServiceReply reply, short sourceProto);
    
}
