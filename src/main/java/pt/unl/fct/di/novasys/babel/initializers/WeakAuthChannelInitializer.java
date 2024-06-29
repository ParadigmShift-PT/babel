package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.secure.SecureChannelListener;
import pt.unl.fct.di.novasys.channel.secure.weakauth.WeakAuthChannel;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

import java.io.IOException;
import java.util.Properties;

public class WeakAuthChannelInitializer implements SecureChannelInitializer<WeakAuthChannel<BabelMessage>> {

    @Override
    public WeakAuthChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer,
            SecureChannelListener<BabelMessage> listener, X509IKeyManager keyManager, X509ITrustManager trustManager,
            Properties properties, short protoId) throws IOException {
        return new WeakAuthChannel<>(serializer, listener, properties, keyManager, trustManager);
    }

}
