package pt.unl.fct.di.novasys.babel.core;

import pt.unl.fct.di.novasys.babel.initializers.*;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.babel.internal.IPCEvent;
import pt.unl.fct.di.novasys.babel.internal.NotificationEvent;
import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;
import pt.unl.fct.di.novasys.babel.internal.TimerEvent;
import pt.unl.fct.di.novasys.babel.internal.security.CryptUtils;
import pt.unl.fct.di.novasys.babel.internal.security.PrivateIdStore;
import pt.unl.fct.di.novasys.babel.internal.security.PublicIdStore;
import pt.unl.fct.di.novasys.babel.internal.security.keystore.PeerIdAliasMapper;
import pt.unl.fct.di.novasys.babel.core.security.X509BabelKeyManager;
import pt.unl.fct.di.novasys.babel.core.security.X509BabelTrustManager;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.NoSuchProtocolException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.babel.metrics.MetricsManager;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.channel.accrual.AccrualChannel;
import pt.unl.fct.di.novasys.channel.secure.SecureIChannel;
import pt.unl.fct.di.novasys.channel.secure.auth.AuthChannel;
import pt.unl.fct.di.novasys.channel.simpleclientserver.SimpleClientChannel;
import pt.unl.fct.di.novasys.channel.simpleclientserver.SimpleServerChannel;
import pt.unl.fct.di.novasys.channel.tcp.SharedTCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

import org.apache.commons.lang3.tuple.Triple;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.Security;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Babel class provides applications with a Runtime that supports
 * the execution of protocols.
 *
 * <p> An example of how to use the class follows:
 *
 * <pre>
 *         Babel babel = Babel.getInstance(); //initialize babel
 *         Properties configProps = babel.loadConfig("network_config.properties", args);
 *         INetwork net = babel.getNetworkInstance();
 *
 *         //Define protocols
 *         ProtocolA protoA = new ProtocolA(net);
 *         protoA.init(configProps);
 *
 *         ProtocolB protoB = new ProtocolB(net);
 *         protoB.init(configProps);
 *
 *         //Register protocols
 *         babel.registerProtocol(protoA);
 *         babel.registerProtocol(protoB);
 *
 *         //subscribe to notifications
 *         protoA.subscribeNotification(protoA.NOTIFICATION_ID, this);
 *
 *         //start babel runtime
 *         babel.start();
 *
 *         //Application Logic
 *
 * </pre>
 * <p>
 * For more information on protocol implementation with Babel:
 *
 * @see GenericProtocol
 */
public class Babel {

    private static Babel system;

    /**
     * Returns the instance of the Babel Runtime
     *
     * @return the Babel instance
     */
    public static synchronized Babel getInstance() {
        if (system == null)
            system = new Babel();
        return system;
    }

    //Protocols
    private final Map<Short, GenericProtocol> protocolMap;
    private final Map<String, GenericProtocol> protocolByNameMap;
    private final Map<Short, Set<GenericProtocol>> subscribers;

    //Timers
    private final Map<Long, TimerEvent> allTimers;
    private final PriorityBlockingQueue<TimerEvent> timerQueue;
    private final Thread timersThread;
    private final AtomicLong timersCounter;

    //Channels
    private final Map<String, ChannelInitializer<? extends IChannel<BabelMessage>>> initializers;
    private final Map<String, SecureChannelInitializer<?>> secureChannelInitializers;

    private final Map<Integer,
            Triple<IChannel<BabelMessage>, ChannelToProtoForwarder, BabelMessageSerializer>> channelMap;
    private final Map<Integer,
            Triple<SecureIChannel<BabelMessage>, SecureChannelToProtoForwarder, BabelMessageSerializer>> secureChannelMap;
    private final AtomicInteger channelIdGenerator;

    private long startTime;
    private boolean started = false;

    // Security
    private boolean secureProtocolExists = false;

    private final SecurityConfiguration securityConfig;

    // TODO organize better
    private PrivateIdStore myIds;
    private PublicIdStore knownIds;

    private String keyStorePath;
    private String keyStorePassword;
    private String trustStorePath;
    private String trustStorePassword;

    private final SecurityConfiguration securityInitializer;

    // TODO interfaces to interact with these...
    private X509IKeyManager defaultKeyManager;
    private X509ITrustManager defaultTrustManager;

    private Babel() {
        //Protocols
        this.protocolMap = new ConcurrentHashMap<>();
        this.protocolByNameMap = new ConcurrentHashMap<>();
        this.subscribers = new ConcurrentHashMap<>();

        //Timers
        allTimers = new HashMap<>();
        timerQueue = new PriorityBlockingQueue<>();
        timersCounter = new AtomicLong();
        timersThread = new Thread(this::timerLoop);

        //Channels
        channelMap = new ConcurrentHashMap<>();
        secureChannelMap = new ConcurrentHashMap<>();
        channelIdGenerator = new AtomicInteger(0);
        this.initializers = new ConcurrentHashMap<>();
        this.secureChannelInitializers = new ConcurrentHashMap<>();

        registerChannelInitializer(SimpleClientChannel.NAME, new SimpleClientChannelInitializer());
        registerChannelInitializer(SimpleServerChannel.NAME, new SimpleServerChannelInitializer());
        registerChannelInitializer(TCPChannel.NAME, new TCPChannelInitializer());
        registerChannelInitializer(AccrualChannel.NAME, new AccrualChannelInitializer());
        registerChannelInitializer(SharedTCPChannel.NAME, new SharedTCPChannelInitializer());

        //registerChannelInitializer("Ackos", new AckosChannelInitializer());
        //registerChannelInitializer(MultithreadedTCPChannel.NAME, new MultithreadedTCPChannelInitializer());

        registerChannelInitializer(AuthChannel.NAME, new AuthChannelInitializer());

        //Security
        securityConfig = new SecurityConfiguration();
    }

    private void timerLoop() {
        while (true) {
            long now = getMillisSinceStart();
            TimerEvent tE = timerQueue.peek();

            long toSleep = tE != null ? tE.getTriggerTime() - now : Long.MAX_VALUE;

            if (toSleep <= 0) {
                TimerEvent t = timerQueue.remove();
                //Deliver
                t.getConsumer().deliverTimer(t);
                if (t.isPeriodic()) {
                    t.setTriggerTime(now + t.getPeriod());
                    timerQueue.add(t);
                }
            } else {
                try {
                    Thread.sleep(toSleep);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * Begins the execution of all protocols registered in Babel
     */
    public void start() {
        startTime = System.currentTimeMillis();
        started = true;
        MetricsManager.getInstance().start();
        timersThread.start();
        protocolMap.values().forEach(GenericProtocol::start);

        if (secureProtocolExists && !securityConfig.isInitialized())
            initSecurityFeatures();
    }

    /**
     * Register a protocol in Babel
     *
     * @param p the protocol to registered
     * @throws ProtocolAlreadyExistsException if a protocol with the same id or name has already been registered
     */
    public void registerProtocol(GenericProtocol p) throws ProtocolAlreadyExistsException {
        GenericProtocol old = protocolMap.putIfAbsent(p.getProtoId(), p);
        if (old != null) throw new ProtocolAlreadyExistsException(
                "Protocol conflicts on id with protocol: id=" + p.getProtoId() + ":name=" + protocolMap.get(
                        p.getProtoId()).getProtoName());
        old = protocolByNameMap.putIfAbsent(p.getProtoName(), p);
        if (old != null) {
            protocolMap.remove(p.getProtoId());
            throw new ProtocolAlreadyExistsException(
                    "Protocol conflicts on name: " + p.getProtoName() + " (id: " + this.protocolByNameMap.get(
                            p.getProtoName()).getProtoId() + ")");
        }

        if (p.isSecureProtocol()) {
            secureProtocolExists = true;
            if (started && !securityConfig.isInitialized())
                initSecurityFeatures();
        }
    }

    /**
     * TODO document better <p>
     * If using security features, this method and changes to the returned object
     * must be made before {@value Babel#start()} to ensure correct behaviour.
     */
    public SecurityConfiguration securityConfiguration() {
        return securityConfig;
    }

    private void initSecurityFeatures() {
        // TODO this is just a placeholder to allow for testing... Make the keystores be selected, read from props, or lazy loadaded (for ad-hoc ids)
        // This will all be replaced by SecurityConfiguration
        Security.addProvider(new BouncyCastleProvider());

        myIds = new PrivateIdStore();
        knownIds = new PublicIdStore();

        var keyPair = CryptUtils.getInstance().createRandomKeyPair();
        var idBytes = PeerIdEncoder.fromPublicKey(keyPair.getPublic());
        var idStr = PeerIdEncoder.encodeToString(idBytes);
        var cert = CryptUtils.getInstance().createSelfSignedX509Certificate(keyPair, idStr, 365);
        myIds.setCredential(idBytes, keyPair.getPrivate(), cert);

        var idAliasMapper = new PeerIdAliasMapper(idStr, idBytes);

        //var trustStore = System.getProperty("javax.net.ssl.trustStore"); ?
        //var trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword"); ?

        try {
            defaultKeyManager = new X509BabelKeyManager(myIds.getKeyStore(), "", idAliasMapper);
            defaultTrustManager = new X509BabelTrustManager();
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to load the given key store.");
        }
    }

    // ----------------------------- NETWORK

    /**
     * Registers a new channel in Babel.
     *
     * @param name        the channel name
     * @param initializer the channel initializer
     */
    public void registerChannelInitializer(String name,
                                           ChannelInitializer<? extends IChannel<BabelMessage>> initializer) {
        ChannelInitializer<? extends IChannel<BabelMessage>> old = initializers.putIfAbsent(name, initializer);
        if (old != null) {
            throw new IllegalArgumentException("Initializer for channel with name " + name +
                    " already registered: " + old);
        }
    }

    /**
     * Registers a new secure channel in Babel.
     *
     * @param name        the channel name
     * @param initializer the channel initializer
     */
    public void registerChannelInitializer(String name, SecureChannelInitializer<?> initializer) {
        var old = secureChannelInitializers.putIfAbsent(name, initializer);
        if (old != null) {
            throw new IllegalArgumentException("Initializer for secure channel with name " + name +
                    " already registered: " + old);
        }
    }


    /**
     * Creates a channel for a protocol. <p>
     * Called by {@link GenericProtocol}. Do not evoke directly.
     *
     * @param channelName the name of the channel to create
     * @param protoId     the protocol numeric identifier
     * @param props       the properties required by the channel
     * @return the channel Id
     * @throws IOException if channel creation fails
     */
    int createChannel(String channelName, short protoId, Properties props)
            throws IOException {
        ChannelInitializer<? extends IChannel<?>> initializer = initializers.get(channelName);
        if (initializer == null)
            throw new IllegalArgumentException("Channel initializer not registered: " + channelName +
                    (secureChannelInitializers.containsKey(channelName)
                            ? ". Did you mean to use createSecureChannel() instead?"
                            : ""));

        int channelId = channelIdGenerator.incrementAndGet();
        BabelMessageSerializer serializer = new BabelMessageSerializer(new ConcurrentHashMap<>());
        ChannelToProtoForwarder forwarder = new ChannelToProtoForwarder(channelId);
        IChannel<BabelMessage> newChannel = initializer.initialize(serializer, forwarder, props, protoId);
        channelMap.put(channelId, Triple.of(newChannel, forwarder, serializer));
        return channelId;
    }

    /**
     * Creates a secure channel for a protocol. <p>
     * Called by {@link GenericProtocol}. Do not evoke directly.
     *
     * @param channelName  the name of the channel to create
     * @param protoId      the protocol numeric identifier
     * @param props        the properties required by the channel
     * @param keyManager   the key manager to be used by the channel. If empty, the default is used.
     * @param trustManager the trust manager to be used by the channel. If empty, the default is used.
     * @return the channel Id
     * @throws IOException if channel creation fails
     */
    int createSecureChannel(String channelName, short protoId, Properties props,
            Optional<X509IKeyManager> keyManager, Optional<X509ITrustManager> trustManager)
            throws IOException {
        SecureChannelInitializer<?> initializer = secureChannelInitializers.get(channelName);
        if (initializer == null)
            throw new IllegalArgumentException("Secure channel initializer not registered: " + channelName +
                    (initializers.containsKey(channelName)
                            ? ". Did you mean to use createChannel(...) instead?"
                            : ""));

        int channelId = channelIdGenerator.incrementAndGet();
        BabelMessageSerializer serializer = new BabelMessageSerializer(new ConcurrentHashMap<>());
        SecureChannelToProtoForwarder forwarder = new SecureChannelToProtoForwarder(channelId);
        SecureIChannel<BabelMessage> newChannel = initializer.initialize(serializer, forwarder,
                trustManager.orElse(defaultTrustManager), keyManager.orElse(defaultKeyManager), props, protoId);
        secureChannelMap.put(channelId, Triple.of(newChannel, forwarder, serializer));
        return channelId;
    }

    /**
     * Registers interest in receiving events from a channel.
     *
     * @param consumerProto the protocol that will receive events generated by the new channel
     *                      Called by {@link GenericProtocol}. Do not evoke directly.
     * @throws IllegalArgumentException if {@code channelId} refers to a secure channel but {@code consumerProto}
     *                                  is not a {@link SecureProtocol}.
     */
    void registerChannelInterest(int channelId, short protoId, GenericProtocol consumerProto) {
        ChannelToProtoForwarder forwarder = getChannelSecureOrNot(channelId).getMiddle();
        forwarder.addConsumer(protoId, consumerProto);
    }

    /**
     * Sends a message to a peer using the given channel and connection.
     * Called by {@link GenericProtocol}. Do not evoke directly.
     */
    void sendMessage(int channelId, int connection, BabelMessage msg, Host target) {
        Triple<IChannel<BabelMessage>, ChannelToProtoForwarder, BabelMessageSerializer> channelEntry =
                getChannelSecureOrNot(channelId);
        if (channelEntry == null)
            throw new AssertionError("Sending message to non-existing channelId " + channelId);
        channelEntry.getLeft().sendMessage(msg, target, connection);
    }

    /**
     * Sends a message to a peer using its id and the given secure channel and connection.
     * Called by {@link GenericProtocol}. Do not evoke directly.
     */
    void sendMessage(int channelId, int connection, BabelMessage msg, byte[] targetId) {
        var channelEntry = secureChannelMap.get(channelId);
        if (channelEntry == null)
            throw new AssertionError("Sending message to non-existing secure channelId " + channelId +
                    (channelMap.containsKey(channelId)
                            ? ". Did you mean to use sendMessage( ... , Host) instead (i.e. to a non-secure channel)?"
                            : ""));
        channelEntry.getLeft().sendMessage(msg, targetId, connection);
    }

    /**
     * Closes a connection to a peer in a given channel.
     * Called by {@link GenericProtocol}. Do not evoke directly.
     */
    void closeConnection(int channelId, Host target, int connection) {
        Triple<IChannel<BabelMessage>, ChannelToProtoForwarder, BabelMessageSerializer> channelEntry =
                getChannelSecureOrNot(channelId);
        if (channelEntry == null)
            throw new AssertionError("Closing connection in non-existing channelId " + channelId);
        channelEntry.getLeft().closeConnection(target, connection);
    }

    /**
     * Closes a secure connection to a peer in a given channel.
     * Called by {@link GenericProtocol}. Do not evoke directly.
     */
    void closeConnection(int channelId, byte[] targetId, int connection) {
        var channelEntry = secureChannelMap.get(channelId);
        if (channelEntry == null)
            throw new AssertionError("Closing connection in non-existing secure channelId " + channelId +
                    (channelMap.containsKey(channelId)
                            ? ". Did you mean to use closeConnection( ... , Host) instead (i.e. to a non-secure channel)?"
                            : ""));
        channelEntry.getLeft().closeConnection(targetId, connection);
    }

    /**
     * Opens a connection to a peer in the given channel.
     * Called by {@link GenericProtocol}. Do not evoke directly.
     */
    void openConnection(int channelId, Host target, int connection) {
        Triple<IChannel<BabelMessage>, ChannelToProtoForwarder, BabelMessageSerializer> channelEntry =
                getChannelSecureOrNot(channelId);
        if (channelEntry == null)
            throw new AssertionError("Opening connection in non-existing channelId " + channelId);
        channelEntry.getLeft().openConnection(target, connection);
    }

    /**
     * Opens a secure connection to a peer in the given channel.
     * Called by {@link GenericProtocol}. Do not evoke directly.
     */
    void openConnection(int channelId, Host target, byte[] targetId, int connection) {
        var channelEntry = secureChannelMap.get(channelId);
        if (channelEntry == null)
            throw new AssertionError("Opening connection in non-existing secure channelId " + channelId +
                    (channelMap.containsKey(channelId)
                            ? ". Did you mean to use openConnection(...) without specifying the peer id (i.e. to a non-secure connection)?"
                            : ""));
        channelEntry.getLeft().openConnection(target, connection);
    }

    /**
     * Registers a (de)serializer for a message type.
     * Called by {@link GenericProtocol}. Do not evoke directly.
     */
    void registerSerializer(int channelId, short msgCode, ISerializer<? extends ProtoMessage> serializer) {
        Triple<IChannel<BabelMessage>, ChannelToProtoForwarder, BabelMessageSerializer> channelEntry =
                getChannelSecureOrNot(channelId);
        if (channelEntry == null)
            throw new AssertionError("Registering serializer in non-existing channelId " + channelId);
        channelEntry.getRight().registerProtoSerializer(msgCode, serializer);
    }

    /**
     * Gets a channel entry from channelMap or secureChannelMap. Whichever one has the {@code channelId} key.
     */
    private Triple<IChannel<BabelMessage>, ChannelToProtoForwarder, BabelMessageSerializer> getChannelSecureOrNot(int channelId) {
        var channelEntry = channelMap.get(channelId);
        if (channelEntry == null) {
            var secure = secureChannelMap.get(channelId);
            channelEntry = Triple.of(secure.getLeft(), secure.getMiddle(), secure.getRight());
        }
        return channelEntry;
    }

    // ----------------------------- REQUEST / REPLY / NOTIFY

    /**
     * Send a request/reply to a protocol
     * Called by {@link GenericProtocol}. Do not evoke directly.
     */
    void sendIPC(IPCEvent ipc) throws NoSuchProtocolException {
        GenericProtocol gp = protocolMap.get(ipc.getDestinationID());
        if (gp == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(ipc.getDestinationID()).append(" not executing.");
            sb.append("Executing protocols: [");
            protocolMap.forEach((id, p) -> sb.append(id).append(" - ").append(p.getProtoName()).append(", "));
            sb.append("]");
            throw new NoSuchProtocolException(sb.toString());
        }
        gp.deliverIPC(ipc);
    }

    /**
     * Subscribes a protocol to a notification
     * Called by {@link GenericProtocol}. Do not evoke directly.
     */
    void subscribeNotification(short nId, GenericProtocol consumer) {
        subscribers.computeIfAbsent(nId, k -> ConcurrentHashMap.newKeySet()).add(consumer);
    }

    /**
     * Unsubscribes a protocol from a notification
     * Called by {@link GenericProtocol}. Do not evoke directly.
     */
    void unsubscribeNotification(short nId, GenericProtocol consumer) {
        subscribers.getOrDefault(nId, Collections.emptySet()).remove(consumer);
    }

    /**
     * Triggers a notification, delivering to all subscribed protocols
     * Called by {@link GenericProtocol}. Do not evoke directly.
     */
    void triggerNotification(NotificationEvent n) {
        for (GenericProtocol c : subscribers.getOrDefault(n.getNotification().getId(), Collections.emptySet())) {
            c.deliverNotification(n);
        }
    }
    // ---------------------------- TIMERS

    /**
     * Setups a periodic timer to be monitored by Babel
     * Called by {@link GenericProtocol}. Do not evoke directly.
     *
     * @param consumer the protocol that setup the periodic timer
     * @param first    the amount of time until the first trigger of the timer event
     * @param period   the periodicity of the timer event
     */
    long setupPeriodicTimer(ProtoTimer t, GenericProtocol consumer, long first, long period) {
        long id = timersCounter.incrementAndGet();
        TimerEvent newTimer = new TimerEvent(t, id, consumer,
                getMillisSinceStart() + first, true, period);
        allTimers.put(newTimer.getUuid(), newTimer);
        timerQueue.add(newTimer);
        timersThread.interrupt();
        return id;
    }

    /**
     * Setups a timer to be monitored by Babel
     * Called by {@link GenericProtocol}. Do not evoke directly.
     *
     * @param consumer the protocol that setup the timer
     * @param timeout  the amount of time until the timer event is triggered
     */
    long setupTimer(ProtoTimer t, GenericProtocol consumer, long timeout) {
        long id = timersCounter.incrementAndGet();
        TimerEvent newTimer = new TimerEvent(t, id, consumer,
                getMillisSinceStart() + timeout, false, -1);
        timerQueue.add(newTimer);
        allTimers.put(newTimer.getUuid(), newTimer);
        timersThread.interrupt();
        return id;
    }

    /**
     * Cancels a timer that was being monitored by Babel
     * Babel will forget that the timer exists
     * Called by {@link GenericProtocol}. Do not evoke directly.
     *
     * @param timerID the unique id of the timer event to be canceled
     * @return the timer event or null if it was not being monitored by Babel
     */
    ProtoTimer cancelTimer(long timerID) {
        TimerEvent tE = allTimers.remove(timerID);
        if (tE == null)
            return null;
        timerQueue.remove(tE);
        timersThread.interrupt(); //TODO is this needed?
        return tE.getTimer();
    }

    // ---------------------------- CONFIG

    /**
     * Reads either the default or the given properties file (the file can be given with the argument -config)
     * Builds a configuration file with the properties from the file and then merges ad-hoc properties given
     * in the arguments.
     * <p>
     * Argument properties should be provided as:   propertyName=value
     *
     * @param defaultConfigFile the path to the default properties file
     * @param args              console parameters
     * @return the configurations built
     * @throws IOException               if the provided file does not exist
     * @throws InvalidParameterException if the console parameters are not in the format: prop=value
     */
    public static Properties loadConfig(String[] args, String defaultConfigFile)
            throws IOException, InvalidParameterException {

        List<String> argsList = new ArrayList<>();
        Collections.addAll(argsList, args);
        String configFile = extractConfigFileFromArguments(argsList, defaultConfigFile);

        Properties configuration = new Properties();
        if (configFile != null)
            configuration.load(new FileInputStream(configFile));
        //Override with launch parameter props
        for (String arg : argsList) {
            String[] property = arg.split("=");
            if (property.length == 2)
                configuration.setProperty(property[0], property[1]);
            else
                throw new InvalidParameterException("Unknown parameter: " + arg);
        }
        return configuration;
    }

    private static String extractConfigFileFromArguments(List<String> args, String defaultConfigFile) {
        String config = defaultConfigFile;
        Iterator<String> iter = args.iterator();
        while (iter.hasNext()) {
            String param = iter.next();
            if (param.equals("-conf")) {
                if (iter.hasNext()) {
                    iter.remove();
                    config = iter.next();
                    iter.remove();
                }
                break;
            }
        }
        return config;
    }

    // TODO loadKeystore and loadTrustStore?

    public long getMillisSinceStart() {
        return started ? System.currentTimeMillis() - startTime : 0;
    }

}
