package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.messages.ParameterMessage;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.timers.SearchTimer;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

public class CheckCopySelfConfigurationProtocol extends SelfConfigurationProtocol {
    private static final Logger logger = LogManager.getLogger(CheckCopySelfConfigurationProtocol.class);

    public static final short PROTO_ID = 32001;
    public static final String PROTO_NAME = "BabelCheckCopySelfConfiguration";
    public static final int SEARCH_COOLDOWN = 5000;


    public CheckCopySelfConfigurationProtocol(String name, short id) {
        super(PROTO_NAME, PROTO_ID);
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
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'init'");
    }

    @Override
    public void uponNewWhisperer() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'uponNewWhisperer'");
    }
}
