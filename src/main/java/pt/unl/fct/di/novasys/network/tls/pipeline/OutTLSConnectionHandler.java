package pt.unl.fct.di.novasys.network.tls.pipeline;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import pt.unl.fct.di.novasys.network.AttributeValidator;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.NetworkManager;
import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.network.listeners.MessageListener;
import pt.unl.fct.di.novasys.network.listeners.OutConnListener;
import pt.unl.fct.di.novasys.network.pipeline.OutConnectionHandler;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;
import pt.unl.fct.di.novasys.network.tls.userevents.PreTLSHandshakeCompleted;

import static pt.unl.fct.di.novasys.network.tls.TLSChannelHandlerFactory.TLS_HANDLER_NAME;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

public class OutTLSConnectionHandler<T> extends OutConnectionHandler<T> {

    private static final Logger logger = LogManager.getLogger(OutTLSConnectionHandler.class);

    // TODO read TLS options from props?

    private final X509IKeyManager keyManager;
    private final X509ITrustManager trustManager;

    public OutTLSConnectionHandler(long connectionId, Host peer, Bootstrap bootstrap, OutConnListener<T> listener,
            MessageListener<T> consumer, ISerializer<T> serializer,
            EventLoop loop, Attributes selfAttrs, int hbInterval, int hbTolerance,
            AttributeValidator validator, int handshakeSteps,
            X509IKeyManager keyManager, X509ITrustManager trustManager) {
        super(connectionId, peer, bootstrap, listener, consumer, serializer, loop, selfAttrs,
                hbInterval, hbTolerance, validator, handshakeSteps,
                new OutPreTLSHandshakeHandler(connectionId, validator, selfAttrs, handshakeSteps, keyManager));

        this.keyManager = keyManager;
        this.trustManager = trustManager;
    }

    @Override
    public void internalUserEventTriggered(ChannelHandlerContext ctx, Object evt) {
        logger.debug("User event triggered: {}", evt);
        if (evt instanceof PreTLSHandshakeCompleted hsEvt) {
            if (state != State.HANDSHAKING || ctx.channel() != channel)
                throw new AssertionError("Handshake completed while not in handshake state: " + peer);
            this.peerAttributes = hsEvt.getAttr();
            logger.debug("Pre TLS out handshake completed with: " + peer);
            try {
                addAndStartTLSHandler(ctx, hsEvt.getSelectedId(), hsEvt.getPeerId());
            } catch (Exception e) {
                logger.error("SSLHandler creation in connection to {} failed with exception: {}", peer, e);
                exceptionCaught(ctx, e);
            }
        } else if (logger.isWarnEnabled() && !(evt instanceof SslHandshakeCompletionEvent)) {
            logger.warn("Unknown user event caught: " + evt);
        }
        // TODO handle SslHandler events
    }

    private void addAndStartTLSHandler(ChannelHandlerContext chCtx, byte[] selectedId, byte[] peerId)
            throws Exception {
        var sslCtx = SslContextBuilder.forClient()
                // TODO should we use this or the subkeymanager directly?
                .keyManager(keyManager.getPrivateKey(selectedId), keyManager.getCertificateChain(selectedId))
                .trustManager(trustManager.singleTrustManager(peerId))
                .clientAuth(ClientAuth.REQUIRE) // Unneeded
                // Known issues with standard Java provider:
                // https://netty.io/4.1/api/io/netty/handler/ssl/SslHandler.html#Known_issues
                .sslContextProvider(new BouncyCastleJsseProvider())
                /* TODO study these options
                .protocols("TLSv1.3")
                .ciphers("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY_SHA256")
                .applicationProtocolConfig(
                        new ApplicationProtocolConfig(
                                ApplicationProtocolConfig.Protocol.ALPN,
                                ApplicationProtocolConfig.SelectorFailureBehavior.FATAL_ALERT,
                                ApplicationProtocolConfig.SelectedListenerFailureBehavior.FATAL_ALERT,
                                muxers.allProtocols + NoEarlyMuxerNegotiationEntry // early muxer negotiation
                        ))
                */
                .startTls(false) // This will be true on the server
                .build();

        var sslHandler = sslCtx.newHandler(chCtx.alloc(), peer.getAddress().getHostAddress(), peer.getPort(), loop);

        sslHandler.handshakeFuture().addListener(f -> {
            loop.execute(() -> {
                if (f.isSuccess()) {
                    state = State.CONNECTED;
                    listener.outboundConnectionUp(OutTLSConnectionHandler.this);
                } else {
                    exceptionCaught(chCtx, f.cause());
                }
            });
        });

        if (NetworkManager.IDLE_HANDLER_NAME.equals(chCtx.pipeline().names().getFirst()))
            chCtx.pipeline().addAfter(NetworkManager.IDLE_HANDLER_NAME, TLS_HANDLER_NAME, sslHandler);
        else
            chCtx.pipeline().addFirst(TLS_HANDLER_NAME, sslHandler);

        // Start the TLS handshake
        sslHandler.channelActive(chCtx);
    }

    @Override
    public String toString() {
        return "TLSOutConnectionHandler{" +
                "peer=" + peer +
                ", attributes=" + peerAttributes +
                ", channel=" + channel +
                '}';
    }

}
