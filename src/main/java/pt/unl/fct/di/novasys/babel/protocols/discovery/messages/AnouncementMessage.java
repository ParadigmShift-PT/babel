package pt.unl.fct.di.novasys.babel.protocols.discovery.messages;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

public class AnouncementMessage extends ProtoMessage {
    private static final Logger logger = LogManager.getLogger(AnouncementMessage.class);

    public static final short MESSAGE_ID = 20603;
    public static final String BABEL_SIGNAL_STRING = "Babel";
    public static final byte[] BABEL_SIGNAL;
    public static final byte SEPARATOR = 30; // RECORD SEPARATOR IN ASCII

    private final String serviceName;
    private final Host serviceHost;

    static {
        BABEL_SIGNAL = BABEL_SIGNAL_STRING.getBytes(StandardCharsets.UTF_8);
    }

    public AnouncementMessage(String serviceName, Host serviceHost) {
        super(MESSAGE_ID);
        this.serviceName = serviceName;
        this.serviceHost = serviceHost;
    }

    public String getServiceName() {
        return serviceName;
    }
    
    public Host getServiceHost() {
        return serviceHost;
    }

    public static ISerializer<? extends ProtoMessage> serializer = new ISerializer<AnouncementMessage>() {
        @Override
        public void serialize(AnouncementMessage msg, ByteBuf out) throws IOException {
            byte[] serializedServiceName = msg.getServiceName().getBytes(StandardCharsets.UTF_8);
            byte[] serializedServiceHost = msg.getServiceHost().getAddress().getAddress();
            short serializedPort = (short) msg.getServiceHost().getPort();
            // The two represents the number of separators, a separator should only take a byte
            // This sum should represent the number of bytes to write to the ByteBuf
            int size = serializedServiceName.length + serializedServiceHost.length + 3 + Short.BYTES;
            out.ensureWritable(size);

            out.writeBytes(BABEL_SIGNAL);
            out.writeByte(SEPARATOR);
            out.writeBytes(serializedServiceName);
            out.writeByte(SEPARATOR);
            out.writeBytes(serializedServiceHost);
            out.writeByte(SEPARATOR);
            out.writeShortLE(serializedPort);
        }

        @Override
        public AnouncementMessage deserialize(ByteBuf in) throws UnknownHostException {
            byte[] tmpBuf = new byte[in.readableBytes()];
            int idx = 0;
            String babelSignal = null;
            String serviceName = null;
            InetAddress address = null;
            int port = 0;

            while(in.readableBytes() > 0) {
                byte readByte = in.readByte();
                if (readByte == SEPARATOR || in.readableBytes() == 0) {
                    if (babelSignal == null) {
                        babelSignal = new String(tmpBuf, StandardCharsets.UTF_8);
                    } else if (serviceName == null) {
                        serviceName = new String(tmpBuf, StandardCharsets.UTF_8);
                    } else if (address == null) {
                        byte[] addressBytes = Arrays.copyOf(tmpBuf, idx);
                        address = InetAddress.getByAddress(addressBytes);
                    } else if (port == 0) {
                        for (int i = 0; i < Short.BYTES; i++) {
                            port |= (tmpBuf[i] << (i * 8));
                        }
                    }
                    idx = 0;
                } else {
                    tmpBuf[idx] = readByte;
                    idx++;
                }
            }
            return new AnouncementMessage(serviceName, new Host(address, port));
        }
    };
    
}
