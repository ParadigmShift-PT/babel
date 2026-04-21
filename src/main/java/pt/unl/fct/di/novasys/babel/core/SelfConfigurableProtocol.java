package pt.unl.fct.di.novasys.babel.core;

import java.util.concurrent.BlockingQueue;

import pt.unl.fct.di.novasys.babel.internal.InternalEvent;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * A {@link DiscoverableProtocol} that additionally supports self-configuration via DNS TXT records.
 * Subclasses expose their configurable parameters through {@link AutoConfigureParameter}-annotated
 * fields and implement {@link #getHost()} to indicate the DNS name used for TXT record lookups.
 */
public abstract class SelfConfigurableProtocol extends DiscoverableProtocol {

    /**
     * Creates a self-configurable protocol with a pre-known local host address.
     *
     * @param protoName human-readable name for this protocol
     * @param protoId   unique short identifier for this protocol
     * @param myself    the local host address to advertise during discovery
     */
    public SelfConfigurableProtocol(String protoName, short protoId, Host myself) {
        super(protoName, protoId, myself);
    }

    /**
     * Creates a self-configurable protocol with no pre-known local host address.
     *
     * @param protoName human-readable name for this protocol
     * @param protoId   unique short identifier for this protocol
     */
    public SelfConfigurableProtocol(String protoName, short protoId) {
        super(protoName, protoId);
    }

    /**
     * Creates a self-configurable protocol with no pre-known local host address and a custom event queue policy.
     *
     * @param protoName human-readable name for this protocol
     * @param protoId   unique short identifier for this protocol
     * @param policy    the blocking queue used as the internal event queue
     */
    public SelfConfigurableProtocol(String protoName, short protoId, BlockingQueue<InternalEvent> policy) {
        super(protoName, protoId, policy);
    }

    /**
     * Creates a self-configurable protocol with a pre-known local host address and a custom event queue policy.
     *
     * @param protoName human-readable name for this protocol
     * @param protoId   unique short identifier for this protocol
     * @param myself    the local host address to advertise during discovery
     * @param policy    the blocking queue used as the internal event queue
     */
    public SelfConfigurableProtocol(String protoName, short protoId, Host myself, BlockingQueue<InternalEvent> policy) {
        super(protoName, protoId, myself, policy);
    }

    /**
     * Should return the host that this protocol expects the DNS TXT records
     * 
     * @return the String representation of the host
     */
    public abstract String getHost();
}
