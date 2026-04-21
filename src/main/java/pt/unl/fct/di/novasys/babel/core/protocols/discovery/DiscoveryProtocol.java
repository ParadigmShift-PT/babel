package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import java.util.concurrent.BlockingQueue;

import pt.unl.fct.di.novasys.babel.core.DiscoverableProtocol;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.requests.RequestDiscovery;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.internal.InternalEvent;

/**
 * Abstract base class for Babel discovery protocols.
 *
 * <p>Subclasses implement the transport-specific announcement mechanism (broadcast, multicast, etc.)
 * and are responsible for delivering contact addresses to {@link pt.unl.fct.di.novasys.babel.core.DiscoverableProtocol}
 * instances that need them before they can start.
 */
public abstract class DiscoveryProtocol extends GenericProtocol {

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	public static final String PAR_DISCOVERY_UNICAST_INTERFACE = "babel.discovery.unicast.interface";
	public static final String PAR_DISCOVERY_UNICAST_ADDRESS = "babel.discovery.unicast.address";
	public static final String PAR_DISCOVERY_UNICAST_PORT = "babel.discovery.unicast.port";

	/**
	 * Creates a DiscoveryProtocol with the default (unbounded) event queue and registers
	 * the {@link RequestDiscovery} handler.
	 *
	 * @param protoName the human-readable protocol name
	 * @param protoId   the unique short protocol identifier
	 */
	public DiscoveryProtocol(String protoName, short protoId) {
		super(protoName, protoId);
		try {
			registerRequestHandler(RequestDiscovery.REQUEST_ID, this::uponRequestDiscovery);
		} catch (HandlerRegistrationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a DiscoveryProtocol with a custom event-queue policy and registers
	 * the {@link RequestDiscovery} handler.
	 *
	 * @param protoName the human-readable protocol name
	 * @param protoId   the unique short protocol identifier
	 * @param policy    the blocking queue used as the protocol's event queue
	 */
	public DiscoveryProtocol(String protoName, short protoId, BlockingQueue<InternalEvent> policy) {
		super(protoName, protoId, policy);
		try {
			registerRequestHandler(RequestDiscovery.REQUEST_ID, this::uponRequestDiscovery);
		} catch (HandlerRegistrationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Registers a protocol that hasn't started for discovery. Babel expects that
	 * the protocols that extend this class start the protocolo if it has everything
	 * to start.
	 * 
	 * @param dcProto the protocol to receive a contact
	 */
	public abstract void registerProtocol(DiscoverableProtocol dcProto);

	/**
	 * Registers a running protocol for discovery
	 * 
	 * @param request
	 * @param sourceProtocol the protocol to receive a contact
	 */
	public abstract void uponRequestDiscovery(RequestDiscovery request, short sourceProtocol);

}
