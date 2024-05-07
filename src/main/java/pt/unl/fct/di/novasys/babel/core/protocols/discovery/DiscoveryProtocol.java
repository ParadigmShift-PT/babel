package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.*;

import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.SelfConfiguredProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.messages.ServiceMessage;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.timers.AnoucementTimer;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Protocol to be included in the Babel framework in the future
 * 
 * It does not depend on the network-layer as Babel does right now since it does
 * not support any other comunications besides TCP. In the future it might be
 * expanded.
 */
public class DiscoveryProtocol extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(DiscoveryProtocol.class);

    public static final int DEFAULT_PORT = 19348;
    public static final int UNICAST_PORT = 19349;
    public static final String MULTICAST_ADDRESS = "233.138.122.123";
    public static final int ANOUNCEMENT_COOLDOWN = 1000;
    public static final short PROTO_ID = 603;
    public static final String PROTO_NAME = "BabelDiscovery";
    public static final int DATAGRAM_SIZE = 65507;
    @SuppressWarnings("unchecked")
    private static final ISerializer<ServiceMessage> serializer = (ISerializer<ServiceMessage>) ServiceMessage.serializer;

    private MulticastSocket multicastSocket;
    private DatagramSocket datagramSocket;
    private Map<String, byte[]> servicesToReplyMessage;
    private Map<String, WaitingContact> servicesWaiting;
    private Thread listeningMulticastThread;
    private Thread listeningUnicastThread;
    private InetSocketAddress multicastSocketAddress;
    private Host myself;

    public DiscoveryProtocol() {
        super(PROTO_NAME, PROTO_ID);
    }

    @Override
    public void init(Properties props) throws IOException, HandlerRegistrationException {
        servicesToReplyMessage = new ConcurrentHashMap<>();
        servicesWaiting = new ConcurrentHashMap<>();

        String targetPortString = props.getProperty("BabelDiscovery.Port");
        int targetPort = DEFAULT_PORT;
        if (targetPortString != null) {
            targetPort = Integer.parseInt(targetPortString);
        }

        multicastSocketAddress = new InetSocketAddress(
                props.getProperty("BabelDiscovery.Multicast.Address", MULTICAST_ADDRESS), targetPort);
        String networkInterfaceString = props.getProperty("BabelDiscovery.Multicast.Interface");
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
            networkInterface = NetworkInterface.getByName(networkInterfaceString);
        }
        var possibleAdresses = networkInterface.getInetAddresses();
        while (possibleAdresses.hasMoreElements() && myself == null) {
            var possibleAdress = possibleAdresses.nextElement();
            if (possibleAdress instanceof Inet4Address) {
                myself = new Host(possibleAdress, UNICAST_PORT);
            }
        }
        multicastSocket = new MulticastSocket(targetPort);
        multicastSocket.joinGroup(multicastSocketAddress, networkInterface);

        datagramSocket = new DatagramSocket(UNICAST_PORT);

        registerTimerHandler(AnoucementTimer.TIMER_ID, this::announce);

        setupPeriodicTimer(new AnoucementTimer(), ANOUNCEMENT_COOLDOWN, ANOUNCEMENT_COOLDOWN);

        listeningMulticastThread = new Thread(this::listenInMulticast);
        listeningUnicastThread = new Thread(this::listenInUnicast);
        listeningMulticastThread.start();
        listeningUnicastThread.start();

        logger.info("DiscoveryProtocol initialized");
    }

    private void announce(AnoucementTimer timer, long timerId) {
        logger.info("Firing anouncements");
        try {
            for (var message : servicesWaiting.entrySet()) {
                byte[] anouncement = message.getValue().anouncement();
                DatagramPacket packet = new DatagramPacket(anouncement, anouncement.length,
                        multicastSocketAddress.getAddress(), multicastSocketAddress.getPort());
                multicastSocket.send(packet);
                logger.info("Anounced search for " + message.getKey());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenInUnicast() {
        while (true) {
            var byteBuffer = new byte[DATAGRAM_SIZE];
            var messageBuffer = wrappedBuffer(byteBuffer);
            messageBuffer.clear();
            try {
                var packet = new DatagramPacket(byteBuffer, DATAGRAM_SIZE);
                datagramSocket.receive(packet);
                messageBuffer.setIndex(0, packet.getLength());
                var message = serializer.deserialize(messageBuffer);
                if (message.isSearching()) {
                    logger.info("Got search for " + message.getServiceName() + " from "
                            + message.getServiceHost().toString());
                    var replyMessage = servicesToReplyMessage.get(message.getServiceName());
                    if (replyMessage == null) {
                        continue;
                    }
                    Host destination = message.getDiscoveryHost();
                    DatagramPacket replyPacket = new DatagramPacket(replyMessage, replyMessage.length,
                            destination.getAddress(), destination.getPort());
                    datagramSocket.send(replyPacket);
                    logger.info("Replied");
                } else {
                    var serviceWaiting = servicesWaiting.remove(message.getServiceName());
                    if (serviceWaiting == null) {
                        continue;
                    }
                    serviceWaiting.proto().setContact(message.getServiceHost());
                    Babel.getInstance().setupSelfConfiguration(serviceWaiting.proto());
                }
                messageBuffer.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Listens for incoming anouncements
     * 
     * Since no support exists in the network layer, this method should be called
     * in a thread manually for now. Eventually this should be moved to netty.
     */
    private void listenInMulticast() {
        while (true) {
            var byteBuffer = new byte[DATAGRAM_SIZE];
            var messageBuffer = wrappedBuffer(byteBuffer);
            messageBuffer.clear();
            try {
                var packet = new DatagramPacket(byteBuffer, DATAGRAM_SIZE);
                multicastSocket.receive(packet);
                messageBuffer.setIndex(0, packet.getLength());
                var message = serializer.deserialize(messageBuffer);
                if (message.isSearching()) {
                    logger.info("Got search for " + message.getServiceName() + " from "
                            + message.getServiceHost().toString());
                    var replyMessage = servicesToReplyMessage.get(message.getServiceName());
                    if (replyMessage == null) {
                        continue;
                    }
                    Host destination = message.getDiscoveryHost();
                    DatagramPacket replyPacket = new DatagramPacket(replyMessage, replyMessage.length,
                            destination.getAddress(), destination.getPort());
                    multicastSocket.send(replyPacket);
                    logger.info("Replied");
                } else {
                    var serviceWaiting = servicesWaiting.remove(message.getServiceName());
                    if (serviceWaiting == null) {
                        continue;
                    }
                    serviceWaiting.proto().setContact(message.getServiceHost());
                    babel.setupSelfConfiguration(serviceWaiting.proto());
                }
                messageBuffer.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void serviceSearchListenRequest(String serviceName, Host host) throws IOException {
        logger.info("Got search reply request for " + serviceName);
        byte[] messageBytes = new byte[DATAGRAM_SIZE];
        ByteBuf messageByteBuf = wrappedBuffer(messageBytes);
        messageByteBuf.clear();
        ServiceMessage message = new ServiceMessage(serviceName, host, myself, false);
        serializer.serialize(message, messageByteBuf);

        servicesToReplyMessage.put(serviceName, messageBytes);
    }

    public void serviceSearchAnounceRequest(String serviceName, SelfConfiguredProtocol sourceProtocol, Host host)
            throws IOException {
        logger.info("Got search request for " + serviceName);
        byte[] messageBytes = new byte[DATAGRAM_SIZE];
        ByteBuf messageByteBuf = wrappedBuffer(messageBytes);
        messageByteBuf.clear();
        ServiceMessage message = new ServiceMessage(serviceName, host, myself, true);
        serializer.serialize(message, messageByteBuf);

        servicesWaiting.put(serviceName, new WaitingContact(messageBytes, sourceProtocol));
    }
}
