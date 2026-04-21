package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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

/**
 * Self-configuration protocol that resolves parameter values by querying DNS TXT records.
 *
 * <p>TXT records must be published under the host name stored in each protocol's
 * {@code host} field and follow the format {@code protocolname.parametername=value}
 * (all lower-cased). When a matching record is found the value is applied via the
 * corresponding setter and, if the protocol is then fully configured, it is started.
 */
public class DNSSelfConfigurationProtocol extends SelfConfigurationProtocol {

    private static final Logger logger = LogManager.getLogger(DNSSelfConfigurationProtocol.class);

    public static final String PROTO_NAME = "BabelDNSSelfConfiguration";
    public static final short PROTO_ID = 32001;
    public static final String PAR_DNS_LOOKUP_SERVER = "dns.lookup.server";

    protected final Map<String, Map<String, Parameter>> protocolToParameterToConfigure;
    protected final Set<SelfConfigurableProtocol> protocolSet;
    protected DnsNameResolver resolver;
    private String nameserver;

    private Host myself;

    /**
     * Creates a DNSSelfConfigurationProtocol and initialises its internal parameter and protocol maps.
     */
    public DNSSelfConfigurationProtocol() {
        super(PROTO_NAME, PROTO_ID);

        protocolToParameterToConfigure = new HashMap<>();
        protocolSet = new HashSet<>();
    }

    /**
     * Registers a parameter that needs a value by recording it for the next DNS {@link #search()} call.
     * The parameter name and protocol name are lower-cased to match the DNS TXT record format.
     *
     * @param parameterName the logical name of the parameter to configure
     * @param setter        the reflective setter used to apply the discovered value
     * @param getter        the reflective getter (unused by this implementation but required by the interface)
     * @param proto         the protocol that owns the parameter
     */
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
        protocolSet.add(proto);
    }

    /**
     * No-op for the DNS-based strategy: once a parameter is configured via DNS its value
     * is already authoritative and does not need to be shared with peers.
     *
     * @param parameterName the parameter name (unused)
     * @param setter        the setter (unused)
     * @param getter        the getter (unused)
     * @param proto         the owning protocol (unused)
     */
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
     * @return a list of all the protocols that attempted to find a suitable
     *         configuration
     */
    public Set<SelfConfigurableProtocol> search() {
        try {
            for (var proto : protocolSet) {
                if (proto.getHost() == null)
                    continue;

                logger.debug("Looking for config at " + proto.getHost());

                DnsResponse results = resolver.query(new DefaultDnsQuestion(
                        proto.getHost(), DnsRecordType.TXT)).get().content();
                int answerCount = results.count(DnsSection.ANSWER);

                for (int i = 0; i < answerCount; i++) {
                    if (results.recordAt(DnsSection.ANSWER, i) instanceof DefaultDnsRawRecord record) {
                        var foundConfig = DefaultDnsRecordDecoder.decodeName(record.content()).split("=");
                        var protoNameAndParamName = foundConfig[0].split("\\.");
                        var protoName = StringUtils.lowerCase(protoNameAndParamName[0]);
                        var paramName = StringUtils.lowerCase(protoNameAndParamName[1]);
                        var valueFound = foundConfig[1].split("\\.")[0];
                        logger.debug("Got " + protoName + "." + paramName + "=" + valueFound);
                        var protoParam = protocolToParameterToConfigure.get(protoName);
                        var paramToConfigure = protoParam.remove(paramName);
                       paramToConfigure.setter().invoke(paramToConfigure.proto(), valueFound);
                    }
                }

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
        return Collections.unmodifiableSet(protocolSet);
    }

    /**
     * Returns this protocol's own host address.
     * Always {@code null} for the DNS strategy as no TCP self-configuration endpoint is opened.
     *
     * @return {@code null}
     */
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
    }
}
