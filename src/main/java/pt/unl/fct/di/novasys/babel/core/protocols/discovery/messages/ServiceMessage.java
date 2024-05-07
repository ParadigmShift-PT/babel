package pt.unl.fct.di.novasys.babel.core.protocols.discovery.messages;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * In the future, serialization of this message might be changed to support IPv6 addresses which are laking in Babel
 */
public class ServiceMessage extends ProtoMessage {
    private static final Logger logger = LogManager.getLogger(ServiceMessage.class);

    public static final short MESSAGE_ID = 20603;
    public static final String BABEL_SIGNAL_STRING = "BabelD";
    public static final byte[] BABEL_SIGNAL;
    public static final byte SEPARATOR = 30; // RECORD SEPARATOR IN ASCII

    private final String serviceName;
    private final Host serviceHost;
    private final Host discoveryHost;
    private final boolean searching;

    static {
        BABEL_SIGNAL = BABEL_SIGNAL_STRING.getBytes(StandardCharsets.UTF_8);
    }

    public ServiceMessage(String serviceName, Host serviceHost, Host discoveryHost, boolean searching) {
        super(MESSAGE_ID);
        this.serviceName = serviceName;
        this.serviceHost = serviceHost;
        this.searching = searching;
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

    public boolean isSearching() {
        return searching;
    }

    public static ISerializer<? extends ProtoMessage> serializer = new ISerializer<ServiceMessage>() {
        @Override
        public void serialize(ServiceMessage msg, ByteBuf out) throws IOException {
            byte[] serializedServiceName = msg.getServiceName().getBytes(StandardCharsets.UTF_8);
            byte[] serializedServiceHost = msg.getServiceHost().getAddress().getAddress();
            short serializedServicePort = (short) msg.getServiceHost().getPort();
            byte[] serializedDiscoveryHost = msg.getDiscoveryHost().getAddress().getAddress();
            short serializedDiscoveryPort = (short) msg.getDiscoveryHost().getPort();
            // The two represents the number of separators, a separator should only take a
            // byte
            // This sum should represent the number of bytes to write to the ByteBuf
            int size = BABEL_SIGNAL.length + serializedServiceName.length + serializedServiceHost.length + 5
                    + 2 * Short.BYTES + serializedDiscoveryHost.length;
            out.ensureWritable(size);

            out.writeBytes(BABEL_SIGNAL);
            out.writeByte(SEPARATOR);
            out.writeBytes(serializedServiceName);
            if (msg.isSearching()) {
                out.writeByte('?');
            }
            out.writeByte(SEPARATOR);
            out.writeBytes(serializedServiceHost);
            out.writeByte(SEPARATOR);
            out.writeShortLE(serializedServicePort);
            out.writeByte(SEPARATOR);
            out.writeBytes(serializedDiscoveryHost);
            out.writeByte(SEPARATOR);
            out.writeShortLE(serializedDiscoveryPort);
        }

        @Override
        public ServiceMessage deserialize(ByteBuf in) throws UnknownHostException {
            byte[] tmpBuf = new byte[in.readableBytes()];
            int idx = 0;
            String babelSignal = null;
            String serviceName = null;
            InetAddress serviceAddress = null;
            int servicePort = 0;
            InetAddress discoveryAddress = null;
            int discoveryPort = 0;
            boolean searching = false;

            while (in.readableBytes() > 0) {
                byte readByte = in.readByte();
                tmpBuf[idx++] = readByte;
                if (readByte == '?') {
                    searching = true;
                } else if (readByte == SEPARATOR || in.readableBytes() == 0) {
                    if (babelSignal == null) {
                        babelSignal = new String(tmpBuf, 0, idx - 1, StandardCharsets.UTF_8);
                    } else if (serviceName == null) {
                        serviceName = new String(tmpBuf, 0, idx - 1 - BooleanUtils.toInteger(searching), StandardCharsets.UTF_8);
                    } else if (serviceAddress == null && idx > 4) {
                        byte[] addressBytes = Arrays.copyOf(tmpBuf, idx - 1);
                        serviceAddress = InetAddress.getByAddress(addressBytes);
                    } else if (servicePort == 0 && idx > 2) {
                        for (int i = 0; i < Short.BYTES; i++) {
                            servicePort |= (Byte.toUnsignedInt(tmpBuf[i]) << (i * 8));
                        }
                    } else if (discoveryAddress == null && idx > 4) {
                        byte[] addressBytes = Arrays.copyOf(tmpBuf, idx - 1);
                        discoveryAddress = InetAddress.getByAddress(addressBytes);
                    } else if (discoveryPort == 0 && idx > 2) {
                        for (int i = 0; i < Short.BYTES; i++) {
                            discoveryPort |= (Byte.toUnsignedInt(tmpBuf[i]) << (i * 8));
                        }
                    }
                    idx = 0;
                }
            }
            return new ServiceMessage(serviceName, new Host(serviceAddress, servicePort),
                    new Host(discoveryAddress, discoveryPort), searching);
        }
    };
}
