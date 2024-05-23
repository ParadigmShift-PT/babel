package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.DiscoverableProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.messages.ServiceMessage;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.timers.AnoucementTimer;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Protocol to be included in the Babel framework in the future
 * 
 * It does not depend on the network-layer as Babel does right now since it does
 * not support any other communications besides TCP. In the future it might be
 * expanded
 */
public class MulticastDiscoveryProtocol extends DiscoveryProtocol {
	private static final Logger logger = LogManager.getLogger(MulticastDiscoveryProtocol.class);

	public static final int DEFAULT_MULTICAST_PORT = 1025;
	public static final int DEFAULT_UNICAST_PORT = 1026;
	public static final String MULTICAST_ADDRESS = "233.138.122.123";
	public static final int ANOUNCEMENT_COOLDOWN = 1000;
	public static final short PROTO_ID = 32500;
	public static final String PROTO_NAME = "BabelMulticastDiscovery";

	public static final String PAR_DISCOVERY_MULTICAST_INTERFACE = "babel.discovery.multicast.interface";
	public static final String PAR_DISCOVERY_MULTICAST_ADDRESS = "babel.discovery.multicast.addr";
	public static final String PAR_DISCOVERY_MULTICAST_PORT = "babel.discovery.multicast.port";

	private MulticastSocket multicastSocket;
	private DatagramSocket unicastSocket;
	private Map<String, ServiceMessage> discoveryProtocolsData;
	private Map<String, DiscoverableProtocol> protocolsWaiting;
	private InetSocketAddress multicastSocketAddress;
	private Host discoveryHost;

	private Thread listeningMulticastThread;
	private Thread listeningUnicastThread;

	public MulticastDiscoveryProtocol() {
		super(PROTO_NAME, PROTO_ID);
	}

	@Override
	public void init(Properties props) throws HandlerRegistrationException, IOException {
		discoveryProtocolsData = new HashMap<String, ServiceMessage>();
		protocolsWaiting = new HashMap<String, DiscoverableProtocol>();

		int targetPort = DEFAULT_MULTICAST_PORT;
		if (props.containsKey(PAR_DISCOVERY_MULTICAST_PORT)) {
			targetPort = Integer.parseInt(props.getProperty(PAR_DISCOVERY_MULTICAST_PORT));
		}

		multicastSocketAddress = new InetSocketAddress(
				props.getProperty(PAR_DISCOVERY_MULTICAST_ADDRESS, MULTICAST_ADDRESS), targetPort);

		NetworkInterface networkInterface;
		if (!props.containsKey(PAR_DISCOVERY_MULTICAST_INTERFACE)) {
			// Bind to all?? interface that supports multicast
			networkInterface = NetworkInterface.networkInterfaces().filter(i -> {
				try {
					return i.supportsMulticast();
				} catch (SocketException e) {
					e.printStackTrace();
					return false;
				}
			}).findAny().orElseThrow(() -> new RuntimeException("No network interface supports multicast"));
		} else {
			networkInterface = NetworkInterface.getByName(props.getProperty(PAR_DISCOVERY_MULTICAST_INTERFACE));
		}

		multicastSocket = new MulticastSocket(targetPort);
		System.err.println("Multicast is going to use interface: " + networkInterface.getDisplayName());
		multicastSocket.joinGroup(multicastSocketAddress, networkInterface);

		logger.info("DiscoveryProtocol set up");

		InetAddress address = null;

		if (props.containsKey(PAR_DISCOVERY_UNICAST_ADDRESS)) {
			address = InetAddress.getByName(props.getProperty(PAR_DISCOVERY_UNICAST_ADDRESS));
		} else if (props.containsKey(PAR_DISCOVERY_UNICAST_INTERFACE)) {
			List<InterfaceAddress> l = NetworkInterface.getByName(props.getProperty(PAR_DISCOVERY_UNICAST_INTERFACE))
					.getInterfaceAddresses();
			for (InterfaceAddress a : l) {
				if (a.getAddress() != null && a.getAddress() instanceof Inet4Address) {
					address = a.getAddress();
					break;
				}
				if (address != null)
					break;
			}
		} else {
			Iterator<NetworkInterface> iterator = NetworkInterface.networkInterfaces().distinct().iterator();
			while (iterator.hasNext()) {
				NetworkInterface n = iterator.next();
				if (!n.isLoopback() && !n.isVirtual() && n.isUp() && !n.isPointToPoint()) {
					for (InterfaceAddress a : n.getInterfaceAddresses()) {
						if (a.getAddress() != null && a.getAddress() instanceof Inet4Address)
							address = a.getAddress();
						break;
					}
					if (address != null)
						break;
				}
			}
		}

		int unicastPort = DEFAULT_UNICAST_PORT;
		if (props.containsKey(PAR_DISCOVERY_UNICAST_PORT))
			unicastPort = Integer.parseInt(props.getProperty(PAR_DISCOVERY_UNICAST_PORT));

		discoveryHost = new Host(address, unicastPort);
		unicastSocket = new DatagramSocket(unicastPort, address);

		registerTimerHandler(AnoucementTimer.TIMER_ID, this::announce);

		setupPeriodicTimer(new AnoucementTimer(), ANOUNCEMENT_COOLDOWN, ANOUNCEMENT_COOLDOWN);

		listeningMulticastThread = new Thread(() -> listen(multicastSocket));
		listeningUnicastThread = new Thread(() -> listen(unicastSocket));
		listeningMulticastThread.start();
		listeningUnicastThread.start();

		logger.info("DiscoveryProtocol initialized");
	}

	private void announce(AnoucementTimer timer, long timerId) {
		logger.info("Firing anouncements");

		if (protocolsWaiting.size() == 0) {
			logger.debug("No protocols waiting registered");
			return;
		}

		List<ServiceMessage> pendingServices = new ArrayList<ServiceMessage>();
		for (String protocol : protocolsWaiting.keySet()) {
			pendingServices.add(this.discoveryProtocolsData.get(protocol));
			logger.debug("Added protocol " + protocol + " to seend buffer");
		}

		try {

			List<byte[]> announces = ServiceMessage.convertToMessage(pendingServices, true);
			for (byte[] m : announces) {
				unicastSocket.send(new DatagramPacket(m, m.length, multicastSocketAddress.getAddress(),
						multicastSocketAddress.getPort()));
				logger.debug("Sent one multicast message");
			}

		} catch (Exception e) {
			logger.error("Could not send announcements.");
			e.printStackTrace();
		}
	}

	@Override
	public void registerProtocol(DiscoverableProtocol dcProto) {
		this.discoveryProtocolsData.put(dcProto.getProtoName(),
				new ServiceMessage(dcProto.getProtoName(), dcProto.getMyself(), discoveryHost));
		if (dcProto.needsDiscovery()) {
			this.protocolsWaiting.put(dcProto.getProtoName(), dcProto);
		}
	}

	/**
	 * Listens for incoming announcements
	 * 
	 * Since no support exists in the network layer, this method should be called in
	 * a thread manually for now. Eventually this should be moved to netty.
	 */
	private void listen(DatagramSocket socket) {
		while (true) {
			try {
				DatagramPacket packet = new DatagramPacket(new byte[ServiceMessage.DATAGRAM_SIZE],
						ServiceMessage.DATAGRAM_SIZE);
				socket.receive(packet);

				// Check if this message is mine, if so drop it silently
				if (packet.getAddress().equals(this.unicastSocket.getLocalAddress())
						&& packet.getPort() == this.unicastSocket.getLocalPort()) {
					logger.debug("Discarding multicast packet sent by myself.");
					continue;
				}

				byte[] data = new byte[packet.getLength()];
				System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
				List<ServiceMessage> messages = ServiceMessage.manyFromDatagram(data);

			
				if (messages.size() > 0) {
					if (messages.getFirst().isProbe()) {
						// Prepare responses for all services that requested a contact that we are
						// running
						List<ServiceMessage> replies = new ArrayList<ServiceMessage>();
						for (ServiceMessage m : messages) {
							ServiceMessage reply = this.discoveryProtocolsData.get(m.getServiceName());
							if (reply != null) {
								replies.add(reply);
							} 
						}

						for (byte[] bm : ServiceMessage.convertToMessage(replies, false)) {
							this.unicastSocket
									.send(new DatagramPacket(bm, bm.length, packet.getAddress(), packet.getPort()));
							logger.debug("Sent a reply back to announcer with " + bm.length + " bytes");
						}

					}

					// Actually, the announces/probes that we receive also contain information that
					// we can use
					// bootstrap our own protocols :)
					synchronized (protocolsWaiting) {
						if (protocolsWaiting.size() > 0) {
							for (ServiceMessage m : messages) {
								DiscoverableProtocol dp = this.protocolsWaiting.get(m.getServiceName());
								if (dp != null) {
									dp.addContact(m.getServiceHost());
									if (!dp.needsDiscovery())
										this.protocolsWaiting.remove(m.getServiceName());

									if (!dp.hasProtocolThreadStarted() && dp.readyToStart()) {
										dp.start();
										dp.startEventThread();
									}
								} 
							}
						}
					}

				} else {
					logger.warn("Could not deserialize any service message from received datagram.");
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
