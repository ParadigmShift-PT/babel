package pt.unl.fct.di.novasys.network.tls;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoop;
import pt.unl.fct.di.novasys.network.AttributeValidator;
import pt.unl.fct.di.novasys.network.ChannelHandlerFactory;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.listeners.InConnListener;
import pt.unl.fct.di.novasys.network.listeners.MessageListener;
import pt.unl.fct.di.novasys.network.listeners.OutConnListener;
import pt.unl.fct.di.novasys.network.pipeline.InConnectionHandler;
import pt.unl.fct.di.novasys.network.pipeline.InHandshakeHandler;
import pt.unl.fct.di.novasys.network.pipeline.MessageDecoder;
import pt.unl.fct.di.novasys.network.pipeline.MessageEncoder;
import pt.unl.fct.di.novasys.network.pipeline.OutConnectionHandler;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;
import pt.unl.fct.di.novasys.network.tls.pipeline.InPreTLSHandshakeHandler;
import pt.unl.fct.di.novasys.network.tls.pipeline.InTLSConnectionHandler;
import pt.unl.fct.di.novasys.network.tls.pipeline.OutTLSConnectionHandler;

public class TLSChannelHandlerFactory extends ChannelHandlerFactory {

    public static final String TLS_HANDLER_NAME = "TLSHandler";

    private X509IKeyManager keyManager;
    private X509ITrustManager trustManager;

    public TLSChannelHandlerFactory(X509IKeyManager keyManager, X509ITrustManager trustManager) {
        this.keyManager = keyManager;
        this.trustManager = trustManager;
    }

    @Override
    public <T> OutConnectionHandler<T> createOutConnectionHandler(long connectionId, Host peer, Bootstrap bootstrap,
            OutConnListener<T> listener, MessageListener<T> consumer, ISerializer<T> serializer,
            EventLoop loop, Attributes selfAttrs, int hbInterval, int hbTolerance,
            AttributeValidator validator, int handshakeSteps) {
        return new OutTLSConnectionHandler<>(connectionId, peer, bootstrap, listener,
                consumer, serializer, loop, selfAttrs, hbInterval, hbTolerance, validator, handshakeSteps,
                keyManager, trustManager);
    }

    @Override
    public <T> InConnectionHandler<T> createInConnectionHandler(long connectionId, InConnListener<T> listener,
            MessageListener<T> consumer, EventLoop loop, Attributes selfAttrs,
            MessageEncoder<T> encoder, MessageDecoder<T> decoder) {
        return new InTLSConnectionHandler<>(connectionId, listener, consumer, loop, selfAttrs, encoder, decoder,
                keyManager, trustManager);
    }

    @Override
    public InHandshakeHandler createInHandshakeHandler(long connectionId, AttributeValidator validator,
            Attributes attributes, int handshakeSteps) {
        return new InPreTLSHandshakeHandler(connectionId, validator, attributes, handshakeSteps, keyManager);
    }

}
