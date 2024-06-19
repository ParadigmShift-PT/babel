package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DefaultDnsRecordDecoder;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.util.concurrent.Future;
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
    protected final Map<String, SelfConfigurableProtocol> protocolMap;
    protected DnsNameResolver resolver;
    private String nameserver;

    private Host myself;

    public DNSSelfConfigurationProtocol() {
        super(PROTO_NAME, PROTO_ID);

        protocolToParameterToConfigure = new ConcurrentHashMap<>();
        protocolMap = new ConcurrentHashMap<>();
    }

    @Override
    public void addProtocolParameterToConfigure(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto) {
        Parameter parameter = new Parameter(getter, setter, proto, parameterName);
        var protocolParameters = protocolToParameterToConfigure.get(proto.getProtoName());
        if (protocolParameters == null) {
            protocolParameters = new ConcurrentHashMap<>();
            protocolToParameterToConfigure.put(proto.getProtoName(), protocolParameters);
        }
        protocolParameters.put(parameterName, parameter);
        protocolMap.put(proto.getProtoName(), proto);
    }

    @Override
    public void addProtocolParameterConfigured(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto) {

    }

    public void search() {
        Map<String, Map<String, Future<AddressedEnvelope<DnsResponse, InetSocketAddress>>>> results = new HashMap<>();
        for (var protoParam : protocolToParameterToConfigure.entrySet()) {
            Map<String, Future<AddressedEnvelope<DnsResponse, InetSocketAddress>>> protoResults = new HashMap<>();
            results.put(protoParam.getKey(), protoResults);
            for (var param : protoParam.getValue().values()) {
                DnsQuestion question = new DefaultDnsQuestion(
                        protoParam.getKey() + "." + param.name() + "." + nameserver,
                        DnsRecordType.TXT);
                protoResults.put(param.name(), resolver.query(question));
            }
        }

        for (var protoResults : results.entrySet()) {
            var protoParams = protocolToParameterToConfigure.get(protoResults.getKey());
            for (var paramResults : protoResults.getValue().entrySet()) {
                try {
                    var result = DefaultDnsRecordDecoder.decodeName(((DefaultDnsRawRecord)paramResults.getValue().get().content().recordAt(DnsSection.ANSWER)).content()).split("\\.")[0];
                    var paramToConfigure = protoParams.get(paramResults.getKey());
                    paramToConfigure.setter().invoke(paramToConfigure.proto(), result.toString());
                } catch (InterruptedException | ExecutionException e) {
                    logger.error(
                            "Couldn't retrieve DNS record for " + protoResults.getKey() + "." + paramResults.getKey());
                    e.printStackTrace();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
            var proto = protocolMap.get(protoResults.getKey());
            babel.checkAndStartDcProto(proto);
        }
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
