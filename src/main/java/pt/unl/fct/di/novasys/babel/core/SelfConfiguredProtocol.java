package pt.unl.fct.di.novasys.babel.core;

import java.io.IOException;
import java.util.Properties;

import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;

public abstract class SelfConfiguredProtocol extends GenericProtocol {

    private boolean started;
    private Properties props;

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
     * 
     */
    protected abstract void start();
}
