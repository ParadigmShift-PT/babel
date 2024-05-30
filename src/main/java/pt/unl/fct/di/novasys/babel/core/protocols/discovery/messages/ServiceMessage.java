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




    public ServiceMessage(String serviceName, Host serviceHost, Host discoveryHost) {
    	this.id = ServiceMessage.probeID;
        this.serviceName = serviceName;
        this.serviceHost = serviceHost; 
        this.discoveryHost = discoveryHost;
    }
    
    public ServiceMessage(byte[] rid, String serviceName, Host serviceHost, Host discoveryHost) throws Exception {
    	if (Arrays.compare(rid, probeID) == 0)
    		this.id = ServiceMessage.probeID;
    	else if (Arrays.compare(rid, announceID) == 0) 
    		this.id = ServiceMessage.announceID;
    	else
    		throw new Exception("Malformed ServiceMessage. No valid identifier: " + rid);
        this.serviceName = serviceName;
        this.serviceHost = serviceHost; 
        this.discoveryHost = discoveryHost;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Host getServiceHost() {
        return serviceHost;
    }

    public Host getDiscoveryHost() {
        return discoveryHost;
    }

    public boolean isProbe() {
        return this.id == probeID;
    }

   public static byte[] convertToMessage(ServiceMessage m, boolean asProbe) throws IOException {
    	ByteBuf buffer = wrappedBuffer(new byte[DATAGRAM_SIZE]);
    	buffer.clear();
    	if(asProbe)
    		buffer.writeBytes(ServiceMessage.probeID);
    	else
    		buffer.writeBytes(ServiceMessage.announceID);
    	buffer.writeInt(m.serviceName.getBytes().length);
    	buffer.writeBytes(m.serviceName.getBytes());
    	Host.serializer.serialize(m.serviceHost, buffer);
    	Host.serializer.serialize(m.discoveryHost, buffer);
    	
    	return  buffer.capacity(buffer.readableBytes()).array();	
    }
    
    public static ServiceMessage fromDatagram(byte[] data) throws Exception {
    	ByteBuf buffer = wrappedBuffer(data);
    	buffer.resetReaderIndex();
    	byte[] rid = new byte[4];
    	buffer.readBytes(rid);
    	byte[] serviceName = new byte[buffer.readInt()];
    	buffer.readBytes(serviceName);
    	Host seviceHost = Host.serializer.deserialize(buffer);
    	Host discoveryHost = Host.serializer.deserialize(buffer);
    	
    	return new ServiceMessage(rid, new String(serviceName), seviceHost, discoveryHost);
    }
   
    public static List<byte[]> convertToMessage(Collection<ServiceMessage> ms, boolean asProbe) throws IOException {
    	ByteBuf buffer = wrappedBuffer(new byte[DATAGRAM_SIZE]);
    	buffer.clear();
    	List<byte[]> messages = new ArrayList<byte[]>();
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	return messages;
    }
}
