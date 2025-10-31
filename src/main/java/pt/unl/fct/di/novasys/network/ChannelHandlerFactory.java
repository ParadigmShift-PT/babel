package pt.unl.fct.di.novasys.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoop;
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

public class ChannelHandlerFactory {

    public <T> OutConnectionHandler<T> createOutConnectionHandler(long connectionId, Host peer, Bootstrap bootstrap,
            OutConnListener<T> listener, MessageListener<T> consumer, ISerializer<T> serializer, EventLoop loop,
            Attributes selfAttrs, int hbInterval, int hbTolerance, AttributeValidator validator, int handshakeSteps) {
        return new OutConnectionHandler<>(connectionId, peer, bootstrap, listener, consumer, serializer, loop,
                selfAttrs, hbInterval, hbTolerance, validator, handshakeSteps);
    }

    public <T> InConnectionHandler<T> createInConnectionHandler(long connectionId,
            InConnListener<T> listener, MessageListener<T> consumer, EventLoop loop,
            Attributes selfAttrs, MessageEncoder<T> encoder, MessageDecoder<T> decoder) {
        return new InConnectionHandler<>(connectionId, listener, consumer, loop, selfAttrs, encoder, decoder);
    }

    public <T> InHandshakeHandler createInHandshakeHandler(long connectionId,
            AttributeValidator validator, Attributes attributes, int handshakeSteps) {
        return new InHandshakeHandler(connectionId, validator, attributes, handshakeSteps);
    }

}
