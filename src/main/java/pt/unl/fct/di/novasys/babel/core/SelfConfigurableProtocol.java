package pt.unl.fct.di.novasys.babel.core;

import pt.unl.fct.di.novasys.network.data.Host;

public abstract class SelfConfigurableProtocol extends DiscoverableProtocol {

    private Host whispererContact;

    public SelfConfigurableProtocol(String protoName, short protoId, Host myself) {
        super(protoName, protoId, myself);
    }

    public void setWhispererContact(Host whisperer) {
        this.whispererContact = whisperer;
    }

    public Host getWhisperer() {
        return this.whispererContact;
    }
}
