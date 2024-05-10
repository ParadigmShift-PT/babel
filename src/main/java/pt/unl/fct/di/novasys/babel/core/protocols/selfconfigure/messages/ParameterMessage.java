package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.messages;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class ParameterMessage extends ProtoMessage {

    public static final short MSG_ID = 10604;
    public static final byte PROTO_SEPARATOR = 30;
    public static final char PROTO_AND_PARAM_SEPARATOR = ';';
    public static final char PARAM_AND_PARAM_SEPARATOR = ',';
    public static final char PARAM_AND_VALUE_SEPARATOR = ':';

    private final Map<String, Map<String, String>> protoParam;

    public ParameterMessage() {
        this(new HashMap<>());
    }

    public ParameterMessage(Map<String, Map<String, String>> protoParam) {
        super(MSG_ID);

        this.protoParam = protoParam;
    }

    public void addParameter(String protoName, String paramName, String value) {
        Map<String, String> paramValueMap = protoParam.get(protoName);
        if (paramValueMap == null) {
            paramValueMap = new HashMap<>();
            protoParam.put(protoName, paramValueMap);
        }
        paramValueMap.put(paramName, value);
    }

    public void addAskingParameter(String protoName, String paramName) {
        addParameter(protoName, paramName, null);
    }

    public void join(ParameterMessage msg) {
        protoParam.putAll(msg.getAllProtocolParams());
    }

    public Map<String, String> getProtocolParams(String protoName) {
        return Collections.unmodifiableMap(protoParam.get(protoName));
    }

    public Map<String, Map<String, String>> getAllProtocolParams() {
        return Collections.unmodifiableMap(protoParam);
    }

    public static final ISerializer<ParameterMessage> serializer = new ISerializer<ParameterMessage>() {

        @Override
        public ParameterMessage deserialize(ByteBuf in) throws IOException {
            Map<String, Map<String, String>> protoParam = new HashMap<>();
            byte[] tmpBuf = new byte[in.readableBytes()];
            String currentProto = null;
            Map<String, String> currentMapping = null;
            String currentParam = null;
            String paramValue = null;
            int idx = 0;

            while (in.readableBytes() > 0) {
                byte readByte = in.readByte();
                tmpBuf[idx++] = readByte;
                if (readByte == PROTO_SEPARATOR) {
                    idx = 0;
                    currentProto = null;
                    currentParam = null;
                    paramValue = null;
                    currentMapping = null;
                } else if (readByte == PROTO_AND_PARAM_SEPARATOR) {
                    currentProto = new String(tmpBuf, 0, idx - 1, StandardCharsets.UTF_8);
                    currentMapping = protoParam.get(currentProto);
                    if (currentMapping == null) {
                        currentMapping = new HashMap<>();
                        protoParam.put(currentProto, currentMapping);
                    }
                    idx = 0;
                } else if (readByte == PARAM_AND_PARAM_SEPARATOR) {
                    if (idx == 1) {
                        idx = 0;
                    } else if (currentParam == null) {
                        currentParam = new String(tmpBuf, 0, idx - 1, StandardCharsets.UTF_8);
                        currentMapping.put(currentParam, null);
                    } else {
                        paramValue = new String(tmpBuf, 0, idx - 1, StandardCharsets.UTF_8);
                        currentMapping.put(currentParam, paramValue);
                    }
                    currentParam = null;
                    paramValue = null;
                    idx = 0;
                } else if (readByte == PARAM_AND_VALUE_SEPARATOR) {
                    currentParam = new String(tmpBuf, 0, idx - 1, StandardCharsets.UTF_8);
                    idx = 0;
                }
            }

            return new ParameterMessage(protoParam);
        }

        @Override
        public void serialize(ParameterMessage msg, ByteBuf out) throws IOException {
            for (var protoEntry : msg.getAllProtocolParams().entrySet()) {
                byte[] serializedProtoName = protoEntry.getKey().getBytes(StandardCharsets.UTF_8);
                out.writeByte(PROTO_SEPARATOR);
                out.writeBytes(serializedProtoName);
                out.writeByte(PROTO_AND_PARAM_SEPARATOR);
                for (var paramEntry : protoEntry.getValue().entrySet()) {
                    out.writeByte(PARAM_AND_PARAM_SEPARATOR);
                    byte[] serializedParamName = paramEntry.getKey().getBytes(StandardCharsets.UTF_8);
                    out.writeBytes(serializedParamName);
                    String value = paramEntry.getValue();
                    if (value != null) {
                        out.writeByte(PARAM_AND_VALUE_SEPARATOR);
                        byte[] serializedValue = paramEntry.getValue().getBytes(StandardCharsets.UTF_8);
                        out.writeBytes(serializedValue);
                    }
                }
                out.writeByte(PARAM_AND_PARAM_SEPARATOR);
            }
        }
    };
}
