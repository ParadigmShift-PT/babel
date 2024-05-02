package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
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

    private final Map<String, Map<String, Parameter>> protocolToParameterToConfigure;

    public SelfConfigurationProtocol() {
        super(PROTO_NAME, PROTO_ID);

        protocolToParameterToConfigure = new ConcurrentHashMap<>();
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        
    }

    public void addProtocol(String parameterName, Method setter, Method getter, SelfConfiguredProtocol proto) {
        Parameter parameter = new Parameter(getter, setter, proto);
        Map<String, Parameter> protocolParameteres = protocolToParameterToConfigure.get(proto.getProtoName());
        if (protocolParameteres == null) {
            protocolParameteres = new HashMap<>();
            protocolToParameterToConfigure.put(proto.getProtoName(), protocolParameteres);
        }
        protocolParameteres.put(parameterName, parameter);
    }
}
