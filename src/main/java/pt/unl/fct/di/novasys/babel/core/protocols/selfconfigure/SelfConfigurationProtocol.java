package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.SelfConfiguredProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.messages.ParameterAskMessage;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.messages.ParameterResponseMessage;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.network.data.Host;

public class SelfConfigurationProtocol extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(SelfConfigurationProtocol.class);

    public static final String DEFAULT_ADDRESS = "0.0.0.0";
    public static final String DEFAULT_PORT = "19349";
    public static final short PROTO_ID = 604;
    public static final String PROTO_NAME = "BabelSelfConfiguration";

    private final Map<String, Map<String, Parameter>> protocolToParameterToConfigure;
    private final Map<String, Map<String, Parameter>> protocolToParameterConfigured;
    private final Map<String, SelfConfiguredProtocol> protocolMap;

    private int defaultChannelID;

    public SelfConfigurationProtocol() {
        super(PROTO_NAME, PROTO_ID);

        protocolToParameterToConfigure = new ConcurrentHashMap<>();
        protocolToParameterConfigured = new ConcurrentHashMap<>();
        protocolMap = new ConcurrentHashMap<>();
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        String address = props.getProperty("BabelSelfConfiguration.Channel.Adress", DEFAULT_ADDRESS);
        String port = props.getProperty("BabelSelfConfiguration.Channel.Port", DEFAULT_PORT);
        Properties channelProps = new Properties(2);
        channelProps.setProperty(TCPChannel.ADDRESS_KEY, address);
        channelProps.setProperty(TCPChannel.PORT_KEY, port);
        defaultChannelID = createChannel(TCPChannel.NAME, props);

        registerMessageSerializer(defaultChannelID, ParameterResponseMessage.MSG_ID, ParameterResponseMessage.serializer);
    }

    public void addProtocolParameterToConfigure(String parameterName, Method setter, Method getter, SelfConfiguredProtocol proto) {
        Parameter parameter = new Parameter(getter, setter, proto);
        Map<String, Parameter> protocolParameters = protocolToParameterToConfigure.get(proto.getProtoName());
        if (protocolParameters == null) {
            protocolParameters = new ConcurrentHashMap<>();
            protocolToParameterToConfigure.put(proto.getProtoName(), protocolParameters);
        }
        protocolParameters.put(parameterName, parameter);
        protocolMap.put(proto.getProtoName(), proto);
    }

    public void addProtocolParameterConfigured(String parameterName, Method setter, Method getter, SelfConfiguredProtocol proto) {
        Parameter parameter = new Parameter(getter, setter, proto);
        Map<String, Parameter> protocolParameter = protocolToParameterConfigured.get(proto.getProtoName());
        if (protocolParameter == null) {
            protocolParameter = new ConcurrentHashMap<>();
            protocolToParameterConfigured.put(proto.getProtoName(), protocolParameter);
        }
        protocolParameter.put(parameterName, parameter);
        protocolMap.put(proto.getProtoName(), proto);
    }

    public void uponParameterAsk(ParameterAskMessage msg, Host from, short sourceProto, int channelId) {
        
    }

    public void uponParameterResponse(ParameterResponseMessage msg, Host from, short sourceProto, int channelId) {

    }
}
