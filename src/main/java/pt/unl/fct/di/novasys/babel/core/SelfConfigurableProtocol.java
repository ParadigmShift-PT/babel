package pt.unl.fct.di.novasys.babel.core;

import java.util.Properties;

import pt.unl.fct.di.novasys.network.data.Host;

public abstract class SelfConfigurableProtocol extends GenericProtocol {

    private boolean started;
    private Properties props;
    private Host contact;
    private Host myself;
    private Host whispererContact;

    public SelfConfigurableProtocol(String protoName, short protoId) {
        super(protoName, protoId);
    }

    /**
     * Sets the protocol to start.
     * 
     * Do not evoke directly.
     */
    void setToStart() {
        started = true;
    }

    /**
     * Starts the protocol. Should be implemented to start comunication once all the
     * parameters have been definied.
     * 
     * Do not evoke directly.
     */
    protected abstract void start();

    public abstract boolean readyToStart();

    public void setContact(Host host) {
        this.contact = host;
    }

    public Host getContact() {
        return this.contact;
    }

    public void setMyself(Host host) {
        this.myself = host;
    }

    public Host getMyself() {
        return this.myself;
    }

    public void setWhispererContact(Host whisperer) {
        this.whispererContact = whisperer;
    }

    public Host getWhisperer() {
        return this.whispererContact;
    }
}
