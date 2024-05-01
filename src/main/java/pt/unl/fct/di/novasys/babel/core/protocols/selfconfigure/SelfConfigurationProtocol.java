package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.SelfConfiguredProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;

public class SelfConfigurationProtocol extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(SelfConfigurationProtocol.class);

    public static final int DEFAULT_PORT = 19349;
    public static final short PROTO_ID = 604;
    public static final String PROTO_NAME = "BabelSelfConfiguration";

    private final Map<String, List<Parameter>> protocolToParameterToConfigure;

    public SelfConfigurationProtocol() {
        super(PROTO_NAME, PROTO_ID);

        protocolToParameterToConfigure = new ConcurrentHashMap<>();
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        
    }

    public void addProtocol(String parameterName, Method setter, Method getter, SelfConfiguredProtocol proto) {
        Parameter parameter = new Parameter(proto.getProtoName(), getter, setter, proto);
    }
}
