package pt.unl.fct.di.novasys.babel.core;

import java.security.KeyStore;
import java.security.Provider;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import pt.unl.fct.di.novasys.babel.internal.security.IdAliasMapper;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.security.X509ITrustManager;

// TODO docs
class SecurityCore {
    private final Provider provider = new BouncyCastleProvider();

    private final KeyStore keyStore;
    private final String keyStorePath;
    private final IdAliasMapper idAliasMapper;

    private final Map<Short, String> defaultProtoKeys;

    private final KeyStore trustStore;
    private final String trustStorePath;

    // TODO allow to get a protocol's default alias?
    private final KeyStore secretStore;
    private final String secretStorePath;
    private final Map<Short, String> defaultProtoSecrets;

    private final X509IKeyManager keyManager;
    private final X509ITrustManager trustManager;

    SecurityCore(KeyStore privKeyStore, String privKeyStorePath, KeyStore secretKeyStore, String secretKeyStorePath,
            KeyStore trustStore, String trustStorePath, X509IKeyManager keyManager, X509ITrustManager trustManager,
            IdAliasMapper idAliasMapper) {
        this.keyStore = privKeyStore;
        this.keyStorePath = privKeyStorePath;
        this.idAliasMapper = idAliasMapper;

        this.secretStore = secretKeyStore;
        this.secretStorePath = secretKeyStorePath;

        this.trustStore = trustStore;
        this.trustStorePath = trustStorePath;

        this.keyManager = keyManager;
        this.trustManager = trustManager;

        this.defaultProtoKeys = new ConcurrentHashMap<>();
        this.defaultProtoSecrets = new ConcurrentHashMap<>();
    }

}
