package pt.unl.fct.di.novasys.babel.protocols.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.protocols.DiscoverableProtocol;
import pt.unl.fct.di.novasys.babel.protocols.discovery.messages.AnouncementMessage;
import pt.unl.fct.di.novasys.babel.protocols.discovery.timers.AnoucementTimer;
import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.*;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Protocol to be included in the Babel framework in the future
 * 
 * It does not depend on the network-layer as Babel does right now since it does
 * not support any other comunications besides TCP. In the future it might be
 * expanded
 */
public class DiscoveryProtocol extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(DiscoveryProtocol.class);

    public static final int DEFAULT_PORT = 19348;
    public static final String MULTICAST_ADDRESS = "233.138.122.123";
    public static final int ANOUNCEMENT_COOLDOWN = 1000;
    public static final short PROTO_ID = 603;
    public static final String PROTO_NAME = "BabelDiscovery";
    public static final int DATAGRAM_SIZE = 65535;
    @SuppressWarnings("unchecked")
    private static final ISerializer<AnouncementMessage> serializer = (ISerializer<AnouncementMessage>) AnouncementMessage.serializer;

    private MulticastSocket multicastSocket;
    private Set<AnouncementMessage> servicesToAnounce;
    private Map<String, Set<DiscoverableProtocol>> servicesToListen;
    private Thread listeningThread;
    private InetSocketAddress multicastSocketAddress;

    public DiscoveryProtocol(Properties props) throws IOException, HandlerRegistrationException {
        super(PROTO_NAME, PROTO_ID);

        servicesToAnounce = ConcurrentHashMap.newKeySet();
        servicesToListen = new HashMap<>();

        String targetPortString = props.getProperty("babel_discovery_port");
        int targetPort = DEFAULT_PORT;
        if (targetPortString != null) {
            targetPort = Integer.parseInt(targetPortString);
        }

        multicastSocketAddress = new InetSocketAddress(
                props.getProperty("babel_multicast_address", MULTICAST_ADDRESS), targetPort);
        String networkInterfaceString = props.getProperty("babel_multicast_interface");
        NetworkInterface networkInterface;
        if (networkInterfaceString == null) {
            // Bind to any interface that supports multicast
            networkInterface = NetworkInterface.networkInterfaces()
                    .filter(i -> {
                        try {
                            return i.supportsMulticast();
                        } catch (SocketException e) {
                            e.printStackTrace();
                            return false;
                        }
                    })
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("No network interface supports multicast"));
        } else {
            InetAddress interfaceAddress = InetAddress.getByName(networkInterfaceString);
            networkInterface = NetworkInterface.getByInetAddress(interfaceAddress);
        }
        multicastSocket = new MulticastSocket(targetPort);
        multicastSocket.joinGroup(multicastSocketAddress, networkInterface);

        registerTimerHandler(AnoucementTimer.TIMER_ID, this::announce);

        logger.info("DiscoveryProtocol set up");
    }

    @Override
    public void start() {
        setupTimer(new AnoucementTimer(), ANOUNCEMENT_COOLDOWN);

        listeningThread = new Thread(this::listen);
        listeningThread.start();
        logger.info("DiscoveryProtocol initialized");
    }

    private void announce(AnoucementTimer timer, long timerId) {
        try {
            for (AnouncementMessage message : servicesToAnounce) {
                ByteBuf messageBuffer = buffer();
                serializer.serialize(message, messageBuffer);
                DatagramPacket packet = new DatagramPacket(messageBuffer.array(), messageBuffer.readableBytes(),
                        multicastSocketAddress.getAddress(), multicastSocketAddress.getPort());
                multicastSocket.send(packet);
                logger.info("Anounced " + message.getServiceName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupTimer(timer, ANOUNCEMENT_COOLDOWN);
    }

    /**
     * Listens for incoming anouncements
     * 
     * Since no support exists in the network layer, this method should be called
     * in a thread manually for now. Eventually this should be moved to netty.
     */
    private void listen() {
        while (true) {
            try {
                var byteBuffer = new byte[DATAGRAM_SIZE];
                var messageBuffer = wrappedBuffer(byteBuffer);
                var packet = new DatagramPacket(byteBuffer, DATAGRAM_SIZE);
                multicastSocket.receive(packet);
                var message = serializer.deserialize(messageBuffer);
                logger.info("Got " + message.getServiceName() + "@" + message.getServiceHost().toString());
                var protosWaiting = servicesToListen.get(message.getServiceName());
                if (protosWaiting != null) {
                    protosWaiting.forEach((proto) -> proto.setContact(message.getServiceHost()));
                }
                messageBuffer.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void serviceListenRequest(String serviceName, DiscoverableProtocol sourceProtocol) {
        var protosWaiting = servicesToListen.get(serviceName);
        if (protosWaiting == null) {
            protosWaiting = new HashSet<>();
            servicesToListen.put(serviceName, protosWaiting);
        }
        protosWaiting.add(sourceProtocol);
    }

    public void serviceUnlistenRequest(String serviceName, DiscoverableProtocol sourceProtocol) {
        var protosWaiting = servicesToListen.get(serviceName);
    }

    public void serviceAnounceRequest(String serviceName, Host serviceHost) {
        logger.info("Anouncing " + serviceName + "@" + serviceHost.toString());
        var message = new AnouncementMessage(serviceName, serviceHost);
        servicesToAnounce.add(message);
    }
}
