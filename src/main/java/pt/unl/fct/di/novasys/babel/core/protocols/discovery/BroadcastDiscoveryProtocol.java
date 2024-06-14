package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;

/**
 * Broadcast discovery protocol extending local discovery protocol
 */
public class BroadcastDiscoveryProtocol extends LocalDiscoveryProtocol {
	private static final Logger logger = LogManager.getLogger(BroadcastDiscoveryProtocol.class);

	public static final int DEFAULT_PORT = 1025;
	public static final int ANOUNCEMENT_COOLDOWN = 1000;
	public static final short PROTO_ID = 32700;
	public static final String PROTO_NAME = "BabelBroadcastDiscovery";

	public static final String PAR_DISCOVERY_BROADCAST_INTERFACE = "babel.discovery.broadcast.interface";
	public static final String PAR_DISCOVERY_BROADCAST_PORT = "babel.discovery.broadcast.port";

	private int bcastPort;
	private Thread listeningThread;
	private Set<InetAddress> broadcastAddresses;

	public BroadcastDiscoveryProtocol() throws IOException, HandlerRegistrationException {
		super(PROTO_NAME, PROTO_ID);
	}

	@Override
	public void init(Properties props) throws HandlerRegistrationException, IOException {
		super.init(props);
		if (!props.containsKey(PAR_DISCOVERY_BROADCAST_INTERFACE) && props.containsKey(Babel.PAR_DEFAULT_INTERFACE))
			props.put(PAR_DISCOVERY_BROADCAST_INTERFACE, props.get(Babel.PAR_DEFAULT_INTERFACE));

		this.bcastPort = DEFAULT_PORT;
		if (props.containsKey(PAR_DISCOVERY_BROADCAST_PORT))
			this.bcastPort = Integer.parseInt(props.getProperty(PAR_DISCOVERY_BROADCAST_PORT));

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

		for (NetworkInterface n : broadcastInterfaces)
			for (InterfaceAddress a : n.getInterfaceAddresses())
				addInetSocketAddres(a.getBroadcast(), bcastPort);

		if (this.broadcastAddresses.size() == 0)
			throw new RuntimeException("No available broadcast address in network interface");

		InetAddress address = getAddressForSocket(props);

		DatagramSocket socket = setSocket(address, bcastPort, true);

		logger.info("BroadcastDiscoveryProtocol set up");

		listeningThread = new Thread(() -> listen(socket));
		listeningThread.start();

		logger.info("DiscoveryProtocol initialized");
	}
}
