package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.messages.ParameterMessage;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.timers.SearchTimer;
import pt.unl.fct.di.novasys.babel.internal.InternalEvent;
import pt.unl.fct.di.novasys.network.data.Host;

public abstract class SelfConfigurationProtocol extends GenericProtocol {

    public SelfConfigurationProtocol(String name, short id) {
        super(name, id);
    }

    public SelfConfigurationProtocol(String name, short id, BlockingQueue<InternalEvent> policy) {
        super(name, id, policy);
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
     * Periodicaly activated. Looks for a suitable configuration in all known hosts
     * 
     * @param timer the timer
     * @param timerId the timer id
     */
    public abstract void search(SearchTimer timer, long timerId);

    public abstract void uponParameterMessage(ParameterMessage msg, Host from, short sourceProto, int channelId);

    public abstract Host getMyself();
}
