package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.requests.FoundServiceReply;
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

/**
 * Protocol that copies, checks and sets the parameters of other protocols in
 * the Babel stack.
 */
public class CopySelfConfigurationProtocol extends SelfConfigurationProtocol {
    private static final Logger logger = LogManager.getLogger(CopySelfConfigurationProtocol.class);

    public static final String DEFAULT_PORT = "19349";
    public static final short PROTO_ID = 32000;
    public static final String PROTO_NAME = "BabelCopySelfConfiguration";
    public static final int SEARCH_COOLDOWN = 5000;
    public static final int CONFIRMATION_TIMEOUT = 30000;

    public static final String PAR_SELF_CONFIGURE_CONFIRMATIONS = "babel.selfconfig.confirmations";
    public static final String PAR_SELF_CONFIGURE_INTERFACE = "babel.selfconfig.interface";
    public static final String PAR_SELF_CONFIGURE_ADDRESS = "babel.selfconfig.address";
    public static final String PAR_SELF_CONFIGURE_PORT = "babel.selfconfig.port";

    protected final Map<String, Map<String, MutableTriple<Parameter, CountDownLatch, Pair<Map<String, Integer>, Set<Host>>>>> protocolToParameterToConfigure;
    protected final Map<String, Map<String, Parameter>> protocolToParameterConfigured;
    protected final Map<String, SelfConfigurableProtocol> protocolMap;
    protected final Map<Host, ParameterMessage> msgToSend;
    protected final Set<Host> whisperers;

    private Host myself;

    private int defaultChannelID;
    private int confirmationsNeeded = 1;

    /**
     * Creates a CopySelfConfigurationProtocol and initialises all internal tracking maps.
     */
    public CopySelfConfigurationProtocol() {
        super(PROTO_NAME, PROTO_ID);

        protocolToParameterToConfigure = new ConcurrentHashMap<>();
        protocolToParameterConfigured = new ConcurrentHashMap<>();
        protocolMap = new ConcurrentHashMap<>();
        msgToSend = new HashMap<>();
        whisperers = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        if (!props.containsKey(PAR_SELF_CONFIGURE_INTERFACE) && props.containsKey(Babel.PAR_DEFAULT_INTERFACE))
            props.put(PAR_SELF_CONFIGURE_INTERFACE, props.get(Babel.PAR_DEFAULT_INTERFACE));
        if (!props.containsKey(PAR_SELF_CONFIGURE_ADDRESS) && props.containsKey(Babel.PAR_DEFAULT_ADDRESS))
            props.put(PAR_SELF_CONFIGURE_ADDRESS, props.get(Babel.PAR_DEFAULT_ADDRESS));
        if (!props.containsKey(PAR_SELF_CONFIGURE_PORT))
            props.put(PAR_SELF_CONFIGURE_PORT, DEFAULT_PORT);
        if (props.containsKey(PAR_SELF_CONFIGURE_CONFIRMATIONS))
            confirmationsNeeded = Integer.valueOf(props.getProperty(PAR_SELF_CONFIGURE_CONFIRMATIONS));

        String networkInterface = props.getProperty(PAR_SELF_CONFIGURE_INTERFACE);
        String address = null;
        if (networkInterface == null) {
            address = props.getProperty(PAR_SELF_CONFIGURE_ADDRESS);
        } else {
            address = NetworkingUtilities.getAddress(networkInterface);
        }
        String port = props.getProperty(PAR_SELF_CONFIGURE_PORT, DEFAULT_PORT);

        Properties channelProps = new Properties(2);
        channelProps.setProperty(TCPChannel.ADDRESS_KEY, address);
        channelProps.setProperty(TCPChannel.PORT_KEY, port);
        defaultChannelID = createChannel(TCPChannel.NAME, channelProps);
        myself = new Host(InetAddress.getByName(address), Integer.valueOf(port));

        registerChannelEventHandler(defaultChannelID, InConnectionDown.EVENT_ID, this::uponInConnectionDown);
        registerChannelEventHandler(defaultChannelID, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerChannelEventHandler(defaultChannelID, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
        registerChannelEventHandler(defaultChannelID, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
        registerChannelEventHandler(defaultChannelID, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);

        registerMessageSerializer(defaultChannelID, ParameterMessage.MSG_ID, ParameterMessage.serializer);

        registerMessageHandler(defaultChannelID, ParameterMessage.MSG_ID, this::uponParameterMessage,
                this::uponMessageFailed);

        registerTimerHandler(SearchTimer.TIMER_ID, this::search);

        registerReplyHandler(FoundServiceReply.REPLY_ID, this::uponFoundServiceReply);
    }

    /**
     * Registers a parameter that still needs a value before {@code proto} can start.
     * On the first call, this method activates discovery (or primes the whisperer list from
     * the protocol's existing contact) and schedules the periodic {@link SearchTimer}.
     *
     * @param parameterName the logical name of the parameter to configure
     * @param setter        the reflective setter used to apply the discovered value
     * @param getter        the reflective getter used to read the current value when acting as a peer
     * @param proto         the protocol that owns the parameter
     */
    public void addProtocolParameterToConfigure(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto) {
        if (protocolToParameterToConfigure.isEmpty()) {
            if (!babel.askRunningDiscovery(this, myself, true)) {
                var whisperCandidate = proto.getMyself();
                if (whisperCandidate != null && proto.getContact() != null)
                    whisperers.add(new Host(proto.getContact().getAddress(), Integer.valueOf(DEFAULT_PORT)));
            }
            setupTimer(new SearchTimer(), SEARCH_COOLDOWN);
        }
        Parameter parameter = new Parameter(getter, setter, proto, parameterName);
        var protocolParameters = protocolToParameterToConfigure.get(proto.getProtoName());
        if (protocolParameters == null) {
            protocolParameters = new ConcurrentHashMap<>();
            protocolToParameterToConfigure.put(proto.getProtoName(), protocolParameters);
        }
        protocolParameters.put(parameterName,
                new MutableTriple<>(parameter, null, new ImmutablePair<>(new HashMap<>(), new HashSet<>())));
        protocolMap.put(proto.getProtoName(), proto);
    }

    /**
     * Registers a parameter that already has a value so that this node can share it with peers
     * that are still searching.
     * If {@code proto} already has a contact address, that host is added to the whisperer set
     * so it can be contacted for future searches.
     *
     * @param parameterName the logical name of the configured parameter
     * @param setter        the reflective setter for the parameter
     * @param getter        the reflective getter used to read the value when responding to peers
     * @param proto         the protocol that owns the parameter
     */
    public void addProtocolParameterConfigured(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto) {
        if (proto.getContact() != null) {
            whisperers.add(new Host(proto.getContact().getAddress(), Integer.valueOf(DEFAULT_PORT)));
        }
        Parameter parameter = new Parameter(getter, setter, proto, parameterName);
        Map<String, Parameter> protocolParameter = protocolToParameterConfigured.get(proto.getProtoName());
        if (protocolParameter == null) {
            protocolParameter = new ConcurrentHashMap<>();
            protocolToParameterConfigured.put(proto.getProtoName(), protocolParameter);
        }
        protocolParameter.put(parameterName, parameter);
        protocolMap.put(proto.getProtoName(), proto);
    }

    /**
     * Periodicaly activated. Looks for a suitable configuration in all known hosts
     * 
     * @param timer   the timer
     * @param timerId the timer id
     */
    public void search(SearchTimer timer, long timerId) {
        logger.info("Trying to search");
        for (var protoEntry : protocolToParameterToConfigure.entrySet()) {
            synchronized (msgToSend) {
                for (var host : whisperers) {
                    ParameterMessage msg = msgToSend.get(host);
                    if (msg == null) {
                        msg = new ParameterMessage();
                        msgToSend.put(host, msg);
                        logger.info("Opening connection to " + host);
                        openConnection(host, defaultChannelID);
                    }
                    for (var paramEntry : protoEntry.getValue().entrySet()) {
                        msg.addAskingParameter(protoEntry.getKey(), paramEntry.getKey());
                    }
                }
            }
        }
        if (!protocolToParameterToConfigure.isEmpty()) {
            setupTimer(timer, SEARCH_COOLDOWN);
        }
    }

    /**
     * Waits for all the confirmations for a single parameter. Starts the protocol
     * if everything is in place.
     * 
     * @param proto            the protocol
     * @param paramToConfigure triple with the parameter register, a countdown
     *                         latch, and a map with all the confirmations and where
     *                         they came from
     */
    private void countdownParameter(SelfConfigurableProtocol proto,
            Triple<Parameter, CountDownLatch, Pair<Map<String, Integer>, Set<Host>>> paramToConfigure) {
        try {
            paramToConfigure.getMiddle().await(CONFIRMATION_TIMEOUT, TimeUnit.MILLISECONDS);
            String confirmedValue = paramToConfigure.getRight().getLeft().entrySet().stream()
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
            paramToConfigure.getLeft().setter().invoke(proto, confirmedValue);
            synchronized (proto) {
                if (proto.readyToStart() && !proto.hasProtocolThreadStarted()) {
                    babel.checkAndStartDcProto(proto);
                    protocolToParameterToConfigure.remove(proto.getProtoName());
                    Parameter parameter = paramToConfigure.getLeft();
                    addProtocolParameterConfigured(parameter.name(), parameter.setter(), parameter.getter(), proto);
                    if (protocolToParameterToConfigure.isEmpty()) {
                        babel.askRunningDiscovery(this, myself, false);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(
                    "Interrupted while waiting for confirmation for " + paramToConfigure.getLeft().getter().getName());
        }
    }

    private void addFoundConfiguration(String config,
            Triple<Parameter, CountDownLatch, Pair<Map<String, Integer>, Set<Host>>> paramToConfigure, Host from) {
        if (paramToConfigure.getRight().getRight().add(from)) {
            var possibilities = paramToConfigure.getRight().getLeft();
            Integer confirmations = possibilities.get(config);
            possibilities.put(config, confirmations == null ? 1 : confirmations.intValue() + 1);
            paramToConfigure.getMiddle().countDown();
        }
    }

    /**
     * Handles an incoming {@link ParameterMessage} from a peer.
     * For each parameter the peer is asking about, supplies the locally configured value (if known).
     * For each parameter the peer is providing, records the value as a candidate and begins the
     * confirmation countdown if this is the first reply received for that parameter.
     *
     * @param msg        the received parameter message (may contain both queries and answers)
     * @param from       the host that sent the message
     * @param sourceProto the source protocol ID
     * @param channelId  the channel on which the message arrived
     */
    public void uponParameterMessage(ParameterMessage msg, Host from, short sourceProto, int channelId) {
        logger.info("Got parameter message from " + from);
        var receivedParams = msg.getAllProtocolParams();
        ParameterMessage replyMsg = new ParameterMessage();
        for (var protoEntry : receivedParams.entrySet()) {
            SelfConfigurableProtocol proto = protocolMap.get(protoEntry.getKey());
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
                    if (paramToConfigure.getMiddle() == null) {
                        paramToConfigure.setMiddle(new CountDownLatch(confirmationsNeeded));
                        new Thread(() -> countdownParameter(proto, paramToConfigure)).start();
                    }
                    addFoundConfiguration(paramEntry.getValue(), paramToConfigure, from);
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

    private void uponFoundServiceReply(FoundServiceReply reply, short sourceProto) {
        whisperers.add(reply.getServiceHost());
    }

    /**
     * Returns the network address of this protocol's TCP self-configuration endpoint.
     *
     * @return the host address and port on which this node listens for parameter exchanges
     */
    public Host getMyself() {
        return myself;
    }
}
