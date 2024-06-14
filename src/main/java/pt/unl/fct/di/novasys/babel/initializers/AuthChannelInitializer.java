package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.secure.SecureChannelListener;
import pt.unl.fct.di.novasys.channel.secure.auth.AuthChannel;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

import java.io.IOException;
import java.util.Properties;

public class AuthChannelInitializer implements SecureChannelInitializer<AuthChannel<BabelMessage>> {

    @Override
    public AuthChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer,
            SecureChannelListener<BabelMessage> listener, X509ITrustManager trustManager, X509IKeyManager keyManager,
            Properties properties, short protoId) throws IOException {
        return new AuthChannel<>(serializer, listener, properties, keyManager, trustManager);
    }
}
