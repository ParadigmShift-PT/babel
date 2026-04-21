package pt.unl.fct.di.novasys.babel.core.protocols.discovery.messages;

import static io.netty.buffer.Unpooled.wrappedBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * In the future, serialization of this message might be changed to support IPv6 addresses which are laking in Babel
 */
public class ServiceMessage {
    public static final int DATAGRAM_SIZE = 65507;
	
	public static final byte[] probeID = {'B','P','D','P'}; //Babel Protocol Discovery Probe
	public static final byte[] announceID = {'B','P','D','A'}; //Babel Protocol Discovery Announce
	
	private final byte[] id;
    private final String serviceName;
    private final Host serviceHost;
    private final Host discoveryHost;
    private final boolean isDiscoverable;

    /**
     * Creates a probe-type ServiceMessage advertising the given service.
     *
     * @param serviceName    the logical name of the service being advertised
     * @param serviceHost    the host at which the service accepts connections
     * @param discoveryHost  the host on which this node's discovery socket is listening
     * @param isDiscoverable {@code true} if this service is willing to be contacted by peers
     */
    public ServiceMessage(String serviceName, Host serviceHost, Host discoveryHost, boolean isDiscoverable) {
    	this.id = ServiceMessage.probeID;
        this.serviceName = serviceName;
        this.serviceHost = serviceHost;
        this.discoveryHost = discoveryHost;
        this.isDiscoverable = isDiscoverable;
    }

    /**
     * Creates a ServiceMessage with an explicit message-type identifier, used during deserialisation.
     *
     * @param rid            4-byte identifier — must equal {@link #probeID} or {@link #announceID}
     * @param serviceName    the logical name of the service
     * @param serviceHost    the host at which the service accepts connections
     * @param discoveryHost  the host on which this node's discovery socket is listening
     * @param isDiscoverable {@code true} if this service is willing to be contacted by peers
     * @throws Exception if {@code rid} is neither a probe nor an announce identifier
     */
    public ServiceMessage(byte[] rid, String serviceName, Host serviceHost, Host discoveryHost, boolean isDiscoverable) throws Exception {
    	if (Arrays.compare(rid, probeID) == 0)
    		this.id = ServiceMessage.probeID;
    	else if (Arrays.compare(rid, announceID) == 0) 
    		this.id = ServiceMessage.announceID;
    	else
    		throw new Exception("Malformed ServiceMessage. No valid identifier: " + rid);
        this.serviceName = serviceName;
        this.serviceHost = serviceHost; 
        this.discoveryHost = discoveryHost;
        this.isDiscoverable = isDiscoverable;
    }

    /**
     * Returns the logical name of the advertised service.
     *
     * @return the service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the host at which the advertised service accepts connections.
     *
     * @return the service host
     */
    public Host getServiceHost() {
        return serviceHost;
    }

    /**
     * Returns the host on which the sender's discovery socket is listening.
     *
     * @return the discovery host
     */
    public Host getDiscoveryHost() {
        return discoveryHost;
    }

    /**
     * Returns whether the sender is willing to be contacted by discovering peers.
     *
     * @return {@code true} if the sender is discoverable
     */
    public boolean isSenderDiscoverable() {
    	return this.isDiscoverable;
    }

    /**
     * Returns {@code true} if this message is a probe (seeking peers), {@code false} if it is an announce (reply).
     *
     * @return {@code true} for probe messages
     */
    public boolean isProbe() {
        return this.id == probeID;
    }

    /**
     * Serialises a single ServiceMessage into a datagram-ready byte array.
     *
     * @param m       the message to serialise
     * @param asProbe {@code true} to mark the wire message as a probe; {@code false} to mark it as an announce
     * @return the serialised byte array
     * @throws IOException if serialisation of the host addresses fails
     */
   public static byte[] convertToMessage(ServiceMessage m, boolean asProbe) throws IOException {
    	ByteBuf buffer = wrappedBuffer(new byte[DATAGRAM_SIZE]);
    	buffer.clear();
    	if(asProbe)
    		buffer.writeBytes(ServiceMessage.probeID);
    	else
    		buffer.writeBytes(ServiceMessage.announceID);
    	buffer.writeInt(m.serviceName.getBytes().length);
    	buffer.writeBytes(m.serviceName.getBytes());
    	buffer.writeBoolean(m.isDiscoverable);
    	Host.serializer.serialize(m.serviceHost, buffer);
    	Host.serializer.serialize(m.discoveryHost, buffer);
    	
    	return  buffer.capacity(buffer.readableBytes()).array();	
    }
    
    /**
     * Deserialises a single ServiceMessage from a raw byte array produced by {@link #convertToMessage}.
     *
     * @param data the raw bytes of exactly one serialised ServiceMessage
     * @return the deserialised ServiceMessage
     * @throws Exception if the message identifier is unrecognised or deserialisation fails
     */
    public static ServiceMessage fromDatagram(byte[] data) throws Exception {
    	ByteBuf buffer = wrappedBuffer(data);
    	buffer.resetReaderIndex();
    	byte[] rid = new byte[4];
    	buffer.readBytes(rid);
    	byte[] serviceName = new byte[buffer.readInt()];
    	buffer.readBytes(serviceName);
    	boolean discoverable = buffer.readBoolean();
    	Host seviceHost = Host.serializer.deserialize(buffer);
    	Host discoveryHost = Host.serializer.deserialize(buffer);
    	
    	return new ServiceMessage(rid, new String(serviceName), seviceHost, discoveryHost, discoverable);
    }
   
    /**
     * Serialises a collection of ServiceMessages into one or more datagram-ready byte arrays,
     * splitting across multiple buffers when a single UDP datagram would be exceeded.
     *
     * @param ms      the collection of messages to serialise
     * @param asProbe {@code true} to mark each wire message as a probe; {@code false} for announce
     * @return a list of byte arrays, each fitting within {@link #DATAGRAM_SIZE} bytes
     * @throws IOException if serialisation of any message fails
     */
    public static List<byte[]> convertToMessage(Collection<ServiceMessage> ms, boolean asProbe) throws IOException {
    	ByteBuf buffer = wrappedBuffer(new byte[DATAGRAM_SIZE]);
    	buffer.clear();
    	List<byte[]> messages = new ArrayList<byte[]>(ms.size());
    	for(ServiceMessage m: ms) {
    		byte[] bm = ServiceMessage.convertToMessage(m, asProbe);
    		if(! (buffer.capacity() - buffer.writerIndex() >= Short.SIZE + bm.length) ) {
    			messages.add(buffer.capacity(buffer.readableBytes()).array());
    			buffer = wrappedBuffer(new byte[DATAGRAM_SIZE]);
    		}	
    		buffer.writeShort(Integer.valueOf(bm.length).shortValue());
    		buffer.writeBytes(bm);
    	}
        	
    	if(buffer.isReadable())
    		messages.add(buffer.capacity(buffer.readableBytes()).array());
    	
    	return messages;
    }
    
    /**
     * Deserialises all ServiceMessages packed into a single received datagram.
     * Messages that fail to deserialise are silently skipped and their stack traces are printed.
     *
     * @param data the raw datagram bytes (may contain multiple framed messages)
     * @return a list of successfully deserialised ServiceMessages; may be empty if none could be parsed
     */
    public static List<ServiceMessage> manyFromDatagram(byte[] data) {
    	List<ServiceMessage> messages = new ArrayList<ServiceMessage>();
    	ByteBuf buffer = wrappedBuffer(data);
    	buffer.resetReaderIndex();
    	while(buffer.isReadable()) {
    		byte[] msg = new byte[buffer.readShort()];
    		buffer.readBytes(msg);
    		try {
				messages.add(ServiceMessage.fromDatagram(msg));
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	
    	return messages;
    }
}
