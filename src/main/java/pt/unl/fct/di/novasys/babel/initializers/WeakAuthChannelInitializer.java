package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.secure.SecureChannelListener;
import pt.unl.fct.di.novasys.channel.secure.weakauth.WeakAuthChannel;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

import java.io.IOException;
import java.util.Properties;

/**
 * {@link SecureChannelInitializer} that creates a {@link WeakAuthChannel}, a channel that
 * authenticates peers but tolerates weaker certificate validation (e.g. self-signed or
 * first-use-trust semantics) compared to the strict {@link AuthChannelInitializer}.
 */
public class WeakAuthChannelInitializer implements SecureChannelInitializer<WeakAuthChannel<BabelMessage>> {

    /**
     * Creates and returns a new {@link WeakAuthChannel} with the given key/trust managers and properties.
     *
     * @param serializer   the serializer for {@link BabelMessage}s
     * @param listener     the secure channel event listener
     * @param keyManager   provides the local identity's private key and certificate chain
     * @param trustManager validates incoming peer certificates under the weak-auth policy
     * @param properties   channel configuration properties
     * @param protoId      the owning protocol's numeric ID
     * @return the initialized weak-auth channel
     * @throws IOException if the channel cannot bind or start
     */
    @Override
    public WeakAuthChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer,
            SecureChannelListener<BabelMessage> listener, X509IKeyManager keyManager, X509ITrustManager trustManager,
            Properties properties, short protoId) throws IOException {
        return new WeakAuthChannel<>(serializer, listener, properties, keyManager, trustManager);
    }

}
