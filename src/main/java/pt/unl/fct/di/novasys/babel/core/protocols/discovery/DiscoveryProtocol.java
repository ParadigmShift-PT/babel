package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import java.util.concurrent.BlockingQueue;

import pt.unl.fct.di.novasys.babel.core.DiscoverableProtocol;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.protocols.discovery.requests.RequestDiscovery;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.internal.InternalEvent;

public abstract class DiscoveryProtocol extends GenericProtocol {

	static {
		System.setProperty("java.net.preferIPv4Stack" , "true");
	}
	public static final String PAR_DISCOVERY_UNICAST_INTERFACE = "babel.discovery.unicast.interface";
	public static final String PAR_DISCOVERY_UNICAST_ADDRESS = "babel.discovery.unicast.address";
	public static final String PAR_DISCOVERY_UNICAST_PORT = "babel.discovery.unicast.port";

	public DiscoveryProtocol(String protoName, short protoId) {
		super(protoName, protoId);
		try {
			registerRequestHandler(RequestDiscovery.REQUEST_ID, this::uponRequestDiscovery);
		} catch (HandlerRegistrationException e) {
			throw new RuntimeException(e);
		}
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	public DiscoveryProtocol(String protoName, short protoId, BlockingQueue<InternalEvent> policy) {
		super(protoName, protoId, policy);
		try {
			registerRequestHandler(RequestDiscovery.REQUEST_ID, this::uponRequestDiscovery);
		} catch (HandlerRegistrationException e) {
			throw new RuntimeException(e);
		}
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	public abstract void registerProtocol(DiscoverableProtocol dcProto);

	public abstract void uponRequestDiscovery(RequestDiscovery request, short sourceProtocol);

}
