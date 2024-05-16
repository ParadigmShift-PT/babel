package pt.unl.fct.di.novasys.babel.protocols.discovery;

import static io.netty.buffer.Unpooled.buffer;
import static io.netty.buffer.Unpooled.wrappedBuffer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.protocol.DiscoverableProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.protocols.discovery.messages.AnouncementMessage;
import pt.unl.fct.di.novasys.babel.protocols.discovery.timers.AnoucementTimer;
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
    private static final ISerializer<AnouncementMessage> serializer = (ISerializer<AnouncementMessage>) AnouncementMessage.serializer;

    public static final String PAR_DISCOVERY_BROADCAST_INTERFACE = "babel.discocvery.broadcast.interface";
    public static final String PAR_DISCOVERY_BROADCAST_PORT = "babel.discovery.broadcast.port";
 
    private DatagramSocket socket;
    private int bcastPort;
    private Set<AnouncementMessage> servicesToAnounce;
    private Map<String, Set<DiscoverableProtocol>> servicesToListen;
    private Thread listeningThread;
    private Set<InetAddress> broadcastAddresses;

    public BroadcastDiscoveryProtocol(Properties props) throws IOException, HandlerRegistrationException {
        super(PROTO_NAME, PROTO_ID);

        servicesToAnounce = ConcurrentHashMap.newKeySet();
        servicesToListen = new HashMap<>();

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
                byte[] data = messageBuffer.array();
                int len = messageBuffer.readableBytes();
            	for(InetAddress a: this.broadcastAddresses) {       
	                DatagramPacket packet = new DatagramPacket(data, len, new InetSocketAddress(a, bcastPort));
	                socket.send(packet);
	                logger.info("Anounced " + message.getServiceName());
                }
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
                socket.receive(packet);
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
