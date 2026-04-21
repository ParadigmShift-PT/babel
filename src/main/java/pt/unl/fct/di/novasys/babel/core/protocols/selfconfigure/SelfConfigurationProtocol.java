package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;
import pt.unl.fct.di.novasys.babel.internal.InternalEvent;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * Abstract base class for Babel self-configuration protocols.
 *
 * <p>A self-configuration protocol is responsible for discovering and distributing the runtime
 * parameter values required by {@link SelfConfigurableProtocol} instances before they can start.
 * Concrete implementations (e.g. copy-based or DNS-based) override the abstract methods to define
 * how parameter values are obtained and shared across nodes.
 */
public abstract class SelfConfigurationProtocol extends GenericProtocol {

    /**
     * Creates a SelfConfigurationProtocol with the default (unbounded) event queue.
     *
     * @param protoName the human-readable protocol name
     * @param protoId   the unique short protocol identifier
     */
    public SelfConfigurationProtocol(String protoName, short protoId) {
        super(protoName, protoId);
    }

    /**
     * Creates a SelfConfigurationProtocol with a custom event-queue policy.
     *
     * @param protoName the human-readable protocol name
     * @param protoId   the unique short protocol identifier
     * @param policy    the blocking queue used as the protocol's event queue
     */
    public SelfConfigurationProtocol(String protoName, short protoId, BlockingQueue<InternalEvent> policy) {
        super(protoName, protoId, policy);
    }

    /**
     * Adds a parameter to be configured for the proto
     * 
     * @param parameterName parameter's name
     * @param setter        setter for the parameter
     * @param getter        getter for the parameter
     * @param proto         the protocol that has the parameter
     */
    public abstract void addProtocolParameterToConfigure(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto);

    /**
     * Adds a parameter already configured in proto
     * 
     * @param parameterName parameter's name
     * @param setter        setter for the parameter
     * @param getter        getter for the parameter
     * @param proto         the protocol that has the parameter
     */
    public abstract void addProtocolParameterConfigured(String parameterName, Method setter, Method getter,
            SelfConfigurableProtocol proto);

    /**
     * @return the host that represents the protocol
     */
    public abstract Host getMyself();
}
