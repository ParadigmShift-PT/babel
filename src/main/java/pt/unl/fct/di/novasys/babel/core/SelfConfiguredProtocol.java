package pt.unl.fct.di.novasys.babel.core;

import java.io.IOException;
import java.util.Properties;

import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

public abstract class SelfConfiguredProtocol extends GenericProtocol {

    private boolean started;
    private Properties props;
    private Host contact;
    private Host myself;

    public SelfConfiguredProtocol(String protoName, short protoId) {
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

    @Override
    public final void init(Properties props) throws HandlerRegistrationException, IOException {
        this.props = props;
    }

    /**
     * Starts the protocol. Should be implemented to start comunication once all the
     * parameters have been definied.
     * 
     * Do not evoke directly.
     */
    protected abstract void start();

    protected abstract boolean readyToStart();

    public void setContact(Host host) {
        this.contact = host;
    }

    public Host getContact() {
        return this.contact;
    }

    public Host getMyself() {
        return this.myself;
    }
}
