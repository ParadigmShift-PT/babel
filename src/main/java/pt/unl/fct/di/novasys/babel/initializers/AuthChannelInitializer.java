package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.secure.SecureChannelListener;
import pt.unl.fct.di.novasys.channel.secure.auth.AuthChannel;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

import java.io.IOException;
import java.util.Properties;

/**
 * {@link SecureChannelInitializer} that creates an {@link AuthChannel}, a mutually authenticated
 * secure channel where both peers present X.509 certificates before data is exchanged.
 */
public class AuthChannelInitializer implements SecureChannelInitializer<AuthChannel<BabelMessage>> {

    /**
     * Creates and returns a new {@link AuthChannel} with the given key/trust managers and properties.
     *
     * @param serializer   the serializer for {@link BabelMessage}s
     * @param listener     the secure channel event listener
     * @param keyManager   provides the local identity's private key and certificate chain
     * @param trustManager validates incoming peer certificates
     * @param properties   channel configuration properties
     * @param protoId      the owning protocol's numeric ID
     * @return the initialized authenticated channel
     * @throws IOException if the channel cannot bind or start
     */
    @Override
    public AuthChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer,
            SecureChannelListener<BabelMessage> listener, X509IKeyManager keyManager, X509ITrustManager trustManager,
            Properties properties, short protoId) throws IOException {
        return new AuthChannel<>(serializer, listener, properties, keyManager, trustManager);
    }
}
