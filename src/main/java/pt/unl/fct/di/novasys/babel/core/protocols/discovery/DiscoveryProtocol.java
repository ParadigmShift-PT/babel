package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;
import pt.unl.fct.di.novasys.babel.internal.InternalEvent;
import pt.unl.fct.di.novasys.network.data.Host;

public abstract class DiscoveryProtocol extends GenericProtocol {
  
	public DiscoveryProtocol(String protoName, short protoId) {
		super(protoName, protoId);
		System.setProperty("java.net.preferIPv4Stack" , "true");
	}

	public DiscoveryProtocol(String protoName, short protoId, BlockingQueue<InternalEvent> policy) {
		super(protoName, protoId, policy);
		System.setProperty("java.net.preferIPv4Stack" , "true");
	}

    public abstract void serviceSearchListenRequest(String serviceName, Host host) throws IOException;

    public abstract void serviceSearchAnounceRequest(String serviceName, SelfConfigurableProtocol sourceProtocol, Host host) throws IOException;

    public abstract Host getMyself();
}
