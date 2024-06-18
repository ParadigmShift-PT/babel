package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.messages.ParameterMessage;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.timers.SearchTimer;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

public class DNSSelfConfigurationProtocol extends SelfConfigurationProtocol {

    public static final String PAR_DNS_LOOKUP_SERVER = "dns.lookup.server";
    public static final String PAR_DNS_LOOKUP_HOSTNAME = "dns.lookup.hostname";

    protected final Map<String, Map<String, Parameter>> protocolToParameterToConfigure;
    protected final Map<String, SelfConfigurableProtocol> protocolMap;
    protected final Map<Host, ParameterMessage> msgToSend;

    public DNSSelfConfigurationProtocol(String name, short id) {
        super(name, id);

        protocolToParameterToConfigure = new ConcurrentHashMap<>();
        protocolMap = new ConcurrentHashMap<>();
        msgToSend = new ConcurrentHashMap<>();
    }

    @Override
    public void addProtocolParameterToConfigure(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addProtocolParameterToConfigure'");
    }

    @Override
    public void addProtocolParameterConfigured(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addProtocolParameterConfigured'");
    }

    @Override
    public void search(SearchTimer timer, long timerId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'search'");
    }

    @Override
    public void uponParameterMessage(ParameterMessage msg, Host from, short sourceProto, int channelId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'uponParameterMessage'");
    }

    @Override
    public Host getMyself() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMyself'");
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'init'");
    }

}
