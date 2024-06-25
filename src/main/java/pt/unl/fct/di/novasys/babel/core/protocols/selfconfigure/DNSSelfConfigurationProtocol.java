package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DefaultDnsRecordDecoder;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

public class DNSSelfConfigurationProtocol extends SelfConfigurationProtocol {

    private static final Logger logger = LogManager.getLogger(DNSSelfConfigurationProtocol.class);

    public static final String PROTO_NAME = "BabelDNSSelfConfiguration";
    public static final short PROTO_ID = 32001;
    public static final String PAR_DNS_LOOKUP_SERVER = "dns.lookup.server";
    public static final String PAR_DNS_LOOKUP_HOSTNAME = "dns.lookup.hostname";

    protected final Map<String, Map<String, Parameter>> protocolToParameterToConfigure;
    protected final List<SelfConfigurableProtocol> protocolList;
    protected DnsNameResolver resolver;
    private String nameserver;

    private Host myself;

    public DNSSelfConfigurationProtocol() {
        super(PROTO_NAME, PROTO_ID);

        protocolToParameterToConfigure = new HashMap<>();
        protocolList = new ArrayList<>();
    }

    @Override
    public void addProtocolParameterToConfigure(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto) {
        Parameter parameter = new Parameter(getter, setter, proto, parameterName);
        var protoName = StringUtils.lowerCase(proto.getProtoName());
        var lowerParameterName = StringUtils.lowerCase(parameterName);
        var protocolParameters = protocolToParameterToConfigure.get(protoName);
        if (protocolParameters == null) {
            protocolParameters = new HashMap<>();
            protocolToParameterToConfigure.put(protoName, protocolParameters);
        }
        protocolParameters.put(lowerParameterName, parameter);
        protocolList.add(proto);
    }

    @Override
    public void addProtocolParameterConfigured(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto) {

    }

    /**
     * Tries to search in the provided host for TXT records in babel.host
     * The TXT records should come in the format
     * name_of_the_protocol.parameter_name_in_code=value
     * For example, if your protocol has the name PingPong and a parameter with the
     * AutoConfigure anotation named nMessages, the txt record should be in host
     * babel with the value pingpong.nmessages=5.
     * 
     * @return a list of all the protocols that attempted to find a suitable configuration
     */
    public List<SelfConfigurableProtocol> search() {
        try {
            DnsResponse results = resolver.query(new DefaultDnsQuestion(
                    "babel." + nameserver, DnsRecordType.TXT)).get().content();
            int answerCount = results.count(DnsSection.ANSWER);

            for (int i = 0; i < answerCount; i++) {
                if (results.recordAt(DnsSection.ANSWER, i) instanceof DefaultDnsRawRecord record) {
                    var foundConfig = DefaultDnsRecordDecoder.decodeName(record.content()).split("=");
                    var protoNameAndParamName = foundConfig[0].split("\\.");
                    var protoName = protoNameAndParamName[0];
                    var paramName = protoNameAndParamName[1];
                    var valueFound = foundConfig[1].split("\\.")[0];
                    var protoParam = protocolToParameterToConfigure.get(protoName);
                    var paramToConfigure = protoParam.remove(paramName);
                    paramToConfigure.setter().invoke(paramToConfigure.proto(), valueFound);
                }
            }

            for (var proto : protocolList) {
                if (babel.checkAndStartDcProto(proto)) {
                    protocolToParameterToConfigure.remove(StringUtils.lowerCase(proto.getProtoName()));
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            logger.error(
                    "Couldn't retrieve DNS record from babel." + nameserver);
            e.printStackTrace();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return Collections.unmodifiableList(protocolList);
    }

    @Override
    public Host getMyself() {
        return myself;
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        var executor = new NioEventLoopGroup();
        var builder = new DnsNameResolverBuilder(executor.next());
        if (props.containsKey(PAR_DNS_LOOKUP_SERVER)) {
        }
        builder.channelType(NioDatagramChannel.class);
        resolver = builder.build();

        if (!props.containsKey(PAR_DNS_LOOKUP_HOSTNAME)) {
            throw new RuntimeException("DNSSelfConfigurationProtocol needs a nameserver");
        }
        nameserver = props.getProperty(PAR_DNS_LOOKUP_HOSTNAME);
    }

}
