package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.BabelRuntime;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;

/**
 * Multicast discovery protocol extending local discovery protocol
 */
public class MulticastDiscoveryProtocol extends LocalDiscoveryProtocol {
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
	private InetSocketAddress multicastSocketAddress;
	private Thread listeningMulticastThread;
	private Thread listeningUnicastThread;

	public MulticastDiscoveryProtocol() {
		super(PROTO_NAME, PROTO_ID);
	}

	@Override
	public void init(Properties props) throws HandlerRegistrationException, IOException {
		super.init(props);
		if (!props.containsKey(PAR_DISCOVERY_UNICAST_INTERFACE) && props.containsKey(BabelRuntime.PAR_DEFAULT_INTERFACE))
			props.put(PAR_DISCOVERY_UNICAST_INTERFACE, props.get(BabelRuntime.PAR_DEFAULT_INTERFACE));
		if (!props.containsKey(PAR_DISCOVERY_MULTICAST_INTERFACE) && props.containsKey(BabelRuntime.PAR_DEFAULT_INTERFACE))
			props.put(PAR_DISCOVERY_MULTICAST_INTERFACE, props.get(BabelRuntime.PAR_DEFAULT_INTERFACE));
		if (!props.containsKey(PAR_DISCOVERY_UNICAST_ADDRESS) && props.containsKey(BabelRuntime.PAR_DEFAULT_ADDRESS))
			props.put(PAR_DISCOVERY_UNICAST_ADDRESS, props.get(BabelRuntime.PAR_DEFAULT_ADDRESS));

		int targetPort = DEFAULT_MULTICAST_PORT;
		if (props.containsKey(PAR_DISCOVERY_MULTICAST_PORT)) {
			targetPort = Integer.parseInt(props.getProperty(PAR_DISCOVERY_MULTICAST_PORT));
		}

		multicastSocketAddress = addInetSocketAddress(props.getProperty(PAR_DISCOVERY_MULTICAST_ADDRESS, MULTICAST_ADDRESS), targetPort);

		NetworkInterface networkInterface;
		if (!props.containsKey(PAR_DISCOVERY_MULTICAST_INTERFACE)) {
			if(props.containsKey(PAR_DISCOVERY_UNICAST_INTERFACE)) {
				networkInterface = NetworkInterface.getByName(props.getProperty(PAR_DISCOVERY_UNICAST_INTERFACE));
			} else if(props.containsKey(PAR_DISCOVERY_UNICAST_ADDRESS)) {
				networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(props.getProperty(PAR_DISCOVERY_UNICAST_ADDRESS)));
			} else {
			// LAST RESORT :)
			// Bind to all?? interface that supports multicast
			networkInterface = NetworkInterface.networkInterfaces().filter(i -> {
				try {
					boolean supportsIPv4 = false;
					Enumeration<InetAddress> addresses = i.getInetAddresses();
					while (addresses.hasMoreElements())
						if (addresses.nextElement() instanceof Inet4Address) {
							supportsIPv4 = true; break;
						}
					
					return i.supportsMulticast() && supportsIPv4 && i.isUp() && !i.isLoopback();
				} catch (SocketException e) {
					e.printStackTrace();
					return false;
				}
			}).findAny().orElseThrow(() -> new RuntimeException("No network interface supports multicast"));
			}
		} else {
			networkInterface = NetworkInterface.getByName(props.getProperty(PAR_DISCOVERY_MULTICAST_INTERFACE));
		}

		multicastSocket = new MulticastSocket(targetPort);
		logger.debug("Multicast is going to use interface: " + networkInterface.getDisplayName());
		logger.debug("Selected interface supports multicast: " + networkInterface.supportsMulticast());
		multicastSocket.joinGroup(multicastSocketAddress, networkInterface);

		logger.info("DiscoveryProtocol set up");

		InetAddress address = getAddressForSocket(props);

		int unicastPort = DEFAULT_UNICAST_PORT;
		if (props.containsKey(PAR_DISCOVERY_UNICAST_PORT))
			unicastPort = Integer.parseInt(props.getProperty(PAR_DISCOVERY_UNICAST_PORT));

		DatagramSocket unicastSocket = setSocket(address, unicastPort, false);
		unicastSocket.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
		
		listeningMulticastThread = new Thread(() -> listen(multicastSocket));
		listeningUnicastThread = new Thread(() -> listen(unicastSocket));
		listeningMulticastThread.start();
		listeningUnicastThread.start();

		logger.info("DiscoveryProtocol initialized");
	}
}
