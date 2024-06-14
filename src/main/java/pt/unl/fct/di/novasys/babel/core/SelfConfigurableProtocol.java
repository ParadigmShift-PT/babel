package pt.unl.fct.di.novasys.babel.core;

import pt.unl.fct.di.novasys.network.data.Host;

public abstract class SelfConfigurableProtocol extends DiscoverableProtocol {

    public SelfConfigurableProtocol(String protoName, short protoId, Host myself) {
        super(protoName, protoId, myself);
    }
    
    public SelfConfigurableProtocol(String protoName, short protoId) {
        super(protoName, protoId);
    }
}
