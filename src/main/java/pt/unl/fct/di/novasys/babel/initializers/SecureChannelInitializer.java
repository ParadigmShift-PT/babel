package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.secure.SecureChannelListener;
import pt.unl.fct.di.novasys.channel.secure.SecureIChannel;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

import java.io.IOException;
import java.util.Properties;

/**
 * Factory interface for creating and configuring a secure (authenticated/encrypted) Babel channel.
 *
 * <p>Extends the plain {@link ChannelInitializer} contract with key-management parameters.
 * Implementations are passed to {@code Babel.createChannel} to instantiate channels that
 * perform TLS or challenge-response authentication (e.g. {@code AuthChannelInitializer},
 * {@code WeakAuthChannelInitializer}).
 *
 * @param <T> the concrete {@link SecureIChannel} type produced by this initializer
 */
public interface SecureChannelInitializer<T extends SecureIChannel<BabelMessage>> {

    /**
     * Creates and returns an initialized secure channel instance.
     *
     * @param serializer   the message serializer used to encode/decode {@link BabelMessage}s
     * @param listener     the listener that receives secure channel events and delivered messages
     * @param keyManager   provides the local identity's private key and certificate chain
     * @param trustManager validates the peer's certificate against trusted identities
     * @param properties   channel-specific configuration properties
     * @param protoId      the protocol ID of the owning protocol
     * @return the initialized secure channel
     * @throws IOException if the channel cannot bind or connect
     */
    T initialize(ISerializer<BabelMessage> serializer, SecureChannelListener<BabelMessage> listener,
            X509IKeyManager keyManager, X509ITrustManager trustManager,
            Properties properties, short protoId)
            throws IOException;
}
