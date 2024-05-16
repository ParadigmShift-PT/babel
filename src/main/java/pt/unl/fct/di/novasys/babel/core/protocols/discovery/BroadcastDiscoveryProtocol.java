package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import static io.netty.buffer.Unpooled.wrappedBuffer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
public class BroadcastDiscoveryProtocol extends DiscoveryProtocol {
    private static final Logger logger = LogManager.getLogger(BroadcastDiscoveryProtocol.class);

    public static final int DEFAULT_PORT = 1025;
    public static final int ANOUNCEMENT_COOLDOWN = 1000;
    public static final short PROTO_ID = 32767;
    public static final String PROTO_NAME = "BabelBroadcastDiscovery";
    public static final int DATAGRAM_SIZE = 65535;
    @SuppressWarnings("unchecked")
    private static final ISerializer<ServiceMessage> serializer = (ISerializer<ServiceMessage>) ServiceMessage.serializer;

    public static final String PAR_DISCOVERY_BROADCAST_INTERFACE = "babel.discocvery.broadcast.interface";
    public static final String PAR_DISCOVERY_BROADCAST_PORT = "babel.discovery.broadcast.port";
 
    private DatagramSocket socket;
    private int bcastPort;
    private Map<String, byte[]> servicesToReplyMessage;
    private Map<String, WaitingContact> servicesWaiting;
    private Thread listeningThread;
    private Set<InetAddress> broadcastAddresses;
    private Host myself;


    public BroadcastDiscoveryProtocol() throws IOException, HandlerRegistrationException {
        super(PROTO_NAME, PROTO_ID);
    }
    
    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
    	servicesToReplyMessage = new ConcurrentHashMap<>();
        servicesWaiting = new ConcurrentHashMap<>();

        this.bcastPort = DEFAULT_PORT;
        if (props.contains(PAR_DISCOVERY_BROADCAST_PORT)) {
           this.bcastPort = Integer.parseInt(props.getProperty(PAR_DISCOVERY_BROADCAST_PORT));
        }

        Set<NetworkInterface>broadcastInterfaces = new HashSet<NetworkInterface>();
        
        if (!props.contains(PAR_DISCOVERY_BROADCAST_INTERFACE)) {
            Iterator<NetworkInterface> iterator = NetworkInterface.networkInterfaces().distinct().iterator();
        	while(iterator.hasNext()) {
        		NetworkInterface n = iterator.next();
            	if(!n.isLoopback() && !n.isVirtual() && n.isUp()) {
            		broadcastInterfaces.add(n);
            	}
        	}
        } else {
            broadcastInterfaces.add(NetworkInterface.getByName(props.getProperty(PAR_DISCOVERY_BROADCAST_INTERFACE)));
        }
        
        for(NetworkInterface n: broadcastInterfaces) 
        	for(InterfaceAddress a: n.getInterfaceAddresses()) 
        		this.broadcastAddresses.add(a.getBroadcast());
        
        if(this.broadcastAddresses.size() == 0) {
        	throw new RuntimeException("No available broadcast address in network interface");
        }
        
        socket = new DatagramSocket(bcastPort);
        socket.setBroadcast(true);
      
        registerTimerHandler(AnoucementTimer.TIMER_ID, this::announce);

        logger.info("BroadcastDiscoveryProtocol set up");
        
        listeningThread = new Thread(() -> listen(socket));
        listeningThread.start();

        logger.info("DiscoveryProtocol initialized");
    }

    
    private void announce(AnoucementTimer timer, long timerId) {
        logger.info("Firing anouncements");
        try {
            for (var message : servicesWaiting.entrySet()) {
                byte[] anouncement = message.getValue().anouncement();
                for(InetAddress a: this.broadcastAddresses) {  
	                DatagramPacket packet = new DatagramPacket(anouncement, anouncement.length, new InetSocketAddress(a, bcastPort));
	                socket.send(packet);
	                logger.info("Anounced search for " + message.getKey());
                }
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
