package pt.unl.fct.di.novasys.babel.core;

import pt.unl.fct.di.novasys.babel.core.security.IdentityCrypt;
import pt.unl.fct.di.novasys.babel.core.security.IdentityPair;
import pt.unl.fct.di.novasys.babel.core.security.SecretCrypt;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.NoSuchProtocolException;
import pt.unl.fct.di.novasys.babel.handlers.*;
import pt.unl.fct.di.novasys.babel.internal.*;
import pt.unl.fct.di.novasys.babel.internal.security.PeerIdEncoder;
import pt.unl.fct.di.novasys.babel.metrics.Metric;
import pt.unl.fct.di.novasys.babel.metrics.MetricsManager;
import pt.unl.fct.di.novasys.babel.generic.*;
import pt.unl.fct.di.novasys.channel.ChannelEvent;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.security.spec.InvalidKeySpecException;
import java.security.KeyStore.SecretKeyEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.net.ssl.KeyManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.NoSuchProtocolException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.babel.internal.CustomChannelEvent;
import pt.unl.fct.di.novasys.babel.internal.IPCEvent;
import pt.unl.fct.di.novasys.babel.internal.InternalEvent;
import pt.unl.fct.di.novasys.babel.internal.MessageFailedEvent;
import pt.unl.fct.di.novasys.babel.internal.MessageInEvent;
import pt.unl.fct.di.novasys.babel.internal.MessageSentEvent;
import pt.unl.fct.di.novasys.babel.internal.NotificationEvent;
import pt.unl.fct.di.novasys.babel.internal.TimerEvent;
import pt.unl.fct.di.novasys.babel.metrics.Counter;
import pt.unl.fct.di.novasys.babel.metrics.Metric;
import pt.unl.fct.di.novasys.babel.metrics.MetricsManager;
import pt.unl.fct.di.novasys.channel.ChannelEvent;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * An abstract class that represent a generic protocol
 * <p>
 * This class handles all interactions required by protocols
 * <p>
 * Users should extend this class to implement their protocols
 */
@SuppressWarnings({"unused"})
public abstract class GenericProtocol {

    private static final Logger logger = LogManager.getLogger(GenericProtocol.class);

    //TODO split in GenericConnectionlessProtocol and GenericConnectionProtocol?

    private final BlockingQueue<InternalEvent> queue;
    private final Thread executionThread;
    private final String protoName;
    private final short protoId;

    private boolean protocolThreadedStarted;
    
    private int defaultChannel;

    // TODO if some other protocol removes this identity/secret, this variable
    // will not be updated. Should it?
    private IdentityCrypt defaultIdentityCrypt;
    private IdentityPair defaultIdentity;
    private SecretCrypt defaultSecret;

    private final Map<Integer, ChannelHandlers> channels;
    private final Map<Short, TimerHandler<? extends ProtoTimer>> timerHandlers;
    private final Map<Short, RequestHandler<? extends ProtoRequest>> requestHandlers;
    private final Map<Short, ReplyHandler<? extends ProtoReply>> replyHandlers;
    private final Map<Short, NotificationHandler<? extends ProtoNotification>> notificationHandlers;

    public static final BabelRuntime babel = BabelRuntime.getInstance();
    public static final BabelSecurity babelSecurity = BabelSecurity.getInstance();

    private final ProtocolMetricsBabelMetrics metrics_babel;

    //Debug
    //protected ThreadMXBean tmx = ManagementFactory.getThreadMXBean();

    /**
     * Creates a generic protocol with the provided name and numeric identifier
     * and the given event queue policy.
     * <p>
     * Event queue policies can be defined to specify handling events in desired orders:
     * Eg. If multiple events are inside the queue, then timers are always processes first
     * than any other event in the queue.
     *
     * @param protoName the protocol name
     * @param protoId   the protocol numeric identifier
     * @param policy    the queue policy to use
     */
    public GenericProtocol(String protoName, short protoId, BlockingQueue<InternalEvent> policy) {
        this.queue = policy;
        this.protoId = protoId;
        this.protoName = protoName;
        
        this.protocolThreadedStarted = false;

        //TODO change to event loop (simplifies the deliver->poll->handle process)
        //TODO only change if performance better
        this.executionThread = new Thread(this::mainLoop, protoId + "-" + protoName);
        channels = new HashMap<>();
        defaultChannel = -1;

        //Initialize maps for event handlers
        this.timerHandlers = new HashMap<>();
        this.requestHandlers = new HashMap<>();
        this.replyHandlers = new HashMap<>();
        this.notificationHandlers = new HashMap<>();

        //Initialize generic protocol metrics, but don't register them by default
        this.metrics_babel = new ProtocolMetricsBabelMetrics(protoId, protoName);

        //tmx.setThreadContentionMonitoringEnabled(true);
    }

    /**
     * Provides information if the protocol thread of this protocol was already stated in the past.
     * @return true if the protocol thread was started in the past
     */
    public final boolean hasProtocolThreadStarted() {
    	return this.protocolThreadedStarted;
    }
    
    /**
     * Create a generic protocol with the provided name and numeric identifier
     * and network service
     * <p>
     * The internal event queue is defined to have a FIFO policy on all events
     *
     * @param protoName name of the protocol
     * @param protoId   numeric identifier
     */
    public GenericProtocol(String protoName, short protoId) {
        this(protoName, protoId, new LinkedBlockingQueue<>());
    }

    /**
     * Returns the numeric identifier of the protocol
     *
     * @return numeric identifier
     */
    public final short getProtoId() {
        return protoId;
    }

    /**
     * Returns the name of the protocol
     *
     * @return name
     */
    public final String getProtoName() {
        return protoName;
    }

    /**
     * Start the event thread of the protocol
     */
    public final void startEventThread() {
    	if(this.protocolThreadedStarted)
    		return;
    	
        try {
            this.executionThread.start();
            this.protocolThreadedStarted = true;
        } catch (IllegalThreadStateException e) {

        }
    }

    /**
     * Initializes the protocol with the given properties
     * 
     * @param props properties
     */
    public abstract void init(Properties props) throws HandlerRegistrationException, IOException;

    protected long getMillisSinceBabelStart(){
        return babel.getMillisSinceStart();
    }

    /* ------------------ PROTOCOL REGISTERS -------------------------------------------------*/

    private <V> void registerHandler(short id, V handler, Map<Short, V> handlerMap)
            throws HandlerRegistrationException {
        if (handlerMap.putIfAbsent(id, handler) != null) {
            throw new HandlerRegistrationException("Conflict in registering handler for "
                    + handler.getClass().toString() + " with id " + id + ".");
        }
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     *
     * @param cId       the id of the channel
     * @param msgId     the numeric identifier of the message event
     * @param inHandler the function to process message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         MessageInHandler<V> inHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, null, null);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handler's {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId       the id of the channel
     * @param msgId     the numeric identifier of the message event
     * @param inHandler the function to process message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         SecureMessageInHandler<V> inHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, null, null);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param sentHandler the function to handle a sent message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         MessageInHandler<V> inHandler,
                                                                         MessageSentHandler<V> sentHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, sentHandler, null);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handler's {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param sentHandler the function to handle a sent message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         SecureMessageInHandler<V> inHandler,
                                                                         MessageSentHandler<V> sentHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, sentHandler, null);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handler's {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param sentHandler the function to handle a sent message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         MessageInHandler<V> inHandler,
                                                                         SecureMessageSentHandler<V> sentHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, sentHandler, null);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handlers' {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param sentHandler the function to handle a sent message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         SecureMessageInHandler<V> inHandler,
                                                                         SecureMessageSentHandler<V> sentHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, sentHandler, null);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param failHandler the function to handle a failed message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         MessageInHandler<V> inHandler,
                                                                         MessageFailedHandler<V> failHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, null, failHandler);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handler's {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param failHandler the function to handle a failed message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         SecureMessageInHandler<V> inHandler,
                                                                         MessageFailedHandler<V> failHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, null, failHandler);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handler's {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param failHandler the function to handle a failed message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         MessageInHandler<V> inHandler,
                                                                         SecureMessageFailedHandler<V> failHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, null, failHandler);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handlers' {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param failHandler the function to handle a failed message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         SecureMessageInHandler<V> inHandler,
                                                                         SecureMessageFailedHandler<V> failHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, null, failHandler);
    }


    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param sentHandler the function to handle a sent message event
     * @param failHandler the function to handle a failed message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         MessageInHandler<V> inHandler,
                                                                         MessageSentHandler<V> sentHandler,
                                                                         MessageFailedHandler<V> failHandler)
            throws HandlerRegistrationException {
        registerHandler(msgId, inHandler, getChannelOrThrow(cId).messageInHandlers);
        if (sentHandler != null) registerHandler(msgId, sentHandler, getChannelOrThrow(cId).messageSentHandlers);
        if (failHandler != null) registerHandler(msgId, failHandler, getChannelOrThrow(cId).messageFailedHandlers);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handler's {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param sentHandler the function to handle a sent message event
     * @param failHandler the function to handle a failed message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         SecureMessageInHandler<V> inHandler,
                                                                         MessageSentHandler<V> sentHandler,
                                                                         MessageFailedHandler<V> failHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, (MessageInHandler<V>) inHandler, sentHandler, failHandler);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handler's {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param sentHandler the function to handle a sent message event
     * @param failHandler the function to handle a failed message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         MessageInHandler<V> inHandler,
                                                                         SecureMessageSentHandler<V> sentHandler,
                                                                         MessageFailedHandler<V> failHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, (MessageSentHandler<V>) sentHandler, failHandler);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handlers' {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param sentHandler the function to handle a sent message event
     * @param failHandler the function to handle a failed message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         SecureMessageInHandler<V> inHandler,
                                                                         SecureMessageSentHandler<V> sentHandler,
                                                                         MessageFailedHandler<V> failHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, (MessageInHandler<V>) inHandler, (MessageSentHandler<V>) sentHandler, failHandler);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handler's {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param sentHandler the function to handle a sent message event
     * @param failHandler the function to handle a failed message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         MessageInHandler<V> inHandler,
                                                                         MessageSentHandler<V> sentHandler,
                                                                         SecureMessageFailedHandler<V> failHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, sentHandler, (MessageFailedHandler<V>) failHandler);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handlers' {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param sentHandler the function to handle a sent message event
     * @param failHandler the function to handle a failed message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         SecureMessageInHandler<V> inHandler,
                                                                         MessageSentHandler<V> sentHandler,
                                                                         SecureMessageFailedHandler<V> failHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, (MessageInHandler<V>) inHandler, sentHandler, (MessageFailedHandler<V>) failHandler);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handlers' {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param sentHandler the function to handle a sent message event
     * @param failHandler the function to handle a failed message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         MessageInHandler<V> inHandler,
                                                                         SecureMessageSentHandler<V> sentHandler,
                                                                         SecureMessageFailedHandler<V> failHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, inHandler, (MessageSentHandler<V>) sentHandler, (MessageFailedHandler<V>) failHandler);
    }

    /**
     * Register a message inHandler for the protocol to process message events
     * form the network
     * <p>
     * The handlers' {@code peerId} argument will be {@code null} if the message
     * event is triggered by a non-secure channel.
     *
     * @param cId         the id of the channel
     * @param msgId       the numeric identifier of the message event
     * @param inHandler   the function to handle a received message event
     * @param sentHandler the function to handle a sent message event
     * @param failHandler the function to handle a failed message event
     * @throws HandlerRegistrationException if a inHandler for the message id is already registered
     */
    protected final <V extends ProtoMessage> void registerMessageHandler(int cId, short msgId,
                                                                         SecureMessageInHandler<V> inHandler,
                                                                         SecureMessageSentHandler<V> sentHandler,
                                                                         SecureMessageFailedHandler<V> failHandler)
            throws HandlerRegistrationException {
        registerMessageHandler(cId, msgId, (MessageInHandler<V>) inHandler, (MessageSentHandler<V>) sentHandler, (MessageFailedHandler<V>) failHandler);
    }

    /**
     * Register an handler to process a channel-specific event
     *
     * @param cId     the id of the channel
     * @param eventId the id of the event to process
     * @param handler the function to handle the event
     * @throws HandlerRegistrationException if a inHandler for the event id is already registered
     */
    protected final <V extends ChannelEvent> void registerChannelEventHandler(int cId, short eventId,
                                                                              ChannelEventHandler<V> handler)
            throws HandlerRegistrationException {
        registerHandler(eventId, handler, getChannelOrThrow(cId).channelEventHandlers);
    }

    /**
     * Register a timer handler for the protocol to process timer events
     *
     * @param timerID the numeric identifier of the timer event
     * @param handler the function to process timer event
     * @throws HandlerRegistrationException if a handler for the timer timerID is already registered
     */
    protected final <V extends ProtoTimer> void registerTimerHandler(short timerID,
                                                                     TimerHandler<V> handler)
            throws HandlerRegistrationException {
        registerHandler(timerID, handler, timerHandlers);
    }

    /**
     * Register a request handler for the protocol to process request events
     *
     * @param requestId the numeric identifier of the request event
     * @param handler   the function to process request event
     * @throws HandlerRegistrationException if a handler for the request requestId is already registered
     */
    protected final <V extends ProtoRequest> void registerRequestHandler(short requestId,
                                                                         RequestHandler<V> handler)
            throws HandlerRegistrationException {
        registerHandler(requestId, handler, requestHandlers);
    }

    /**
     * Register a reply handler for the protocol to process reply events
     *
     * @param replyId the numeric identifier of the reply event
     * @param handler the function to process reply event
     * @throws HandlerRegistrationException if a handler for the reply replyId is already registered
     */
    protected final <V extends ProtoReply> void registerReplyHandler(short replyId, ReplyHandler<V> handler)
            throws HandlerRegistrationException {
        registerHandler(replyId, handler, replyHandlers);
    }

    /* ------------------------- NETWORK/CHANNELS ---------------------- */

    protected final ChannelHandlers getChannelOrThrow(int channelId) {
        ChannelHandlers handlers = channels.get(channelId);
        if (handlers == null)
            throw new AssertionError("Channel does not exist: " + channelId);
        return handlers;
    }

    /**
     * Registers a (de)serializer for a message type
     *
     * @param msgId      the message id
     * @param serializer the serializer for the given message id
     */
    protected final void registerMessageSerializer(int channelId, short msgId,
                                                   ISerializer<? extends ProtoMessage> serializer) {
        babel.registerSerializer(channelId, msgId, serializer);
    }

    /**
     * Creates a new channel
     *
     * @param channelName the name of the channel
     * @param props       channel-specific properties. See the documentation for each channel.
     * @return the id of the newly created channel
     * @throws IllegalArgumentException if there's no non-secure channel with {@code channelName}.
     */
    protected final int createChannel(String channelName, Properties props) throws IOException {
        int channelId = babel.createChannel(channelName, this.protoId, props);
        registerSharedChannel(channelId);
        return channelId;
    }

    /**
     * Creates a new secure channel that uses all available identities.
     *
     * @param channelName  the name of the channel
     * @param props        channel-specific properties. See the documentation for each channel.
     *
     * @return the id of the newly created channel
     * @throws IllegalArgumentException      if there's no secure channel with {@code channelName}.
     */
    protected final int createSecureChannel(String channelName, Properties props) throws IOException {
        int channelId = babel.createSecureChannel(channelName, this.protoId, props, null, null);
        registerSharedChannel(channelId);
        return channelId;
    }

    /**
     * Creates a new secure channel that will only use the specified identity.
     *
     * @param channelName the name of the channel
     * @param props       channel-specific properties. See the documentation for each channel.
     * @param identity    the identity allowed to be used during communication.
     *
     * @return the id of the newly created channel
     * @throws IllegalArgumentException      if there's no secure channel with {@code channelName}.
     */
    protected final int createSecureChannel(String channelName, Properties props, byte[] identity)
            throws IOException {
        return createSecureChannelWithIdentities(channelName, props, Arrays.asList(identity));
    }

    /**
     * Creates a new secure channel that will only use the identity specified by the given alias.
     *
     * @param channelName   the name of the channel
     * @param props         channel-specific properties. See the documentation for each channel.
     * @param identityAlias the alias of the identity allowed to be used during communication.
     *
     * @return the id of the newly created channel
     * @throws IllegalArgumentException      if there's no secure channel with {@code channelName}.
     */
    protected final int createSecureChannel(String channelName, Properties props, String identityAlias)
            throws IOException {
        return createSecureChannelWithAliases(channelName, props, Arrays.asList(identityAlias));
    }

    /**
     * Creates a new secure channel that will only use the specified identities.
     *
     * @param channelName the name of the channel
     * @param props       channel-specific properties. See the documentation for each channel.
     * @param identities  the identities allowed to be used during communication.
     *
     * @return the id of the newly created channel
     * @throws IllegalArgumentException      if there's no secure channel with {@code channelName}.
     */
    protected final int createSecureChannelWithIdentities(String channelName, Properties props, byte[]... identities)
            throws IOException {
        return createSecureChannelWithIdentities(channelName, props, Arrays.asList(identities));
    }

    /**
     * Creates a new secure channel that will only use the specified identities.
     *
     * @param channelName the name of the channel
     * @param props       channel-specific properties. See the documentation for each channel.
     * @param identities  the identities allowed to be used during communication.
     *
     * @return the id of the newly created channel
     * @throws IllegalArgumentException      if there's no secure channel with {@code channelName}.
     */
    protected final int createSecureChannelWithIdentities(String channelName, Properties props,
            Collection<byte[]> identities) throws IOException {
        int channelId = babel.createSecureChannel(channelName, this.protoId, props, null, identities);
        registerSharedChannel(channelId);
        return channelId;
    }

    /**
     * Creates a new secure channel that will only use the identities with aliases
     * starting with {@code protoName} (this includes any identity added with the
     * {@link GenericProtocol} methods for adding identities without explicit
     * aliases).
     *
     * @param channelName the name of the channel
     * @param props       channel-specific properties. See the documentation for each channel.
     *
     * @return the id of the newly created channel
     * @throws IllegalArgumentException      if there's no secure channel with {@code channelName}.
     */
    protected final int createSecureChannelWithProtoIdentities(String channelName, Properties props)
            throws IOException {
        return createSecureChannelWithAliases(channelName,
                props,
                babelSecurity.getAllIdentitiesWithPrefix(protoName).stream()
                        .map(id -> id.alias())
                        .collect(Collectors.toList()));
    }

    /**
     * Creates a new secure channel that will only use the identities specified by the given aliases.
     *
     * @param channelName     the name of the channel
     * @param props           channel-specific properties. See the documentation for each channel.
     * @param identityAliases the aliases of the identities allowed to be used during communication.
     *
     * @return the id of the newly created channel
     * @throws IllegalArgumentException      if there's no secure channel with {@code channelName}.
     */
    protected final int createSecureChannelWithAliases(String channelName, Properties props, String... identityAliases)
            throws IOException {
        return createSecureChannelWithAliases(channelName, props, Arrays.asList(identityAliases));
    }

    /**
     * Creates a new secure channel that will only use the identities specified by the given aliases.
     *
     * @param channelName     the name of the channel
     * @param props           channel-specific properties. See the documentation for each channel.
     * @param identityAliases the aliases of the identities to be used during communication.
     *
     * @return the id of the newly created channel
     * @throws IllegalArgumentException      if there's no secure channel with {@code channelName}.
     */
    protected final int createSecureChannelWithAliases(String channelName, Properties props, Collection<String> identityAliases)
            throws IOException {
        int channelId = babel.createSecureChannel(channelName, this.protoId, props, identityAliases, null);
        registerSharedChannel(channelId);
        return channelId;
    }

    protected final void registerSharedChannel(int channelId) {
        babel.registerChannelInterest(channelId, this.protoId, this);
        channels.put(channelId, new ChannelHandlers());
        if (defaultChannel == -1)
            setDefaultChannel(channelId);
    }

    /**
     * Sets the default channel for the {@link #sendMessage(ProtoMessage, Host)}, {@link #openConnection(Host)}
     * and {@link #closeConnection(Host)} methods.
     *
     * @param channelId the channel id
     */
    protected final void setDefaultChannel(int channelId) {
        getChannelOrThrow(channelId);
        defaultChannel = channelId;
    }

    /**
     * Returns the default channel for the {@link #sendMessage(ProtoMessage, Host)}, {@link #openConnection(Host)}
     * and {@link #closeConnection(Host)} methods.
     * 
     * @return channel id
     */
    protected final int getDefaultChannel() {
        return defaultChannel;
    }

    /**
     * Sends a message to a specified destination, using the default channel.
     * May require the use of {@link #openConnection(Host)} beforehand.
     *
     * @param msg         the message to send
     * @param destination the ip/port to send the message to
     */
    protected final void sendMessage(ProtoMessage msg, Host destination) {
        sendMessage(defaultChannel, msg, this.protoId, destination, 0);
    }

    /**
     * Sends a message to a specified destination using the given channel.
     * May require the use of {@link #openConnection(Host)} beforehand.
     *
     * @param channelId   the channel to send the message through
     * @param msg         the message to send
     * @param destination the ip/port to send the message to
     */
    protected final void sendMessage(int channelId, ProtoMessage msg, Host destination) {
        sendMessage(channelId, msg, this.protoId, destination, 0);
    }

    /**
     * Sends a message to a different protocol in the specified destination, using the default channel.
     * May require the use of {@link #openConnection(Host)} beforehand.
     *
     * @param destProto   the target protocol for the message.
     * @param msg         the message to send
     * @param destination the ip/port to send the message to
     */
    protected final void sendMessage(ProtoMessage msg, short destProto, Host destination) {
        sendMessage(defaultChannel, msg, destProto, destination, 0);
    }

    /**
     * Sends a message to a specified destination, using the default channel, and a specific connection in that channel.
     * May require the use of {@link #openConnection(Host)} beforehand.
     *
     * @param connection  the channel-specific connection to use.
     * @param msg         the message to send
     * @param destination the ip/port to send the message to
     */
    protected final void sendMessage(ProtoMessage msg, Host destination, int connection) {
        sendMessage(defaultChannel, msg, this.protoId, destination, connection);
    }

    /**
     * Sends a message to a specified destination, using a specific connection in a given channel.
     * May require the use of {@link #openConnection(Host)} beforehand.
     *
     * @param channelId   the channel to send the message through
     * @param connection  the channel-specific connection to use.
     * @param msg         the message to send
     * @param destination the ip/port to send the message to
     */
    protected final void sendMessage(int channelId, ProtoMessage msg, Host destination, int connection) {
        sendMessage(channelId, msg, this.protoId, destination, connection);
    }

    /**
     * Sends a message to a different protocol in the specified destination,
     * using a specific connection in the default channel.
     * May require the use of {@link #openConnection(Host)} beforehand.
     *
     * @param destProto   the target protocol for the message.
     * @param connection  the channel-specific connection to use.
     * @param msg         the message to send
     * @param destination the ip/port to send the message to
     */
    protected final void sendMessage(ProtoMessage msg, short destProto, Host destination, int connection) {
        sendMessage(defaultChannel, msg, destProto, destination, connection);
    }

    /**
     * Sends a message to a different protocol in the specified destination,
     * using a specific connection in the given channel.
     * May require the use of {@link #openConnection(Host)} beforehand.
     *
     * @param channelId   the channel to send the message through
     * @param destProto   the target protocol for the message.
     * @param connection  the channel-specific connection to use.
     * @param msg         the message to send
     * @param destination the ip/port to send the message to
     */
    protected final void sendMessage(int channelId, ProtoMessage msg, short destProto,
                                     Host destination, int connection) {
        getChannelOrThrow(channelId);
        if (logger.isDebugEnabled())
            logger.debug("Sending: " + msg + " to " + destination + " proto " + destProto +
                    " channel " + channelId);
        babel.sendMessage(channelId, connection, new BabelMessage(msg, this.protoId, destProto), destination);
    }

    /**
     * Sends a message to the peer id, using the default secure channel.
     * May require the use of {@link #openConnection(Host, byte[])} beforehand.
     *
     * @param msg           the message to send
     * @param destinationId the peer id to send the message to
     */
    protected final void sendMessage(ProtoMessage msg, byte[] destinationId) {
        sendMessage(defaultChannel, msg, this.protoId, destinationId, 0);
    }

    /**
     * Sends a message to the peer id, using the given secure channel.
     * May require the use of {@link #openConnection(Host, byte[])} beforehand.
     *
     * @param channelId     the secure channel to send the message through
     * @param msg           the message to send
     * @param destinationId the peer id to send the message to
     */
    protected final void sendMessage(int channelId, ProtoMessage msg, byte[] destinationId) {
        sendMessage(channelId, msg, this.protoId, destinationId, 0);
    }

    /**
     * Sends a message to a different protocol for the specified peer id, using the default secure channel.
     * May require the use of {@link #openConnection(Host, byte[])} beforehand.
     *
     * @param msg           the message to send
     * @param destProto     the target protocol for the message.
     * @param destinationId the peer id to send the message to
     */
    protected final void sendMessage(ProtoMessage msg, short destProto, byte[] destinationId) {
        sendMessage(defaultChannel, msg, destProto, destinationId, 0);
    }

    /**
     * Sends a message to a specified peer id, using the default secure channel, and a specific connection.
     * May require the use of {@link #openConnection(Host, byte[])} beforehand.
     *
     * @param msg           the message to send
     * @param destinationId the peer id to send the message to
     * @param connection    the channel-specific connection to use.
     */
    protected final void sendMessage(ProtoMessage msg, byte[] destinationId, int connection) {
        sendMessage(defaultChannel, msg, this.protoId, destinationId, connection);
    }

    /**
     * Sends a message to a specified peer id, using a specific connection in the given secure channel.
     * May require the use of {@link #openConnection(Host, byte[])} beforehand.
     *
     * @param channelId     the channel to send the message through
     * @param msg           the message to send
     * @param destinationId the peer id to send the message to
     * @param connection    the channel-specific connection to use.
     */
    protected final void sendMessage(int channelId, ProtoMessage msg, byte[] destinationId, int connection) {
        sendMessage(channelId, msg, this.protoId, destinationId, connection);
    }

    /**
     * Sends a message to a different protocol for the specified peer id,
     * using a specific connection in the default secure channel.
     * May require the use of {@link #openConnection(Host, byte[])} beforehand.
     *
     * @param msg           the message to send
     * @param destProto     the target protocol for the message.
     * @param destinationId the peer id to send the message to
     * @param connection    the channel-specific connection to use.
     */
    protected final void sendMessage(ProtoMessage msg, short destProto, byte[] destinationId, int connection) {
        sendMessage(defaultChannel, msg, destProto, destinationId, connection);
    }

    /**
     * Sends a message to a different protocol for the speecified peer id,
     * using a specific connection in the given channel.
     * May require the use of {@link #openConnection(Host, byte[])} beforehand.
     *
     * @param channelId     the channel to send the message through
     * @param destProto     the target protocol for the message.
     * @param connection    the channel-specific connection to use.
     * @param msg           the message to send
     * @param destinationId the peer id to send the message to
     */
    protected final void sendMessage(int channelId, ProtoMessage msg, short destProto,
                                     byte[] destinationId, int connection) {
        getChannelOrThrow(channelId);
        if (logger.isDebugEnabled())
            logger.debug("Sending: " + msg + " to " + PeerIdEncoder.encodeToString(destinationId) + " proto " + destProto +
                    " channel " + channelId);
        babel.sendMessage(channelId, connection, new BabelMessage(msg, this.protoId, destProto), destinationId);
    }

    /**
     * Open a connection to the given peer using the default channel.
     * Depending on the channel, this method may be unnecessary/forbidden.
     *
     * @param peer the ip/port to create the connection to.
     */
    protected final void openConnection(Host peer) {
        openConnection(peer, defaultChannel);
    }

    /**
     * Open a connection to the given peer using the given channel.
     * Depending on the channel, this method may be unnecessary/forbidden.
     *
     * @param peer      the ip/port to create the connection to.
     * @param channelId the channel to create the connection in
     */
    protected final void openConnection(Host peer, int channelId) {
        babel.openConnection(channelId, peer, protoId);
    }

    /**
     * Open a connection to the given peer with the specified id using the default secure channel.
     * Depending on the channel, this method may be unnecessary/forbidden. <p>
     *
     * <i>Note:</i> If choosing your own identity for this connection is desired,
     * consider creating a new secure channel with
     * {@link #createSecureChannelWithAliases(String, Properties, Collection)}
     * or {@link #createSecureChannelWithIdentities(String, Properties, Collection)}.
     *
     * @param peer      the ip/port to create the connection to.
     * @param peerId    the id of the peer expected to connect to. If the connected
     *                  peer doesn't prove to have the specified id, the connection fails.
     */
    protected final void openConnection(Host peer, byte[] peerId) {
        openConnection(peer, peerId, defaultChannel);
    }

    /**
     * Open a connection to the given peer with the specified id using the given secure channel.
     * Depending on the channel, this method may be unnecessary/forbidden. <p>
     *
     * <i>Note:</i> If choosing your own identity for this connection is desired,
     * consider creating a new secure channel with
     * {@link #createSecureChannelWithAliases(String, Properties, Collection)}
     * or {@link #createSecureChannelWithIdentities(String, Properties, Collection)}.
     *
     * @param peer      the ip/port to create the connection to.
     * @param peerId    the id of the peer expected to connect to. If the connected
     *                  peer doesn't prove to have the specified id, the connection fails.
     * @param channelId the secure channel to create the connection in.
     */
    protected final void openConnection(Host peer, byte[] peerId, int channelId) {
        babel.openConnection(channelId, peer, peerId, protoId);
    }

    /**
     * Closes the connection to the given peer using the default channel.
     * Depending on the channel, this method may be unnecessary/forbidden.
     *
     * @param peer the ip/port to close the connection to.
     */
    protected final void closeConnection(Host peer) {
        closeConnection(peer, defaultChannel);
    }

    /**
     * Closes the connection to the given peer in the given channel.
     * Depending on the channel, this method may be unnecessary/forbidden.
     *
     * @param peer      the ip/port to close the connection to.
     * @param channelId the channel to close the connection in
     */
    protected final void closeConnection(Host peer, int channelId) {
        closeConnection(peer, channelId, protoId);
    }

    /**
     * Closes a specific connection to the given peer in the given channel.
     * Depending on the channel, this method may be unnecessary/forbidden.
     *
     * @param peer       the ip/port to close the connection to.
     * @param channelId  the channel to close the connection in
     * @param connection the channel-specific connection to close
     */
    protected final void closeConnection(Host peer, int channelId, int connection) {
        babel.closeConnection(channelId, peer, connection);
    }

    /**
     * Closes the connection to the peer with the given id in the default secure channel.
     * Depending on the channel, this method may be unnecessary/forbidden.
     *
     * @param peerId the id of the peer to close the connection to.
     */
    protected final void closeConnection(byte[] peerId) {
        closeConnection(peerId, defaultChannel);
    }

    /**
     * Closes the connection to the peer with the given id in the given secure channel.
     * Depending on the channel, this method may be unnecessary/forbidden.
     *
     * @param peerId    the id of the peer to close the connection to.
     * @param channelId the secure channel to close the connection in
     */
    protected final void closeConnection(byte[] peerId, int channelId) {
        closeConnection(peerId, channelId, protoId);
    }

    /**
     * Closes a specific connection to the peer with the given id in the given secure channel.
     * Depending on the channel, this method may be unnecessary/forbidden.
     *
     * @param peerId    the id of the peer to close the connection to.
     * @param channelId  the channel to close the connection in
     * @param connection the channel-specific connection to close
     */
    protected final void closeConnection(byte[] peerId, int channelId, int connection) {
        babel.closeConnection(channelId, peerId, connection);
    }


    /* ------------------ IPC BABEL PROXY -------------------------------------------------*/

    /**
     * Sends a request to the destination protocol
     *
     * @param request     request event
     * @param destination the destination protocol
     * @throws NoSuchProtocolException if the protocol does not exists
     */
    protected final void sendRequest(ProtoRequest request, short destination) throws NoSuchProtocolException {
        babel.sendIPC(new IPCEvent(request, protoId, destination));
    }

    /**
     * Sends a reply to the destination protocol
     *
     * @param destination the destination protocol
     * @param reply       reply event
     * @throws NoSuchProtocolException if the protocol does not exists
     */
    protected final void sendReply(ProtoReply reply, short destination) throws NoSuchProtocolException {
        babel.sendIPC(new IPCEvent(reply, protoId, destination));
    }

    // ------------------------------ NOTIFICATION BABEL PROXY ---------------------------------

    /**
     * Subscribes a notification, executing the given callback everytime it is triggered by any protocol.
     *
     * @param nId the id of the notification to subscribe to
     * @param h   the callback to execute upon receiving the notification
     * @throws HandlerRegistrationException if there is already a callback for the notification
     */
    protected final <V extends ProtoNotification> void subscribeNotification(short nId, NotificationHandler<V> h)
            throws HandlerRegistrationException {
        registerHandler(nId, h, notificationHandlers);
        babel.subscribeNotification(nId, this);
    }

    /**
     * Unsubscribes a notification.
     *
     * @param nId the id of the notification to unsubscribe from
     */
    protected final void unsubscribeNotification(short nId) {
        notificationHandlers.remove(nId);
        babel.unsubscribeNotification(nId, this);
    }

    /**
     * Triggers a notification, causing every protocol that subscribe it to execute its callback.
     *
     * @param n the notification event to trigger
     */
    protected final void triggerNotification(ProtoNotification n) {
        babel.triggerNotification(new NotificationEvent(n, protoId));
    }

    /* -------------------------- TIMER BABEL PROXY ----------------------- */

    /**
     * Setups a period timer
     *
     * @param timer  the timer event
     * @param first  timeout until first trigger (in milliseconds)
     * @param period periodicity (in milliseconds)
     * @return unique identifier of the timer set
     */
    protected long setupPeriodicTimer(ProtoTimer timer, long first, long period) {
        return babel.setupPeriodicTimer(timer, this, first, period);
    }

    /**
     * Setups a timer
     *
     * @param t       the timer event
     * @param timeout timout until trigger (in milliseconds)
     * @return unique identifier of the t set
     */
    protected long setupTimer(ProtoTimer t, long timeout) {
        return babel.setupTimer(t, this, timeout);
    }

    /**
     * Cancel the timer with the provided unique identifier
     *
     * @param timerID timer unique identifier
     * @return the canceled timer event, or null if it wasn't set or have already been trigger and was not periodic
     */
    protected ProtoTimer cancelTimer(long timerID) {
        return babel.cancelTimer(timerID);
    }

    /* -------------------------- IDENTITY MANAGEMENT ----------------------- */

    protected final IdentityCrypt generateIdentity() {
        return generateIdentity(true);
    }

    protected final IdentityCrypt generateIdentity(boolean persistOnDisk) {
        var idCrypt = babelSecurity.generateIdentityWithAliasPrefix(persistOnDisk, protoName);
        if (defaultIdentity == null) {
            defaultIdentity = new IdentityPair(idCrypt);
            defaultIdentityCrypt = idCrypt;
        }
        return idCrypt;
    }

    protected final IdentityCrypt generateIdentity(String alias) {
        return generateIdentity(true, alias);
    }

    protected final IdentityCrypt generateIdentity(boolean persistOnDisk, String alias) {
        var idCrypt = babelSecurity.generateIdentity(persistOnDisk, alias);
        if (defaultIdentity == null && idCrypt != null) {
            defaultIdentity = new IdentityPair(idCrypt);
            defaultIdentityCrypt = idCrypt;
        }
        return idCrypt;
    }

    protected final IdentityCrypt generateIdentity(KeyPair keyPair) throws NoSuchAlgorithmException {
        return generateIdentity(true, keyPair);
    }

    protected final IdentityCrypt generateIdentity(boolean persistOnDisk, KeyPair keyPair) throws NoSuchAlgorithmException {
        var idCrypt = babelSecurity.generateIdentityWithAliasPrefix(persistOnDisk, protoName, keyPair);
        if (defaultIdentity == null && idCrypt != null) {
            defaultIdentity = new IdentityPair(idCrypt);
            defaultIdentityCrypt = idCrypt;
        }
        return idCrypt;
    }

    protected final IdentityCrypt generateIdentity(boolean persistOnDisk, String alias, KeyPair keyPair) throws NoSuchAlgorithmException {
        var idCrypt = babelSecurity.generateIdentity(persistOnDisk, alias, keyPair);
        if (defaultIdentity == null) {
            defaultIdentity = new IdentityPair(idCrypt);
            defaultIdentityCrypt = idCrypt;
        }
        return idCrypt;
    }

    protected final IdentityPair setDefaultProtoIdentity(String alias) throws NoSuchElementException {
        var id = babelSecurity.getAliasIdentity(alias);
        if (id == null)
            throw new NoSuchElementException("Couldn't get retreive identity with alias " + alias);

        defaultIdentity = new IdentityPair(alias, id);
        defaultIdentityCrypt = null;

        return defaultIdentity;
    }

    protected final IdentityPair setDefaultProtoIdentity(byte[] identity) throws NoSuchElementException {
        var alias = babelSecurity.getIdentityAlias(identity);
        if (alias == null)
            throw new NoSuchElementException("Couldn't get retreive identity with id " + PeerIdEncoder.encodeToString(identity));

        defaultIdentity = new IdentityPair(alias, identity);
        defaultIdentityCrypt = null;

        return defaultIdentity;
    }

    protected final IdentityPair getOrGenerateDefaultProtoIdentity() {
        try {
            return getDefaultProtoIdentity();
        } catch (NoSuchElementException e) {
            defaultIdentityCrypt = generateIdentity();
            defaultIdentity = new IdentityPair(defaultIdentityCrypt);

            return defaultIdentity;
        }
    }

    protected final IdentityCrypt getDefaultProtoIdentityCrypt()
            throws NoSuchAlgorithmException, UnrecoverableEntryException, NoSuchElementException {
        if (defaultIdentityCrypt == null)
            defaultIdentityCrypt = babelSecurity.getIdentityCrypt(getDefaultProtoIdentity().alias());

        return defaultIdentityCrypt;
    }

    protected final IdentityPair getDefaultProtoIdentity() throws NoSuchElementException {
        if (defaultIdentity == null) {
            defaultIdentity = babelSecurity.getAllIdentitiesWithPrefix(protoName).stream()
                    .findAny()
                    .orElseThrow(() -> new NoSuchElementException("No protocol identity set. You must choose or create one."));
        }

        return defaultIdentity;
    }

    /* -------------------------- SECRET MANAGEMENT ----------------------- */

    protected final SecretCrypt generateSecretFromPassword(String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        return generateSecretFromPassword(true, password);
    }

    protected final SecretCrypt generateSecretFromPassword(boolean persistOnDisk, String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        var secret = babelSecurity.generateSecretFromPasswordWithAliasPrefix(persistOnDisk, protoName, password);
        if (defaultSecret == null)
            defaultSecret = secret;
        return secret;
    }

    protected final SecretCrypt generateSecretFromPassword(String password, String alias) {
        return generateSecretFromPassword(true, password, alias);
    }

    protected final SecretCrypt generateSecretFromPassword(boolean persistOnDisk, String password, String alias) {
        return generateSecretFromPassword(persistOnDisk, password, alias);
    }

    protected final SecretCrypt generateSecret() throws NoSuchAlgorithmException {
        return generateSecret(true);
    }

    protected final SecretCrypt generateSecret(boolean persistOnDisk) throws NoSuchAlgorithmException {
        var secret = babelSecurity.generateSecretWithAliasPrefix(persistOnDisk, protoName);
        if (defaultSecret == null)
            defaultSecret = secret;
        return secret;
    }

    protected final SecretCrypt generateSecret(String alias) throws NoSuchAlgorithmException {
        return generateSecret(true);
    }

    protected final SecretCrypt generateSecret(boolean persistOnDisk, String alias) throws NoSuchAlgorithmException {
        var secret = babelSecurity.generateSecret(persistOnDisk, alias);
        if (defaultSecret == null)
            defaultSecret = secret;
        return secret;
    }

    protected final SecretCrypt addSecret(SecretKey secretKey) throws NoSuchAlgorithmException {
        return addSecret(true, secretKey);
    }

    protected final SecretCrypt addSecret(boolean persistOnDisk, SecretKey secretKey) throws NoSuchAlgorithmException {
        var secret = babelSecurity.addSecretWithAliasPrefix(persistOnDisk, protoName, secretKey);
        if (defaultSecret == null)
            defaultSecret = secret;
        return secret;
    }

    protected final SecretCrypt addSecret(String alias, SecretKey secretKey) throws NoSuchAlgorithmException {
        return addSecret(true, alias, secretKey);
    }

    protected final SecretCrypt addSecret(boolean persistOnDisk, String alias, SecretKey secretKey)
            throws NoSuchAlgorithmException {
        var secret = babelSecurity.addSecret(persistOnDisk, alias, secretKey);
        if (defaultSecret == null)
            defaultSecret = secret;
        return secret;
    }

    protected final SecretCrypt setDefaultProtoSecret(String alias) throws NoSuchElementException {
        try {
            var secret = babelSecurity.getSecretCrypt(alias);
            if (secret == null)
                throw new NoSuchElementException("Couldn't retreive secret with alias " + alias);
            defaultSecret = secret;
            return defaultSecret;
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException e) {
            throw new NoSuchElementException(
                    "Couldn't retreive secret with alias " + alias, e);
        }
    }

    protected final SecretCrypt getDefaultProtoSecret() throws NoSuchElementException {
        if (defaultSecret == null)
            throw new NoSuchElementException("Protocol %s has no default secret".formatted(protoName));
        return defaultSecret;
    }

    // --------------------------------- DELIVERERS FROM BABEL ------------------------------------

    /**
     * Used by babel to deliver channel events to protocols. Do not evoke directly.
     */
    void deliverInternalEvent(InternalEvent event) {
        queue.add(event);
    }

    /* ------------------ MAIN LOOP -------------------------------------------------*/

    private void mainLoop() {
        while (true) {
            try {
                InternalEvent pe = this.queue.take();
                metrics_babel.totalEventsCount.inc();
                if (logger.isDebugEnabled()) {
                    logger.debug("Handling event: " + pe);
                }
                switch (pe) {
                    case MessageInEvent castPe -> {
                        metrics_babel.messagesInCount.inc();
                        this.handleMessageIn(castPe);
                    }
                    case MessageFailedEvent castPe -> {
                        metrics_babel.messagesFailedCount.inc();
                        this.handleMessageFailed(castPe);
                    }
                    case MessageSentEvent castPe -> {
                        metrics_babel.messagesSentCount.inc();
                        this.handleMessageSent(castPe);
                    }
                    case TimerEvent castPe -> {
                        metrics_babel.timersCount.inc();
                        this.handleTimer(castPe);
                    }
                    case NotificationEvent castPe -> {
                        metrics_babel.notificationsCount.inc();
                        this.handleNotification(castPe);
                    }
                    case IPCEvent castPe -> {
                        IPCEvent i = castPe;
                        switch (i.getIpc().getType()) {
                            case REPLY -> {
                                metrics_babel.repliesCount.inc();
                                handleReply((ProtoReply) i.getIpc(), i.getSenderID());
                            }
                            case REQUEST -> {
                                metrics_babel.requestsCount.inc();
                                handleRequest((ProtoRequest) i.getIpc(), i.getSenderID());
                            }
                            default -> throw new AssertionError("Ups");
                        }
                    }
                    case CustomChannelEvent castPe -> {
                        metrics_babel.customChannelEventsCount.inc();
                        this.handleChannelEvent(castPe);
                    }
                    default -> throw new AssertionError("Unexpected event received by babel. protocol "
                            + protoId + " (" + protoName + ")");
                }
            } catch (Exception e) {
                logger.error("Unhandled exception in protocol " + getProtoName() +" ("+ getProtoId() +") " + e, e);
                e.printStackTrace();
            }
        }
    }

    //TODO try catch (ClassCastException)
    private void handleMessageIn(MessageInEvent m) {
        BabelMessage msg = m.getMsg();
        MessageInHandler h = getChannelOrThrow(m.getChannelId()).messageInHandlers.get(msg.getMessage().getId());
        if (h != null)
            h.receive(msg.getMessage(), m.getFrom(), m.getFromId().orElse(null), msg.getSourceProto(), m.getChannelId());
        else
            logger.warn("Discarding unexpected message (id " + msg.getMessage().getId() + "): " + m);
    }

    private void handleMessageFailed(MessageFailedEvent e) {
        BabelMessage msg = e.getMsg();
        MessageFailedHandler h = getChannelOrThrow(e.getChannelId()).messageFailedHandlers.get(msg.getMessage().getId());
        if (h != null)
            h.onMessageFailed(msg.getMessage(), e.getTo(), e.getToId().orElse(null), msg.getDestProto(), e.getCause(), e.getChannelId());
        else if (logger.isDebugEnabled())
            logger.debug("Discarding unhandled message failed event " + e);
    }

    private void handleMessageSent(MessageSentEvent e) {
        BabelMessage msg = e.getMsg();
        MessageSentHandler h = getChannelOrThrow(e.getChannelId()).messageSentHandlers.get(msg.getMessage().getId());
        if (h != null)
            h.onMessageSent(msg.getMessage(), e.getTo(), e.getToId().orElse(null), msg.getDestProto(), e.getChannelId());
    }

    private void handleChannelEvent(CustomChannelEvent m) {
        ChannelEventHandler h = getChannelOrThrow(m.getChannelId()).channelEventHandlers.get(m.getEvent().getId());
        if (h != null)
            h.handleEvent(m.getEvent(), m.getChannelId());
        else if (logger.isDebugEnabled())
            logger.debug("Discarding unhandled channel event (id " + m.getChannelId() + "): " + m);
    }

    private void handleTimer(TimerEvent t) {
        TimerHandler h = this.timerHandlers.get(t.getTimer().getId());
        if (h != null)
            h.uponTimer(t.getTimer().clone(), t.getUuid());
        else
            logger.warn("Discarding unexpected timer (id " + t.getTimer().getId() + "): " + t);
    }

    private void handleNotification(NotificationEvent n) {
        NotificationHandler h = this.notificationHandlers.get(n.getNotification().getId());
        if (h != null)
            h.uponNotification(n.getNotification(), n.getEmitterID());
        else
            logger.warn("Discarding unexpected notification (id " + n.getNotification().getId() + "): " + n);
    }

    private void handleRequest(ProtoRequest r, short from) {
        RequestHandler h = this.requestHandlers.get(r.getId());
        if (h != null)
            h.uponRequest(r, from);
        else
            logger.warn("Discarding unexpected request (id " + r.getId() + "): " + r);
    }

    private void handleReply(ProtoReply r, short from) {
        ReplyHandler h = this.replyHandlers.get(r.getId());
        if (h != null)
            h.uponReply(r, from);
        else
            logger.warn("Discarding unexpected reply (id " + r.getId() + "): " + r);
    }

    private static class ChannelHandlers {
        private final Map<Short, MessageInHandler<? extends ProtoMessage>> messageInHandlers;
        private final Map<Short, MessageSentHandler<? extends ProtoMessage>> messageSentHandlers;
        private final Map<Short, MessageFailedHandler<? extends ProtoMessage>> messageFailedHandlers;
        private final Map<Short, ChannelEventHandler<? extends ChannelEvent>> channelEventHandlers;

        public ChannelHandlers() {
            this.messageInHandlers = new HashMap<>();
            this.messageSentHandlers = new HashMap<>();
            this.messageFailedHandlers = new HashMap<>();
            this.channelEventHandlers = new HashMap<>();
        }
    }

    // ------------------ PROTOCOL METRICS -------------------------------------------------

    /**
     * Register a metric for this protocol<br>
     * MUST only be called after call to super() in constructor
     * @param m metric to register
     * @return the registered metric
     * @param <T> type of the metric
     */
    protected final <T extends Metric<?>> T registerMetric(T m)  {
        MetricsManager.getInstance().registerMetric(m, protoId, protoName);
        return m;
    }

    /**
     * Enables the generic metrics for this Babel protocol
     */
    protected void enableGenericMetrics() {
        metrics_babel.registerMetrics();
    }

    /**
     * Set of generic metrics for Babel protocols
     */
    public static class ProtocolMetricsBabelMetrics {
        private final Counter totalEventsCount, messagesInCount, messagesFailedCount, messagesSentCount, timersCount,
                notificationsCount, requestsCount, repliesCount, customChannelEventsCount;

        private final short protoId;

        private final String protoName;

        public ProtocolMetricsBabelMetrics(short protoId, String protoName) {
            this.protoId = protoId;
            this.protoName = protoName;
            this.totalEventsCount =  new Counter.Builder("babel_events_total", Metric.Unit.NONE).build();
            this.messagesInCount = new Counter.Builder("babel_messages_in_total", Metric.Unit.NONE).build();
            this.messagesFailedCount = new Counter.Builder("babel_messages_failed_total", Metric.Unit.NONE).build();
            this.messagesSentCount = new Counter.Builder("babel_messages_sent_total", Metric.Unit.NONE).build();
            this.timersCount = new Counter.Builder("babel_timers_total", Metric.Unit.NONE).build();
            this.notificationsCount = new Counter.Builder("babel_notifications_total", Metric.Unit.NONE).build();
            this.requestsCount = new Counter.Builder("babel_requests_total", Metric.Unit.NONE).build();
            this.repliesCount = new Counter.Builder("babel_replies_total", Metric.Unit.NONE).build();
            this.customChannelEventsCount = new Counter.Builder("babel_custom_channel_events_total", Metric.Unit.NONE).build();
        }

        public void reset() {
            totalEventsCount.reset();
            messagesFailedCount.reset();
            messagesInCount.reset();
            messagesSentCount.reset();
            timersCount.reset();
            notificationsCount.reset();
            repliesCount.reset();
            requestsCount.reset();
            customChannelEventsCount.reset();
        }


        //Metrics are not registered by default
        public void registerMetrics(){
            MetricsManager.getInstance().registerMetric(totalEventsCount, protoId, protoName);
            MetricsManager.getInstance().registerMetric(messagesInCount, protoId, protoName);
            MetricsManager.getInstance().registerMetric(messagesFailedCount, protoId, protoName);
            MetricsManager.getInstance().registerMetric(messagesSentCount, protoId, protoName);
            MetricsManager.getInstance().registerMetric(timersCount, protoId, protoName);
            MetricsManager.getInstance().registerMetric(notificationsCount, protoId, protoName);
            MetricsManager.getInstance().registerMetric(requestsCount, protoId, protoName);
            MetricsManager.getInstance().registerMetric(repliesCount, protoId, protoName);
            MetricsManager.getInstance().registerMetric(customChannelEventsCount, protoId, protoName);
        }
    }

}
