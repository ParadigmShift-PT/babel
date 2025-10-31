package pt.unl.fct.di.novasys.network.security;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import pt.unl.fct.di.novasys.network.data.Bytes;

class X509SubKeyManager extends X509IKeyManager {
    final X509IKeyManager man;
    final Set<String> validAliases;
    final Set<Bytes> validIds;

    //final boolean cache;
    //Optional<PrivateKey> cachedKey;
    //Optional<X509Certificate[]> cachedCert;

    X509SubKeyManager(X509IKeyManager wrappedManager, Collection<String> aliases, Collection<byte[]> ids) {
        man = wrappedManager;
        validAliases = new HashSet<>(aliases);
        validIds = ids.stream().map(Bytes::of).collect(Collectors.toSet());
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return Arrays.stream(man.getClientAliases(keyType, issuers))
                .filter(validAliases::contains)
                .toArray(String[]::new);
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        String alias = man.chooseClientAlias(keyType, issuers, socket);
        if (!validAliases.contains(alias))
            alias = validAliases.stream().findAny().orElse(null);
        return alias;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return Arrays.stream(man.getServerAliases(keyType, issuers))
                .filter(validAliases::contains)
                .toArray(String[]::new);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        String alias = man.chooseServerAlias(keyType, issuers, socket);
        if (!validAliases.contains(alias))
            alias = validAliases.stream().findAny().orElse(null);
        return alias;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return validAliases.contains(alias) ? man.getCertificateChain(alias) : null;
    }

    @Override
    public X509Certificate[] getCertificateChain(byte[] id) {
        return validIds.contains(Bytes.of(id)) ? man.getCertificateChain(id) : null;
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return validAliases.contains(alias) ? man.getPrivateKey(alias) : null;
    }

    @Override
    public PrivateKey getPrivateKey(byte[] id) {
        return validIds.contains(Bytes.of(id)) ? man.getPrivateKey(id) : null;
    }

    @Override
    public String getIdAlias(byte[] id) {
        return validIds.contains(Bytes.of(id)) ? man.getIdAlias(id) : null;
    }

    @Override
    public byte[] getAliasId(String alias) {
        return validAliases.contains(alias) ? man.getAliasId(alias) : null;
    }

}
