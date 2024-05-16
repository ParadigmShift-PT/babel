package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import static io.netty.buffer.Unpooled.wrappedBuffer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.messages.ServiceMessage;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.timers.AnoucementTimer;
import pt.unl.fct.di.novasys.network.ISerializer;
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

    public static final int DEFAULT_PORT = 1025;
    public static final String MULTICAST_ADDRESS = "233.138.122.123";
    public static final int ANOUNCEMENT_COOLDOWN = 1000;
    public static final short PROTO_ID = 32767;
    public static final String PROTO_NAME = "BabelMulticastDiscovery";
    public static final int DATAGRAM_SIZE = 65535;
    @SuppressWarnings("unchecked")
    private static final ISerializer<ServiceMessage> serializer = (ISerializer<ServiceMessage>) ServiceMessage.serializer;

    public static final String PAR_DISCOVERY_MULTICAST_INTERFACE = "babel.discocvery.multicast.interface";
    public static final String PAR_DISCOVERY_MULTICAST_ADDRESS = "babel.discovery.multicast.addr";
    public static final String PAR_DISCOVERY_MULTICAST_PORT = "babel.discovery.multicast.port";
    public static final String PAR_DISCOVERY_UNICAST_INTERFACE = "babel.discovery.unicast.interface";
    public static final String PAR_DISCOVERY_UNICAST_ADDRESS = "babel.discovery.unicast.address";
 
    private MulticastSocket multicastSocket;
    private DatagramSocket unicastSocket;
    private Map<String, byte[]> servicesToReplyMessage;
    private Map<String, WaitingContact> servicesWaiting;
    private InetSocketAddress multicastSocketAddress;
    private Host myself;
    
    private Thread listeningMulticastThread;
    private Thread listeningUnicastThread;

    public MulticastDiscoveryProtocol() {
        super(PROTO_NAME, PROTO_ID);
    }
        
    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
    	servicesToReplyMessage = new ConcurrentHashMap<>();
        servicesWaiting = new ConcurrentHashMap<>();

        int targetPort = DEFAULT_PORT;
        if (props.contains(PAR_DISCOVERY_MULTICAST_PORT)) {
            targetPort = Integer.parseInt(props.getProperty(PAR_DISCOVERY_MULTICAST_PORT));
        }

        multicastSocketAddress = new InetSocketAddress(props.getProperty(PAR_DISCOVERY_MULTICAST_ADDRESS, MULTICAST_ADDRESS), targetPort);
   
        NetworkInterface networkInterface;
        if (!props.contains(PAR_DISCOVERY_MULTICAST_INTERFACE)) {
            // Bind to all?? interface that supports multicast
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
            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(props.getProperty(PAR_DISCOVERY_MULTICAST_INTERFACE)));
        }
        multicastSocket = new MulticastSocket(targetPort);
        multicastSocket.joinGroup(multicastSocketAddress, networkInterface);

        registerTimerHandler(AnoucementTimer.TIMER_ID, this::announce);

        logger.info("DiscoveryProtocol set up");
        
        InetAddress address = null;
        
        if(props.contains(PAR_DISCOVERY_UNICAST_ADDRESS)) {
        	address = InetAddress.ofLiteral(props.getProperty(PAR_DISCOVERY_UNICAST_ADDRESS));
        } else if(props.contains(PAR_DISCOVERY_UNICAST_INTERFACE)) {
        	List<InterfaceAddress> l =  NetworkInterface.getByName(props.getProperty(PAR_DISCOVERY_UNICAST_INTERFACE)).getInterfaceAddresses();
        	for(InterfaceAddress a: l) {
        		if (a.getAddress() != null) {
        			address = a.getAddress();
        			break;
        		}
        		if(address != null)
        			break;
        	}
        } else {
        	 Iterator<NetworkInterface> iterator = NetworkInterface.networkInterfaces().distinct().iterator();
         	while(iterator.hasNext()) {
         		NetworkInterface n = iterator.next();
             	if(!n.isLoopback() && !n.isVirtual() && n.isUp() && !n.isPointToPoint()) {
             		for(InterfaceAddress a: n.getInterfaceAddresses()) {
             			if(a.getAddress() != null)
             				address = a.getAddress();
             				break;
             		}
             		if(address != null)
             			break;
             	}
         	}
        }
        
        myself = new Host(address, targetPort);

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
    
 
    /**
     * Listens for incoming announcements
     * 
     * Since no support exists in the network layer, this method should be called
     * in a thread manually for now. Eventually this should be moved to netty.
     */
    private void listen(DatagramSocket socket) {
        while (true) {
            var byteBuffer = new byte[DATAGRAM_SIZE];
            var messageBuffer = wrappedBuffer(byteBuffer);
            messageBuffer.clear();
            try {
                var packet = new DatagramPacket(byteBuffer, DATAGRAM_SIZE);
                socket.receive(packet);
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
                    socket.send(replyPacket);
                    logger.info("Replied");
                } else {
                    var serviceWaiting = servicesWaiting.remove(message.getServiceName());
                    if (serviceWaiting == null) {
                        continue;
                    }
                    serviceWaiting.proto().setContact(message.getServiceHost());
                    serviceWaiting.proto().setWhispererContact(message.getDiscoveryHost());
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

    public void serviceSearchAnounceRequest(String serviceName, SelfConfigurableProtocol sourceProtocol, Host host)
            throws IOException {
        logger.info("Got search request for " + serviceName);
        byte[] messageBytes = new byte[DATAGRAM_SIZE];
        ByteBuf messageByteBuf = wrappedBuffer(messageBytes);
        messageByteBuf.clear();
        ServiceMessage message = new ServiceMessage(serviceName, host, myself, true);
        serializer.serialize(message, messageByteBuf);

        servicesWaiting.put(serviceName, new WaitingContact(messageBytes, sourceProtocol));
    }

    public Host getMyself() {
        return myself;
    }

}
