package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.messages.ParameterMessage;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.timers.SearchTimer;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.utils.NetworkingUtilities;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionDown;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionUp;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionDown;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionFailed;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionUp;
import pt.unl.fct.di.novasys.network.data.Host;

public class CopySelfConfigurationProtocol extends SelfConfigurationProtocol {
    private static final Logger logger = LogManager.getLogger(CopySelfConfigurationProtocol.class);

    public static final short PROTO_ID = 32000;
    public static final String PROTO_NAME = "BabelCopySelfConfiguration";
    public static final int SEARCH_COOLDOWN = 5000;

    protected final Map<String, Map<String, Pair<Parameter, Map<String, Integer>>>> protocolToParameterToConfigure;
    protected final Map<String, Map<String, Parameter>> protocolToParameterConfigured;
    protected final Map<String, SelfConfigurableProtocol> protocolMap;
    protected final Map<Host, ParameterMessage> msgToSend;

    private int defaultChannelID;
    private int confirmationsNeeded = 1;

    public CopySelfConfigurationProtocol() {
        super(PROTO_NAME, PROTO_ID);

        protocolToParameterToConfigure = new ConcurrentHashMap<>();
        protocolToParameterConfigured = new ConcurrentHashMap<>();
        protocolMap = new ConcurrentHashMap<>();
        msgToSend = new HashMap<>();
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        String networkInterface = props.getProperty("BabelWhisperer.Unicast.Interface");
        String address = null;
        if (networkInterface == null) {
            address = props.getProperty("BabelWhisperer.Unicast.Address");
            if (address == null) {
                address = NetworkingUtilities.getAddress("eth0");
            }
        } else {
            address = NetworkingUtilities.getAddress(networkInterface);
        }
        String port = props.getProperty("BabelWhisperer.Unicast.Port", SelfConfigurableProtocol.DEFAULT_PORT);
        String confirmations = props.getProperty("BabelWhisperer.Confirmations");
        if (confirmations != null) {
            confirmationsNeeded = Integer.valueOf(confirmations);
        }
        Properties channelProps = new Properties(2);
        channelProps.setProperty(TCPChannel.ADDRESS_KEY, address);
        channelProps.setProperty(TCPChannel.PORT_KEY, port);
        defaultChannelID = createChannel(TCPChannel.NAME, channelProps);

        registerChannelEventHandler(defaultChannelID, InConnectionDown.EVENT_ID, this::uponInConnectionDown);
        registerChannelEventHandler(defaultChannelID, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerChannelEventHandler(defaultChannelID, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
        registerChannelEventHandler(defaultChannelID, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
        registerChannelEventHandler(defaultChannelID, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);

        registerMessageSerializer(defaultChannelID, ParameterMessage.MSG_ID, ParameterMessage.serializer);

        registerMessageHandler(defaultChannelID, ParameterMessage.MSG_ID, this::uponParameterMessage,
                this::uponMessageFailed);

        registerTimerHandler(SearchTimer.TIMER_ID, this::search);

        setupPeriodicTimer(new SearchTimer(), SEARCH_COOLDOWN, SEARCH_COOLDOWN);
    }

    public void addProtocolParameterToConfigure(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto) {
        Parameter parameter = new Parameter(getter, setter, proto);
        var protocolParameters = protocolToParameterToConfigure.get(proto.getProtoName());
        if (protocolParameters == null) {
            protocolParameters = new ConcurrentHashMap<>();
            protocolToParameterToConfigure.put(proto.getProtoName(), protocolParameters);
        }
        protocolParameters.put(parameterName, new ImmutablePair<>(parameter, new HashMap<>()));
        protocolMap.put(proto.getProtoName(), proto);
    }

    public void addProtocolParameterConfigured(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto) {
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
        logger.info("Trying to search");
        for (var protoEntry : protocolToParameterToConfigure.entrySet()) {
            Host protoHost = protocolMap.get(protoEntry.getKey()).getWhisperer();
            synchronized (msgToSend) {
                ParameterMessage msg = msgToSend.get(protoHost);
                if (msg == null) {
                    msg = new ParameterMessage();
                    msgToSend.put(protoHost, msg);
                    logger.info("Opening connection to " + protoHost);
                    openConnection(protoHost, defaultChannelID);
                }
                for (var paramEntry : protoEntry.getValue().entrySet()) {
                    msg.addAskingParameter(protoEntry.getKey(), paramEntry.getKey());
                }
            }
        }
    }

    public void uponParameterMessage(ParameterMessage msg, Host from, short sourceProto, int channelId) {
        logger.info("Got parameter message from " + from);
        var receivedParams = msg.getAllProtocolParams();
        ParameterMessage replyMsg = new ParameterMessage();
        for (var protoEntry : receivedParams.entrySet()) {
            SelfConfigurableProtocol proto = protocolMap.get(protoEntry.getKey());
            boolean wasNotReady = proto.readyToStart();
            var thisProtocolToConfigure = protocolToParameterToConfigure.get(protoEntry.getKey());
            var thisProtocolConfigured = protocolToParameterConfigured.get(protoEntry.getKey());
            for (var paramEntry : protoEntry.getValue().entrySet()) {
                var paramToConfigure = thisProtocolToConfigure != null
                        ? thisProtocolToConfigure.get(paramEntry.getKey())
                        : null;
                var paramConfigured = thisProtocolConfigured != null
                        ? thisProtocolConfigured.get(paramEntry.getKey())
                        : null;
                if (paramEntry.getValue() != null && paramToConfigure != null) {
                    var possibilities = paramToConfigure.getRight();
                    int confirmations = possibilities.get(paramEntry.getValue());
                    possibilities.put(paramEntry.getValue(), confirmations + 1);
                    String confirmedValue = possibilities.entrySet().stream()
                            .max(new Comparator<Entry<String, Integer>>() {
                                @Override
                                public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                                    int compare = o1.getValue().compareTo(o2.getValue());
                                    if (compare == 0) {
                                        return o1.getKey().compareTo(o2.getKey());
                                    }
                                    return compare;
                                }

                            }).get().getKey();
                    try {
                        paramToConfigure.getLeft().setter().invoke(proto, confirmedValue);
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
            if (!wasNotReady && proto.readyToStart()) {
                babel.setupSelfConfiguration(proto);
            }
        }
        synchronized (msgToSend) {
            ParameterMessage oldMsg = msgToSend.get(from);
            if (oldMsg != null) {
                oldMsg.join(replyMsg);
            } else {
                msgToSend.put(from, replyMsg);
            }
            if (oldMsg != null && oldMsg.getAllProtocolParams().size() > 0
                    || replyMsg.getAllProtocolParams().size() > 0) {
                openConnection(from);
            }
        }
    }

    /**
     * Handle the case when a message fails to be (confirmed to be) delivered to the
     * destination
     * Print the error
     * 
     * @param msg       the message that failed delivery
     * @param host      the destination host
     * @param destProto the destination protocol ID
     * @param error     the error that caused the failure
     * @param channelId the channel ID (from which channel was the message was sent)
     */
    private void uponMessageFailed(ProtoMessage msg, Host host, short destProto, Throwable error, int channelId) {
        logger.warn("Failed message: {} to host: {} with error: {}", msg, host, error.getMessage());
    }

    /**
     * Handle the case when someone opened a connection to this node
     * Print the event
     * 
     * @param event   the event containing the connection information
     * @param channel the channel ID (from which channel the event was received)
     */
    private void uponInConnectionUp(InConnectionUp event, int channel) {
        logger.debug(event);
    }

    /**
     * Handle the case when someone closed a connection to this node
     * Print the event
     * 
     * @param event   the event containing the connection information
     * @param channel the channel ID (from which channel the event was received)
     */
    private void uponInConnectionDown(InConnectionDown event, int channel) {
        logger.info(event);
    }

    /**
     * Handle the case when a connection to a remote node went down or was closed
     * Print the event
     * 
     * @param event   the event containing the connection information
     * @param channel the channel ID (from which channel the event was received)
     */
    private void uponOutConnectionDown(OutConnectionDown event, int channel) {
        logger.warn(event);
    }

    /**
     * Handle when an open connection operation succeeded
     * Start the periodic timer to send Ping pingpong.messages
     * 
     * @param event   OutConnectionUp event
     * @param channel Channel ID
     */
    private void uponOutConnectionUp(OutConnectionUp event, int channel) {
        logger.info("Connection to {} is now up", event.getNode());

        synchronized (msgToSend) {
            sendMessage(msgToSend.remove(event.getNode()), event.getNode());
            closeConnection(event.getNode(), defaultChannelID);
        }
    }

    /**
     * Handle when an open connection operation has failed
     * Print error message and exit
     * 
     * @param event   OutConnectionFailed event
     * @param channel Channel ID
     */
    private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channel) {
        logger.debug(event);
    }
}
