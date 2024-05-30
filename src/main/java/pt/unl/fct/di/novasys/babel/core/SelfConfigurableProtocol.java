package pt.unl.fct.di.novasys.babel.core;

import pt.unl.fct.di.novasys.network.data.Host;

public abstract class SelfConfigurableProtocol extends DiscoverableProtocol {
    public static final String DEFAULT_PORT = "19349";

    private Host whispererContact;

    public SelfConfigurableProtocol(String protoName, short protoId, Host myself) {
        super(protoName, protoId, myself);
    }
    
    public SelfConfigurableProtocol(String protoName, short protoId) {
        super(protoName, protoId);
    }

    public void setWhispererContact(Host whisperer) {
        this.whispererContact = whisperer;
    }

    public Host getWhisperer() {
        return this.whispererContact;
    }
}
