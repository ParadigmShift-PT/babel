package pt.unl.fct.di.novasys.babel.protocols.discovery;

import java.util.concurrent.BlockingQueue;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.protocol.DiscoverableProtocol;
import pt.unl.fct.di.novasys.babel.internal.InternalEvent;

public abstract class DiscoveryProtocol extends GenericProtocol {

	public DiscoveryProtocol(String protoName, short protoId, BlockingQueue<InternalEvent> policy) {
		super(protoName, protoId, policy);
	}

	public DiscoveryProtocol(String protoName, short protoId) {
		super(protoName, protoId);
	}
	
	public abstract void serviceListenRequest(String protoName, DiscoverableProtocol discProto);

}
