package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;
import pt.unl.fct.di.novasys.babel.internal.InternalEvent;
import pt.unl.fct.di.novasys.network.data.Host;

public abstract class SelfConfigurationProtocol extends GenericProtocol {


    public SelfConfigurationProtocol(String protoName, short protoId) {
        super(protoName, protoId);
    }

    public SelfConfigurationProtocol(String protoName, short protoId, BlockingQueue<InternalEvent> policy) {
        super(protoName, protoId, policy);
    }

    /**
     * Adds a parameter to be configured for the proto
     * 
     * @param parameterName parameter's name
     * @param setter        setter for the parameter
     * @param getter        getter for the parameter
     * @param proto         the protocol that has the parameter
     */
    public abstract void addProtocolParameterToConfigure(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto);

    /**
     * Adds a parameter already configured in proto
     * 
     * @param parameterName parameter's name
     * @param setter        setter for the parameter
     * @param getter        getter for the parameter
     * @param proto         the protocol that has the parameter
     */
    public abstract void addProtocolParameterConfigured(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto);

    /**
     * Returns the host that represents the protocol
     * 
     * @return the host that represents the protocol
     */
    public abstract Host getMyself();
}
