package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.secure.SecureChannelListener;
import pt.unl.fct.di.novasys.channel.secure.auth.AuthChannel;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.security.IKeyManager;
import pt.unl.fct.di.novasys.network.security.ITrustManager;

import java.io.IOException;
import java.util.Properties;

public class AuthChannelInitializer implements SecureChannelInitializer<AuthChannel<BabelMessage>> {

    @Override
    public AuthChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer,
            SecureChannelListener<BabelMessage> listener, ITrustManager trustManager, IKeyManager keyManager,
            Properties properties, short protoId) throws IOException {
        return new AuthChannel<>(serializer, listener, properties, keyManager, trustManager);
    }
}
