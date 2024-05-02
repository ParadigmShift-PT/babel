package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure.messages;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class ParameterMessage extends ProtoMessage {

    public static final short PROTO_ID = 10604;
    public static final byte PROTO_SEPARATOR = 30;
    public static final char PROTO_AND_PARAM_SEPARATOR = ';';
    public static final char PARAM_AND_PARAM_SEPARATOR = ',';
    public static final char PARAM_AND_VALUE_SEPARATOR = ':';

    private final Map<String, Map<String, String>> protoParam;

    public ParameterMessage() {
        super(PROTO_ID);

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
}
