package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.secure.SecureChannelListener;
import pt.unl.fct.di.novasys.channel.secure.SecureIChannel;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

import java.io.IOException;
import java.util.Properties;

public interface SecureChannelInitializer<T extends SecureIChannel<BabelMessage>> {

    T initialize(ISerializer<BabelMessage> serializer, SecureChannelListener<BabelMessage> listener,
            X509IKeyManager keyManager, X509ITrustManager trustManager,
            Properties properties, short protoId)
            throws IOException;
}
