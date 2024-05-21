package pt.unl.fct.di.novasys.babel.initializers;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.channel.signed.CertificateVerifier;
import pt.unl.fct.di.novasys.channel.signed.SignedTCPChannel;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Properties;

public class SignedTCPChannelInitializer implements ChannelInitializer<SignedTCPChannel<BabelMessage>> {
    private final KeyStore idStore;
    private final char[] pwd;
    private final String idAlias;
    private final CertificateVerifier verifier;

    public SignedTCPChannelInitializer(KeyStore idStore, char[] pwd, String idAlias,
            CertificateVerifier verifier) {
        this.idStore = idStore;
        this.pwd = pwd;
        this.idAlias = idAlias;
        this.verifier = verifier;
    }

    @Override
    public SignedTCPChannel<BabelMessage> initialize(ISerializer<BabelMessage> serializer,
                                                     ChannelListener<BabelMessage> list,
                                                     Properties properties, short protoId)
                                          throws IOException {
        // TODO use ProtParam instead of char[] password?
        return new SignedTCPChannel(serializer, list, properties, idStore, idAlias, pwd, verifier);
    }
}
