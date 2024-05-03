package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.messages;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class ParameterResponseMessage extends ProtoMessage {

    public static final short MSG_ID = 10604;
    public static final byte PROTO_SEPARATOR = 30;
    public static final char PROTO_AND_PARAM_SEPARATOR = ';';
    public static final char PARAM_AND_PARAM_SEPARATOR = ',';
    public static final char PARAM_AND_VALUE_SEPARATOR = ':';

    private final Map<String, Map<String, String>> protoParam;

    public ParameterResponseMessage() {
        super(MSG_ID);

        protoParam = new HashMap<>();
    }

    public void addParameter(String protoName, String paramName, String value) {
        Map<String, String> paramValueMap = protoParam.get(protoName);
        if (paramValueMap == null) {
            paramValueMap = new HashMap<>();
            protoParam.put(protoName, paramValueMap);
        }
        paramValueMap.put(paramName, value);
    }

    public Map<String, String> getProtocolParams(String protoName) {
        return Collections.unmodifiableMap(protoParam.get(protoName));
    }

    public static final ISerializer<ParameterResponseMessage> serializer = new ISerializer<ParameterResponseMessage>() {

        @Override
        public ParameterResponseMessage deserialize(ByteBuf arg0) throws IOException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'deserialize'");
        }

        @Override
        public void serialize(ParameterResponseMessage arg0, ByteBuf arg1) throws IOException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'serialize'");
        }
        
    };
}
