package pt.unl.fct.di.novasys.babel.core;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import pt.unl.fct.di.novasys.babel.channels.BabelChannel;

/**
 * Class to create a new Babel Runtime instance
 */
public class BabelBootstrap {
    private List<BabelProtocolBuilder> protocols;
    private List<BabelChannel> channels;

    public BabelBootstrap() {
        this.protocols = new ArrayList<>();
    }

    /**
     * Register a protocol for this Babel Runtime
     * 
     * @param protocolBuilder
     * @return this
     */
    public BabelBootstrap registerProtocol(BabelProtocolBuilder protocolBuilder) {
        throw new NotImplementedException();
    }

    public BabelBootstrap addDefaultAddress(InetSocketAddress address) {
        throw new NotImplementedException();
    }
}
