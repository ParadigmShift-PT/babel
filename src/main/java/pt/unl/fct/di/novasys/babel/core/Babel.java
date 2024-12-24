package pt.unl.fct.di.novasys.babel.core;

import pt.unl.fct.di.novasys.babel.initializers.*;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.babel.internal.IPCEvent;
import pt.unl.fct.di.novasys.babel.internal.NotificationEvent;
import pt.unl.fct.di.novasys.babel.internal.TimerEvent;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.DNSSelfConfigurationProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.SelfConfigurationProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.DiscoveryProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.requests.RequestDiscovery;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.NoSuchProtocolException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.babel.metrics.MetricsManager;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;
import pt.unl.fct.di.novasys.babel.metrics.exporters.Exporter;
import pt.unl.fct.di.novasys.babel.metrics.generic.os.OSMetrics;
import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.channel.accrual.AccrualChannel;
import pt.unl.fct.di.novasys.channel.secure.SecureIChannel;
import pt.unl.fct.di.novasys.channel.secure.auth.AuthChannel;
import pt.unl.fct.di.novasys.channel.secure.tls.TLSChannel;
import pt.unl.fct.di.novasys.channel.secure.weakauth.WeakAuthChannel;
import pt.unl.fct.di.novasys.channel.simpleclientserver.SimpleClientChannel;
import pt.unl.fct.di.novasys.channel.simpleclientserver.SimpleServerChannel;
import pt.unl.fct.di.novasys.channel.tcp.SharedTCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Babel class provides applications with a Runtime that supports the
 * execution of protocols.
 *
 * <p>
 * An example of how to use the class follows:
 *
 * <pre>
 * Babel babel = Babel.getInstance(); // initialize babel
 * Properties configProps = babel.loadConfig("network_config.properties", args);
 * INetwork net = babel.getNetworkInstance();
 *
 * // Define protocols
 * ProtocolA protoA = new ProtocolA(net);
 * protoA.init(configProps);
 *
 * ProtocolB protoB = new ProtocolB(net);
 * protoB.init(configProps);
 *
 * // Register protocols
 * babel.registerProtocol(protoA);
 * babel.registerProtocol(protoB);
 *
 * // subscribe to notifications
 * protoA.subscribeNotification(protoA.NOTIFICATION_ID, this);
 *
 * // start babel runtime
 * babel.start();
 *
 * // Application Logic
 *
 * </pre>
 * <p>
 * For more information on protocol implementation with Babel:
 *
 * @see GenericProtocol
 */
public class Babel {
	private static final Logger logger = LogManager.getLogger(Babel.class);

	private static Babel system;
	private static Properties props;

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

	// Protocols
	private final Map<Short, GenericProtocol> protocolMap;
	private final Map<String, GenericProtocol> protocolByNameMap;
	private final Map<Short, Set<GenericProtocol>> subscribers;

	public final static String PAR_DEFAULT_INTERFACE = "babel.interface";
	public final static String PAR_DEFAULT_ADDRESS = "babel.address";
	public final static String PAR_DEFAULT_PORT = "babel.port";
	public final static String PAR_DISCOVERY_PROTOCOL = "babel.discovery";
	public final static String PAR_SELF_CONFIGURATION_PROTOCOL = "babel.selfconfiguration";
	public final static String PAR_DNS_CONFIGURATION_PROTOCOL = "babel.dnsconfiguration";

	private final List<DiscoveryProtocol> discoveries;
	private SelfConfigurationProtocol selfConfiguration;
	private DNSSelfConfigurationProtocol dnsSelfConfiguration;

	// Timers
	private final Map<Long, TimerEvent> allTimers;
	private final PriorityBlockingQueue<TimerEvent> timerQueue;
	private final Thread timersThread;
	public final AtomicLong timersCounter;

	// Channels
	private final Map<String, ChannelInitializer<? extends IChannel<BabelMessage>>> initializers;
	private final Map<String, SecureChannelInitializer<?>> secureChannelInitializers;

	private final Map<Integer, Triple<IChannel<BabelMessage>, ChannelToProtoForwarder, BabelMessageSerializer>> channelMap;
	private final Map<Integer, Triple<SecureIChannel<BabelMessage>, SecureChannelToProtoForwarder, BabelMessageSerializer>> secureChannelMap;
	private final AtomicInteger channelIdGenerator;

	private long startTime;
	private boolean started = false;

	private Babel() {
		// Protocols
		this.protocolMap = new ConcurrentHashMap<>();
		this.protocolByNameMap = new ConcurrentHashMap<>();
		this.subscribers = new ConcurrentHashMap<>();
		this.discoveries = new ArrayList<>();

		// Timers
		allTimers = new HashMap<>();
		timerQueue = new PriorityBlockingQueue<>();
		timersCounter = new AtomicLong();
		timersThread = new Thread(this::timerLoop);

		// Channels
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

		// registerChannelInitializer("Ackos", new AckosChannelInitializer());
		// registerChannelInitializer(MultithreadedTCPChannel.NAME, new
		// MultithreadedTCPChannelInitializer());

		registerChannelInitializer(AuthChannel.NAME, new AuthChannelInitializer());
		registerChannelInitializer(WeakAuthChannel.NAME, new WeakAuthChannelInitializer());
		registerChannelInitializer(TLSChannel.NAME, (s,l,k,t,p,pId) -> new TLSChannel<>(s,l,p,k,t));
	}

	private void timerLoop() {
		while (true) {
			long now = getMillisSinceStart();
			TimerEvent tE = timerQueue.peek();

			long toSleep = tE != null ? tE.getTriggerTime() - now : Long.MAX_VALUE;

			if (toSleep <= 0) {
				TimerEvent t = timerQueue.remove();
				// Deliver
				t.getConsumer().deliverInternalEvent(t);
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

	private void setupDiscoverable(DiscoverableProtocol dcProto) {
		discoveries.forEach((discovery) -> discovery.registerProtocol(dcProto));
	}

	public void setupSelfConfiguration(SelfConfigurableProtocol scProto, SelfConfigurationProtocol selfConfiguration) {
		Class<? extends SelfConfigurableProtocol> scProtoClass = scProto.getClass();
		Field[] fields = scProtoClass.getDeclaredFields();

		for (var field : fields) {
			var anotation = field.getAnnotation(AutoConfigureParameter.class);
			if (anotation != null) {
				String fieldNameCapitalized = StringUtils.capitalize(field.getName());
				String getterName = "getFirst" + fieldNameCapitalized;
				String setterName = "setFirst" + fieldNameCapitalized;
				try {
					Method getter = scProtoClass.getMethod(getterName);
					Method setter = scProtoClass.getMethod(setterName, String.class);
					String[] propsName = anotation.propsName().split("\\.");
					String fieldName = field.getName();
					if (propsName.length == 2) {
						fieldName = propsName[1];
					}
					if (selfConfiguration instanceof DNSSelfConfigurationProtocol || getter.invoke(scProto) == null) {
						selfConfiguration.addProtocolParameterToConfigure(fieldName, setter, getter, scProto);
					} else {
						selfConfiguration.addProtocolParameterConfigured(fieldName, setter, getter, scProto);
					}
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * Asks all the running discoveries for a contact for proto
	 * 
	 * @param proto  the protocol to receive the contact
	 * @param myself host representing the protocol
	 * @param listen if it should listen or just register
	 * @return true if a discovery protocol is running
	 */
	public boolean askRunningDiscovery(GenericProtocol proto, Host myself, boolean listen) {
		for (DiscoveryProtocol discovery : discoveries) {
			if (discovery.hasProtocolThreadStarted())
				proto.sendRequest(new RequestDiscovery(proto.getProtoName(), myself, proto.getProtoName(), listen),
						discovery.getProtoId());
			else
				discovery.uponRequestDiscovery(
						new RequestDiscovery(proto.getProtoName(), myself, proto.getProtoName(), listen),
						proto.getProtoId());
		}
		return !discoveries.isEmpty();
	}

	/**
	 * Begins the execution of all protocols registered in Babel
	 */
	public void start() {
		if (props.containsKey(PAR_DISCOVERY_PROTOCOL)) {
			try {
				logger.debug("Attempting to load Discovery Protocol: " + props.getProperty(PAR_DISCOVERY_PROTOCOL));
				String[] discoveryClassNames = props.getProperty(PAR_DISCOVERY_PROTOCOL).split(";");
				for (var className : discoveryClassNames) {
					@SuppressWarnings("unchecked")
					Class<? extends DiscoveryProtocol> discoveryClass = (Class<? extends DiscoveryProtocol>) Class
							.forName(className);
					this.discoveries.add(discoveryClass.getDeclaredConstructor().newInstance());
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Unable to load DiscoveryProtocol: '" + props.getProperty(PAR_DISCOVERY_PROTOCOL) + "'");
			}
		} else {
			logger.debug("No Discovery Protocol was requested to be loaded.");
		}

		if (props.containsKey(PAR_SELF_CONFIGURATION_PROTOCOL)) {
			try {
				logger.debug("Attemptimg to load Self Configuration Protocl: "
						+ props.getProperty(PAR_SELF_CONFIGURATION_PROTOCOL));
				@SuppressWarnings("unchecked")
				Class<? extends SelfConfigurationProtocol> selfConfigurationClass = (Class<? extends SelfConfigurationProtocol>) Class
						.forName(props.getProperty(PAR_SELF_CONFIGURATION_PROTOCOL));
				this.selfConfiguration = selfConfigurationClass.getDeclaredConstructor().newInstance();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException e) {
				e.printStackTrace();
				logger.error("Unable to load SelfConfigurationProtocol: '"
						+ props.getProperty(PAR_SELF_CONFIGURATION_PROTOCOL) + "'");
			}
		} else {
			logger.debug("No Self Configuration Protocol was requested to be loaded");
			this.selfConfiguration = null;
		}

		if (props.containsKey(PAR_DNS_CONFIGURATION_PROTOCOL)) {
			try {
				logger.debug("Attempting to load DNS Self Configuration Protocol: "
						+ props.getProperty(PAR_DNS_CONFIGURATION_PROTOCOL));
				@SuppressWarnings("unchecked")
				Class<? extends DNSSelfConfigurationProtocol> dnsSelfConfigurationClass = (Class<? extends DNSSelfConfigurationProtocol>) Class
						.forName(props.getProperty(PAR_DNS_CONFIGURATION_PROTOCOL));
				this.dnsSelfConfiguration = dnsSelfConfigurationClass.getDeclaredConstructor().newInstance();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException e) {
				e.printStackTrace();
				logger.error("Unable to load DNSSelfConfigurationProtocol: '"
						+ props.getProperty(PAR_DNS_CONFIGURATION_PROTOCOL) + "'");
			}
		} else {
			logger.debug("No DNS Self Configuration Protocol was requested to be loaded");
			this.dnsSelfConfiguration = null;
		}

		try {
			for (var discovery : discoveries)
				registerProtocol(discovery);
			if (this.selfConfiguration != null)
				registerProtocol(this.selfConfiguration);
			if (this.dnsSelfConfiguration != null)
				registerProtocol(this.dnsSelfConfiguration);
		} catch (ProtocolAlreadyExistsException e) {
			throw new RuntimeException(e);
		}

		startTime = System.currentTimeMillis();
		started = true;
		try {
			for (var discovery : discoveries)
				discovery.init(props);
			if (this.selfConfiguration != null)
				selfConfiguration.init(props);
			if (this.dnsSelfConfiguration != null)
				dnsSelfConfiguration.init(props);
		} catch (HandlerRegistrationException | IOException e) {
			throw new RuntimeException(e);
		}

		if (selfConfiguration != null) {
			askRunningDiscovery(selfConfiguration, selfConfiguration.getMyself(), false);
		}

		timersThread.start();
		for (GenericProtocol proto : protocolMap.values()) {
			logger.info("Starting " + proto.getProtoName());
			if (discoveries.size() != 0 && proto instanceof DiscoverableProtocol dcProto) {
				setupDiscoverable(dcProto);
			}
			if (dnsSelfConfiguration != null && proto instanceof SelfConfigurableProtocol scProto) {
				setupSelfConfiguration(scProto, dnsSelfConfiguration);
			} else if (selfConfiguration != null && proto instanceof SelfConfigurableProtocol scProto) {
				setupSelfConfiguration(scProto, selfConfiguration);
			}

			if (proto instanceof DiscoverableProtocol dcProto) {
				checkAndStartDcProto(dcProto);
			} else {
				proto.startEventThread();
			}
		}

		if (dnsSelfConfiguration != null) {
			var protosToSetup = dnsSelfConfiguration.search();
			if (selfConfiguration != null) {
				for (var proto : protosToSetup) {
					setupSelfConfiguration(proto, selfConfiguration);
				}
			}
		}
	}

	public boolean checkAndStartDcProto(DiscoverableProtocol dcProto) {
		if (dcProto.readyToStart() && !dcProto.hasProtocolThreadStarted()) {
			dcProto.start();
			dcProto.startEventThread();
			return true;
		}
		return dcProto.hasProtocolThreadStarted();
	}

	/**
	 * Register a protocol in Babel
	 *
	 * @param p the protocol to registered
	 * @throws ProtocolAlreadyExistsException if a protocol with the same id or name
	 *                                        has already been registered
	 */
	public void registerProtocol(GenericProtocol p) throws ProtocolAlreadyExistsException {
		GenericProtocol old = protocolMap.putIfAbsent(p.getProtoId(), p);
		if (old != null)
			throw new ProtocolAlreadyExistsException("Protocol conflicts on id with protocol: id=" + p.getProtoId()
					+ ":name=" + protocolMap.get(p.getProtoId()).getProtoName());
		old = protocolByNameMap.putIfAbsent(p.getProtoName(), p);
		if (old != null) {
			protocolMap.remove(p.getProtoId());
			throw new ProtocolAlreadyExistsException("Protocol conflicts on name: " + p.getProtoName() + " (id: "
					+ this.protocolByNameMap.get(p.getProtoName()).getProtoId() + ")");
		}
	}

	// ----------------------------- NETWORK

	/**
	 * Registers a new channel in babel
	 *
	 * @param name        the channel name
	 * @param initializer the channel initializer
	 */
	public void registerChannelInitializer(String name,
			ChannelInitializer<? extends IChannel<BabelMessage>> initializer) {
		ChannelInitializer<? extends IChannel<BabelMessage>> old = initializers.putIfAbsent(name, initializer);
		if (old != null) {
			throw new IllegalArgumentException(
					"Initializer for channel with name " + name + " already registered: " + old);
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
	 * Creates a channel for a protocol.
	 * <p>
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
	 * Creates a secure channel for a protocol.
	 * <p>
	 * Called by {@link GenericProtocol}. Do not evoke directly.
	 *
	 * @param channelName the name of the channel to create
	 * @param protoId     the protocol numeric identifier
	 * @param props       the properties required by the channel
	 * @param idAliases   the aliases of the identities to be used during
	 *                    communication.
	 *                    If {@code null}, any available identity can be used.
	 * @return the channel Id
	 * @throws IOException if channel creation fails
	 */
	int createSecureChannel(String channelName, short protoId, Properties props, Collection<String> idAliases,
			Collection<byte[]> ids)
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

		var security = BabelSecurity.getInstance();
		var keyManager = security.getKeyManager();
		keyManager = idAliases != null
				? keyManager.subKeyManagerWithAliases(idAliases)
				: ids != null
						? keyManager.subKeyManagerWithIds(ids)
						: keyManager;
		var trustManager = security.getTrustManager();
		SecureIChannel<BabelMessage> newChannel = initializer.initialize(serializer, forwarder,
				keyManager, trustManager, props, protoId);

		secureChannelMap.put(channelId, Triple.of(newChannel, forwarder, serializer));
		return channelId;
	}

	/**
	 * Registers interest in receiving events from a channel.
	 *
	 * @param consumerProto the protocol that will receive events generated by the
	 *                      new channel
	 *                      Called by {@link GenericProtocol}. Do not evoke
	 *                      directly.
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
		Triple<IChannel<BabelMessage>, ChannelToProtoForwarder, BabelMessageSerializer> channelEntry = getChannelSecureOrNot(
				channelId);
		if (channelEntry == null)
			throw new AssertionError("Sending message to non-existing channelId " + channelId);
		channelEntry.getLeft().sendMessage(msg, target, connection);
	}

	/**
	 * Sends a message to a peer using its id and the given secure channel and
	 * connection.
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
		Triple<IChannel<BabelMessage>, ChannelToProtoForwarder, BabelMessageSerializer> channelEntry = getChannelSecureOrNot(
				channelId);
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
		Triple<IChannel<BabelMessage>, ChannelToProtoForwarder, BabelMessageSerializer> channelEntry = getChannelSecureOrNot(
				channelId);
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
		channelEntry.getLeft().openConnection(target, targetId, connection);
	}

	/**
	 * Registers a (de)serializer for a message type.
	 * Called by {@link GenericProtocol}. Do not evoke directly.
	 */
	void registerSerializer(int channelId, short msgCode, ISerializer<? extends ProtoMessage> serializer) {
		Triple<IChannel<BabelMessage>, ChannelToProtoForwarder, BabelMessageSerializer> channelEntry = getChannelSecureOrNot(
				channelId);
		if (channelEntry == null)
			throw new AssertionError("Registering serializer in non-existing channelId " + channelId);
		channelEntry.getRight().registerProtoSerializer(msgCode, serializer);
	}

	/**
	 * Gets a channel entry from channelMap or secureChannelMap. Whichever one has
	 * the {@code channelId} key.
	 */
	private Triple<IChannel<BabelMessage>, ChannelToProtoForwarder, BabelMessageSerializer> getChannelSecureOrNot(
			int channelId) {
		var channelEntry = channelMap.get(channelId);
		if (channelEntry == null) {
			var secure = secureChannelMap.get(channelId);
			channelEntry = Triple.of(secure.getLeft(), secure.getMiddle(), secure.getRight());
		}
		return channelEntry;
	}

	// ----------------------------- REQUEST / REPLY / NOTIFY

	/**
	 * Send a request/reply to a protocol Called by {@link GenericProtocol}. Do not
	 * evoke directly.
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
		gp.deliverInternalEvent(ipc);
	}

	/**
	 * Subscribes a protocol to a notification Called by {@link GenericProtocol}. Do
	 * not evoke directly.
	 */
	void subscribeNotification(short nId, GenericProtocol consumer) {
		subscribers.computeIfAbsent(nId, k -> ConcurrentHashMap.newKeySet()).add(consumer);
	}

	/**
	 * Unsubscribes a protocol from a notification Called by
	 * {@link GenericProtocol}. Do not evoke directly.
	 */
	void unsubscribeNotification(short nId, GenericProtocol consumer) {
		subscribers.getOrDefault(nId, Collections.emptySet()).remove(consumer);
	}

	/**
	 * Triggers a notification, delivering to all subscribed protocols Called by
	 * {@link GenericProtocol}. Do not evoke directly.
	 */
	void triggerNotification(NotificationEvent n) {
		for (GenericProtocol c : subscribers.getOrDefault(n.getNotification().getId(), Collections.emptySet())) {
			c.deliverInternalEvent(n);
		}
	}
	// ---------------------------- TIMERS

	/**
	 * Setups a periodic timer to be monitored by Babel Called by
	 * {@link GenericProtocol}. Do not evoke directly.
	 *
	 * @param consumer the protocol that setup the periodic timer
	 * @param first    the amount of time until the first trigger of the timer event
	 * @param period   the periodicity of the timer event
	 */
	long setupPeriodicTimer(ProtoTimer t, GenericProtocol consumer, long first, long period) {
		long id = timersCounter.incrementAndGet();
		TimerEvent newTimer = new TimerEvent(t, id, consumer, getMillisSinceStart() + first, true, period);
		allTimers.put(newTimer.getUuid(), newTimer);
		timerQueue.add(newTimer);
		timersThread.interrupt();
		return id;
	}

	/**
	 * Setups a timer to be monitored by Babel Called by {@link GenericProtocol}. Do
	 * not evoke directly.
	 *
	 * @param consumer the protocol that setup the timer
	 * @param timeout  the amount of time until the timer event is triggered
	 */
	long setupTimer(ProtoTimer t, GenericProtocol consumer, long timeout) {
		long id = timersCounter.incrementAndGet();
		TimerEvent newTimer = new TimerEvent(t, id, consumer, getMillisSinceStart() + timeout, false, -1);
		timerQueue.add(newTimer);
		allTimers.put(newTimer.getUuid(), newTimer);
		timersThread.interrupt();
		return id;
	}

	/**
	 * Cancels a timer that was being monitored by Babel Babel will forget that the
	 * timer exists Called by {@link GenericProtocol}. Do not evoke directly.
	 *
	 * @param timerID the unique id of the timer event to be canceled
	 * @return the timer event or null if it was not being monitored by Babel
	 */
	ProtoTimer cancelTimer(long timerID) {
		TimerEvent tE = allTimers.remove(timerID);
		if (tE == null)
			return null;
		timerQueue.remove(tE);
		timersThread.interrupt(); // TODO is this needed?
		return tE.getTimer();
	}

	// ---------------------------- CONFIG

	/**
	 * Reads either the default or the given properties file (the file can be given
	 * with the argument -config) Builds a configuration file with the properties
	 * from the file and then merges ad-hoc properties given in the arguments.
	 * <p>
	 * Argument properties should be provided as: propertyName=value
	 *
	 * @param defaultConfigFile the path to the default properties file
	 * @param args              console parameters
	 * @return the configurations built
	 * @throws IOException               if the provided file does not exist
	 * @throws InvalidParameterException if the console parameters are not in the
	 *                                   format: prop=value
	 */

	public static Properties loadConfig(String[] args, String defaultConfigFile)
			throws IOException, InvalidParameterException {
		if (props == null)
			props = new Properties(args.length);
		else
			logger.debug("Extending config...");

		List<String> argsList = new ArrayList<String>(Arrays.asList(args));
		String configFile = extractConfigFileFromArguments(argsList, defaultConfigFile);

		if (configFile != null) {
			logger.debug("Config file being loaded: " + configFile);

			InputStream in = null;
			try {
				in = new FileInputStream(configFile);
			} catch (FileNotFoundException e) {
				// trying to load the file from within a Jar resource file
				in = Babel.class.getResourceAsStream("/" + configFile);
				if (in == null)
					throw e;
			}

			props.load(in);

			in.close();
		}

		if (logger.isDebugEnabled()) {
			logger.debug("------ Config values: ------");
			for (Object key : props.keySet()) {
				logger.debug(key + ": " + props.get(key));
			}
			logger.debug("--- End of configuration ---");
		}
		// Override with launch parameter props
		for (String arg : argsList) {
			String[] property = arg.split("=", 2);
			if (property.length == 2)
				props.setProperty(property[0], property[1]);
			else
				throw new InvalidParameterException("Unknown parameter: " + arg
						+ ". Property after spliting: " + Arrays.toString(property));
		}

		BabelSecurity.getInstance().loadConfig(props);

		return props;
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

	public long getMillisSinceStart() {
		return started ? System.currentTimeMillis() - startTime : 0;
	}

	// ---------------------------- MISC
	// -----------------------------------------------------------

	public String getProtoNameById(short protoId) {
		GenericProtocol proto = protocolMap.get(protoId);
		return proto != null ? proto.getProtoName() : "Unknown";
	}

	// ---------------------------- METRICS
	// -----------------------------------------------------------

	/**
	 * Registers a new exporter in the metrics manager<br>
	 * If no exporters are registered, the manager will attempt to load them from a
	 * configuration file
	 * 
	 * @param exporters the exporters to register
	 */
	public void registerExporters(Exporter... exporters) {
		MetricsManager.getInstance().registerExporters(exporters);
	}

	/**
	 * Registers the OS Metric(s) specified<br>
	 * The list can be seen in {@link OSMetrics.MetricType}
	 * 
	 * @param metricTypes the metrics to register
	 */
	public void registerOSMetrics(OSMetrics.MetricType... metricTypes) {
		MetricsManager.getInstance().registerOSMetrics(metricTypes);
	}

	/**
	 * Registers all the OS Metrics present in the specified Category(ies)<br>
	 * The list can be seen in {@link OSMetrics.MetricCategory}
	 * 
	 * @param metricCategories the categories to register
	 */
	public void registerOSMetricCategory(OSMetrics.MetricCategory... metricCategories) {
		MetricsManager.getInstance().registerOSMetricCategory(metricCategories);
	}

	/**
	 * Starts the metrics manager
	 * This will start all registered exporters
	 */
	public void startMetrics() {
		MetricsManager.getInstance().start();
	}

}
