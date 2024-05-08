package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.SelfConfiguredProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.messages.ParameterMessage;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.timers.SearchTimer;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.network.data.Host;

public class SelfConfigurationProtocol extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(SelfConfigurationProtocol.class);

    public static final String DEFAULT_ADDRESS = "0.0.0.0";
    public static final String DEFAULT_PORT = "19349";
    public static final short PROTO_ID = 604;
    public static final String PROTO_NAME = "BabelSelfConfiguration";
    public static final int SEARCH_COOLDOWN = 5000;

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

        registerMessageSerializer(defaultChannelID, ParameterMessage.MSG_ID, ParameterMessage.serializer);

        registerMessageHandler(defaultChannelID, ParameterMessage.MSG_ID, this::uponParameterMessage);
        
        registerTimerHandler(SearchTimer.TIMER_ID, this::search);

        setupPeriodicTimer(new SearchTimer(), SEARCH_COOLDOWN, SEARCH_COOLDOWN);
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

    public void search(SearchTimer timer, long timerId) {
        Map<Host, ParameterMessage> messages = new HashMap<>();
        for (var protoEntry : protocolToParameterToConfigure.entrySet()) {
            Host protoHost = protocolMap.get(protoEntry.getKey()).getContact();
            ParameterMessage msg = messages.get(protoHost);
            if (msg == null) {
                msg = new ParameterMessage();
                messages.put(protoHost, msg);
            }
            for (var paramEntry : protoEntry.getValue().entrySet()) {
                msg.addAskingParameter(protoEntry.getKey(), paramEntry.getKey());
            }
        }

        for (var msg : messages.entrySet()) {
            sendMessage(msg.getValue(), msg.getKey());
        }
    }

    public void uponParameterMessage(ParameterMessage msg, Host from, short sourceProto, int channelId) {
        var receivedParams = msg.getAllProtocolParams();
        ParameterMessage replyMsg = new ParameterMessage();
        for (var protoEntry : receivedParams.entrySet()) {
            SelfConfiguredProtocol proto = protocolMap.get(protoEntry.getKey());
            Map<String, Parameter> thisProtocolToConfigured = protocolToParameterToConfigure.get(protoEntry.getKey());
            Map<String, Parameter> thisProtocolConfigured = protocolToParameterConfigured.get(protoEntry.getKey());
            for (var paramEntry : protoEntry.getValue().entrySet()) {
                Parameter paramToConfigure = thisProtocolToConfigured.get(paramEntry.getKey());
                Parameter paramConfigured = thisProtocolConfigured.get(paramEntry.getKey());
                if (paramEntry.getValue() != null && paramToConfigure != null) {
                    try {
                        paramToConfigure.setter().invoke(proto, paramEntry.getValue());
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Protocol badly constructed");
                    }
                } else if (paramEntry.getValue() == null && paramConfigured != null) {
                    try {
                        String value = (String) paramConfigured.getter().invoke(proto);
                        replyMsg.addParameter(protoEntry.getKey(), paramEntry.getKey(), value);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Protocol badly constructed");
                    }
                }
            }
        }
        if (replyMsg.getAllProtocolParams().size() > 0) {
            sendMessage(msg, from);
        }
    }
}
