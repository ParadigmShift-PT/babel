package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.lang.reflect.Method;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.messages.ParameterMessage;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.timers.SearchTimer;
import pt.unl.fct.di.novasys.network.data.Host;

public abstract class SelfConfigurationProtocol extends GenericProtocol {

    public SelfConfigurationProtocol(String name, short id) {
        super(name, id);
    }

    public abstract void addProtocolParameterToConfigure(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto);

    public abstract void addProtocolParameterConfigured(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto);

    public abstract void search(SearchTimer timer, long timerId) ;

    public abstract void uponParameterMessage(ParameterMessage msg, Host from, short sourceProto, int channelId);

}
