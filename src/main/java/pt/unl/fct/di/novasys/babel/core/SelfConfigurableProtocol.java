package pt.unl.fct.di.novasys.babel.core;

import java.util.concurrent.BlockingQueue;

import pt.unl.fct.di.novasys.babel.internal.InternalEvent;
import pt.unl.fct.di.novasys.network.data.Host;

public abstract class SelfConfigurableProtocol extends DiscoverableProtocol {

    public SelfConfigurableProtocol(String protoName, short protoId, Host myself) {
        super(protoName, protoId, myself);
    }
    
    public SelfConfigurableProtocol(String protoName, short protoId) {
        super(protoName, protoId);
    }

    public SelfConfigurableProtocol(String protoName, short protoId, BlockingQueue<InternalEvent> policy) {
        super(protoName, protoId, policy);
    }

    public SelfConfigurableProtocol(String protoName, short protoId, Host myself, BlockingQueue<InternalEvent> policy) {
        super(protoName, protoId, myself, policy);
    }
}
