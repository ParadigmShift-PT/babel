package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import java.util.concurrent.BlockingQueue;

import pt.unl.fct.di.novasys.babel.core.DiscoverableProtocol;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.internal.InternalEvent;

public abstract class DiscoveryProtocol extends GenericProtocol {
  
	public DiscoveryProtocol(String protoName, short protoId) {
		super(protoName, protoId);
		System.setProperty("java.net.preferIPv4Stack" , "true");
	}

	public DiscoveryProtocol(String protoName, short protoId, BlockingQueue<InternalEvent> policy) {
		super(protoName, protoId, policy);
		System.setProperty("java.net.preferIPv4Stack" , "true");
	}

	public abstract void registerProtocol(DiscoverableProtocol dcProto);

}
