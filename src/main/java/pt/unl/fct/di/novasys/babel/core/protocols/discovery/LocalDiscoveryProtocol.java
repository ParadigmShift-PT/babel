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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.DiscoverableProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.messages.ServiceMessage;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.requests.FoundServiceReply;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.requests.RequestDiscovery;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.timers.AnoucementTimer;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Abstract class for local discovery protocols using broadcast and multicast.
 * 
 * It does not depend on the network-layer as Babel does right now since it does
 * not support any other communications besides TCP. In the future it might be
 * expanded
 */
public abstract class LocalDiscoveryProtocol extends DiscoveryProtocol {
    private static final Logger logger = LogManager.getLogger(LocalDiscoveryProtocol.class);

    public static final int DEFAULT_PORT = 1025;
    public static final int ANOUNCEMENT_COOLDOWN = 1000;

    private DatagramSocket socket;
    private Map<String, ServiceMessage> discoveryProtocolsData;
    private Map<String, DiscoverableProtocol> protocolsWaiting;
    private Map<String, Short> runningProtcolsWaiting;
    private Host discoveryHost;
    private Set<InetSocketAddress> socketAddresses;
    private Set<ServiceMessage> pendingServices;

    public LocalDiscoveryProtocol(String protoName, short protoId) {
        super(protoName, protoId);
    }

    protected InetAddress getAddressForSocket(Properties props) throws IOException {
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

        return address;
    }

    protected InetSocketAddress addInetSocketAddres(InetAddress address, int port) {
        var socketAddress = new InetSocketAddress(address, port);
        socketAddresses.add(socketAddress);
        return socketAddress;
    }

    protected InetSocketAddress addInetSocketAddres(String address, int port) {
        var socketAddress = new InetSocketAddress(address, port);
        socketAddresses.add(socketAddress);
        return socketAddress;
    }

    protected DatagramSocket setSocket(InetAddress address, int port, boolean broadcast) throws SocketException {
        socket = new DatagramSocket(port, address);
        discoveryHost = new Host(address, port);
        socket.setBroadcast(broadcast);
        return socket;
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        discoveryProtocolsData = new ConcurrentHashMap<>();
        protocolsWaiting = new ConcurrentHashMap<>();
        socketAddresses = new HashSet<>();
        pendingServices = new HashSet<>();
        runningProtcolsWaiting = new ConcurrentHashMap<>();

        registerTimerHandler(AnoucementTimer.TIMER_ID, this::announce);

        setupPeriodicTimer(new AnoucementTimer(), ANOUNCEMENT_COOLDOWN, ANOUNCEMENT_COOLDOWN);
    }

    @Override
    public void registerProtocol(DiscoverableProtocol dcProto) {
        this.discoveryProtocolsData.put(dcProto.getProtoName(),
                new ServiceMessage(dcProto.getProtoName(), dcProto.getMyself(), discoveryHost));
        if (!dcProto.needsDiscovery()) {
            logger.debug("Registered protocol " + dcProto.getProtoName());
            this.protocolsWaiting.put(dcProto.getProtoName(), dcProto);
        }
    }

    private void announce(AnoucementTimer timer, long timerId) {
        logger.info("Firing anouncements");

        if (protocolsWaiting.size() == 0 && runningProtcolsWaiting.size() == 0) {
            logger.debug("No protocols waiting registered");
            return;
        }

        pendingServices.clear();
        for (String protocol : protocolsWaiting.keySet()) {
            pendingServices.add(this.discoveryProtocolsData.get(protocol));
            logger.debug("Added protocol " + protocol + " to send buffer");
        }
        for (String protocol : runningProtcolsWaiting.keySet()) {
            pendingServices.add(this.discoveryProtocolsData.get(protocol));
            logger.debug("Added protocol " + protocol + " to send buffer");
        }

        /*
         * if (!pendingServices.equals(protocolsWaiting.keySet())) {
         * pendingServices.removeIf((service) ->
         * !protocolsWaiting.keySet().contains(service.getServiceName()));
         * for (String protocol : protocolsWaiting.keySet()) {
         * pendingServices.add(this.discoveryProtocolsData.get(protocol));
         * logger.debug("Added protocol " + protocol + " to send buffer");
         * }
         * }
         */

        try {
            List<byte[]> announces = ServiceMessage.convertToMessage(pendingServices, true);
            for (byte[] m : announces) {
                for (var socketAddress : socketAddresses) {
                    socket.send(new DatagramPacket(m, m.length, socketAddress));
                    logger.debug("Sent one message to " + socketAddress.getAddress() + ":"
                            + socketAddress.getPort() + " from " + socket.getLocalAddress() + ":"
                            + socket.getLocalPort());
                }
            }

        } catch (Exception e) {
            logger.error("Could not send announcements.");
            e.printStackTrace();
        }
    }

    /**
     * Listens for incoming announcements
     * 
     * Since no support exists in the network layer, this method should be called in
     * a thread manually for now. Eventually this should be moved to netty.
     */
    protected void listen(DatagramSocket socket) {
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(new byte[ServiceMessage.DATAGRAM_SIZE],
                        ServiceMessage.DATAGRAM_SIZE);
                socket.receive(packet);
                logger.debug("Discovery has received a message from " + packet.getAddress().getHostAddress() + ":"
                        + packet.getPort() +
                        "on " + (socket instanceof MulticastSocket ? "MulticastSocket" : "UnicastSocket"));

                // Check if this message is mine, if so drop it silently
                if (packet.getAddress().equals(this.socket.getLocalAddress())
                        && packet.getPort() == this.socket.getLocalPort()) {
                    logger.debug("Discarding multicast packet sent by myself.");
                    continue;
                }

                logger.debug("Receive a message with " + packet.getLength() + " bytes.");
                List<ServiceMessage> messages = ServiceMessage
                        .manyFromDatagram(Arrays.copyOfRange(packet.getData(), 0, packet.getLength()));

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
                    synchronized (protocolsWaiting) {
                        if (protocolsWaiting.size() > 0) {
                            for (ServiceMessage m : messages) {
                                DiscoverableProtocol dp = this.protocolsWaiting.get(m.getServiceName());
                                if (dp != null) {
                                    synchronized (dp) {
                                        if (dp.hasProtocolThreadStarted()) {
                                            this.protocolsWaiting.remove(m.getServiceName());
                                            continue;
                                        }

                                        dp.addContact(m.getServiceHost());
                                        if (!dp.needsDiscovery())
                                            this.protocolsWaiting.remove(m.getServiceName());

                                        if (!dp.hasProtocolThreadStarted() && dp.readyToStart()) {
                                            dp.start();
                                            dp.startEventThread();
                                        }
                                    }
                                }
                                Short dpID = this.runningProtcolsWaiting.get(m.getServiceName());
                                if (dpID != null)
                                    sendReply(new FoundServiceReply(m.getServiceHost()), dpID);
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

    public void uponRequestDiscovery(RequestDiscovery request, short sourceProtocol) {
        logger.debug("Received discovery request for " + request.getServiceName() + " from proto " + sourceProtocol);
        if (request.getListen()) {
            this.discoveryProtocolsData.put(request.getServiceName(),
                    new ServiceMessage(request.getProtoName(), request.getMyself(), discoveryHost));
            runningProtcolsWaiting.put(request.getServiceName(), sourceProtocol);
        } else
            runningProtcolsWaiting.remove(request.getServiceName());
    }
}
