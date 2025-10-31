package pt.unl.fct.di.novasys.network.tls.pipeline;

import io.netty.channel.*;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import pt.unl.fct.di.novasys.network.NetworkManager;
import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.listeners.MessageListener;
import pt.unl.fct.di.novasys.network.listeners.InConnListener;
import pt.unl.fct.di.novasys.network.pipeline.InConnectionHandler;
import pt.unl.fct.di.novasys.network.pipeline.MessageDecoder;
import pt.unl.fct.di.novasys.network.pipeline.MessageEncoder;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;
import pt.unl.fct.di.novasys.network.tls.userevents.PreTLSHandshakeCompleted;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

import static pt.unl.fct.di.novasys.network.tls.TLSChannelHandlerFactory.TLS_HANDLER_NAME;

public class InTLSConnectionHandler<T> extends InConnectionHandler<T> {

    private static final Logger logger = LogManager.getLogger(InTLSConnectionHandler.class);

    private X509IKeyManager keyManager;
    private X509ITrustManager trustManager;

    public InTLSConnectionHandler(long connectionId, InConnListener<T> listener,
            MessageListener<T> consumer, EventLoop loop, Attributes selfAttrs,
            MessageEncoder<T> encoder, MessageDecoder<T> decoder,
            X509IKeyManager keyManager, X509ITrustManager trustManager) {
        super(connectionId, listener, consumer, loop, selfAttrs, encoder, decoder);

        this.keyManager = keyManager;
        this.trustManager = trustManager;
    }

    @Override
    public void internalUserEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof PreTLSHandshakeCompleted hsEvt) {
            this.peerAttributes = hsEvt.getAttr();
            logger.debug("Pre TLS in handshake completed from: " + peer);
            try {
                addAndWaitTLSHandler(ctx, hsEvt.getSelectedId(), hsEvt.getPeerId());
            } catch (Exception e) {
                logger.error("SSLHandler creation in connection to {} failed with exception: {}", peer, e);
                exceptionCaught(ctx, e);
            }
        } else if (logger.isWarnEnabled() && !(evt instanceof SslHandshakeCompletionEvent)) {
            logger.warn("Unknown user event caught: " + evt);
        }
        // TODO handle SslHandler events
    }

    private void addAndWaitTLSHandler(ChannelHandlerContext chCtx, byte[] selectedId, byte[] peerId)
            throws Exception {
        var sslCtx = SslContextBuilder
                // TODO should we use this or the subkeymanager directly?
                .forServer(keyManager.getPrivateKey(selectedId), keyManager.getCertificateChain(selectedId))
                .trustManager(trustManager.singleTrustManager(peerId))
                .clientAuth(ClientAuth.REQUIRE)
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
                .startTls(true)
                .build();

        var sslHandler = sslCtx.newHandler(chCtx.alloc(), peer.getAddress().getHostAddress(), peer.getPort(), loop);

        sslHandler.handshakeFuture().addListener(f -> {
            loop.execute(() -> {
                if (f.isSuccess()) {
                    outsideUp = true;
                    listener.inboundConnectionUp(InTLSConnectionHandler.this);
                } else {
                    exceptionCaught(chCtx, f.cause());
                }
            });
        });

        if (NetworkManager.IDLE_HANDLER_NAME.equals(chCtx.pipeline().names().getFirst()))
            chCtx.pipeline().addAfter(NetworkManager.IDLE_HANDLER_NAME, TLS_HANDLER_NAME, sslHandler);
        else
            chCtx.pipeline().addFirst(TLS_HANDLER_NAME, sslHandler);
    }

    @Override
    public String toString() {
        return "InTLSConnectionHandler{" +
                "peer=" + peer +
                ", attributes=" + peerAttributes +
                ", channel=" + channel +
                '}';
    }

}
