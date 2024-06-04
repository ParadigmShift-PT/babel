package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.Babel;
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
public class BroadcastDiscoveryProtocol extends DiscoveryProtocol {
	private static final Logger logger = LogManager.getLogger(BroadcastDiscoveryProtocol.class);

	public static final int DEFAULT_PORT = 1025;
	public static final int ANOUNCEMENT_COOLDOWN = 1000;
	public static final short PROTO_ID = 32700;
	public static final String PROTO_NAME = "BabelBroadcastDiscovery";

	public static final String PAR_DISCOVERY_BROADCAST_INTERFACE = "babel.discovery.broadcast.interface";
	public static final String PAR_DISCOVERY_BROADCAST_PORT = "babel.discovery.broadcast.port";

	private DatagramSocket socket;
	private int bcastPort;
	private Map<String, ServiceMessage> discoveryProtocolsData;
	private Map<String, DiscoverableProtocol> protocolsWaiting;
	private Thread listeningThread;
	private Set<InetAddress> broadcastAddresses;
	private Host discoveryHost;

	public BroadcastDiscoveryProtocol() throws IOException, HandlerRegistrationException {
		super(PROTO_NAME, PROTO_ID);
	}

	@Override
	public void init(Properties props) throws HandlerRegistrationException, IOException {
		if(!props.containsKey(PAR_DISCOVERY_BROADCAST_INTERFACE) && props.containsKey(Babel.PAR_DEFAULT_INTERFACE))
			props.put(PAR_DISCOVERY_BROADCAST_INTERFACE, props.get(Babel.PAR_DEFAULT_INTERFACE));
		
		discoveryProtocolsData = new HashMap<String, ServiceMessage>();
		protocolsWaiting = new HashMap<String, DiscoverableProtocol>();

		this.bcastPort = DEFAULT_PORT;
		if (props.containsKey(PAR_DISCOVERY_BROADCAST_PORT)) {
			this.bcastPort = Integer.parseInt(props.getProperty(PAR_DISCOVERY_BROADCAST_PORT));
		}

		Set<NetworkInterface> broadcastInterfaces = new HashSet<NetworkInterface>();

		if (!props.containsKey(PAR_DISCOVERY_BROADCAST_INTERFACE)) {
			Iterator<NetworkInterface> iterator = NetworkInterface.networkInterfaces().distinct().iterator();
			while (iterator.hasNext()) {
				NetworkInterface n = iterator.next();
				if (!n.isLoopback() && !n.isVirtual() && n.isUp()) {
					broadcastInterfaces.add(n);
				}
			}
		} else {
			broadcastInterfaces.add(NetworkInterface.getByName(props.getProperty(PAR_DISCOVERY_BROADCAST_INTERFACE)));
		}

		this.broadcastAddresses = new HashSet<InetAddress>();
		for (NetworkInterface n : broadcastInterfaces)
			for (InterfaceAddress a : n.getInterfaceAddresses())
				this.broadcastAddresses.add(a.getBroadcast());

		if (this.broadcastAddresses.size() == 0) {
			throw new RuntimeException("No available broadcast address in network interface");
		}

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
		
		socket = new DatagramSocket(bcastPort, address);
		discoveryHost = new Host(socket.getLocalAddress(), socket.getLocalPort());
		socket.setBroadcast(true);

		registerTimerHandler(AnoucementTimer.TIMER_ID, this::announce);

		logger.info("BroadcastDiscoveryProtocol set up");

		listeningThread = new Thread(() -> listen(socket));
		listeningThread.start();

		logger.info("DiscoveryProtocol initialized");
	}

	
	private void announce(AnoucementTimer timer, long timerId) {
		logger.info("Firing anouncements");

		if (protocolsWaiting.size() == 0) {
			return;
		}

		List<ServiceMessage> pendingServices = new ArrayList<ServiceMessage>();
		for (String protocol : protocolsWaiting.keySet()) {
			pendingServices.add(this.discoveryProtocolsData.get(protocol));
		}

		try {

			List<byte[]> announces = ServiceMessage.convertToMessage(pendingServices, true);
			for (byte[] m : announces) {
				for (InetAddress a : this.broadcastAddresses) {
				socket.send(new DatagramPacket(m, m.length, new InetSocketAddress(a, bcastPort)));
				logger.info("Anounced search for broadcast address " + a);
				}
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
		if (!dcProto.needsDiscovery()) {
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
				DatagramPacket packet = new DatagramPacket(new byte[ServiceMessage.DATAGRAM_SIZE], ServiceMessage.DATAGRAM_SIZE);
				socket.receive(packet);

				// Check if this message is mine, if so drop it silently
				if (packet.getAddress().equals(this.socket.getLocalAddress())
						&& packet.getPort() == this.socket.getLocalPort()) {
					logger.debug("Discarding multicast packet sent by myself.");
					continue;
				}

				logger.debug("Receive a message with " + packet.getLength() + " bytes.");
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
							if (reply != null)
								replies.add(reply);
						}

						for (byte[] bm : ServiceMessage.convertToMessage(replies, false)) {
							this.socket.send(new DatagramPacket(bm, bm.length, packet.getAddress(), packet.getPort()));
						}

					}

					// Actually, the announces/probes that we receive also contain information that
					// we can use
					// bootstrap our own protocols :)
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

				} else {
					logger.warn("Could not deserialize any service message from received datagram.");
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
