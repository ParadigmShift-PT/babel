package pt.unl.fct.di.novasys.babel.internal.security;

import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.core.security.IdFromCertExtractor;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;

// TODO make a Babel keystore generator program for BabelCommons?
public class X509BabelKeyManager extends X509IKeyManager {
    private final static Logger logger = LogManager.getLogger(X509BabelKeyManager.class);

    private final IdAliasMapper idAliasMapper;

    private final KeyStore keyStore;
    private final ProtectionParameter protParam;

    /**
     * @throws KeyStoreException if the keystore has not been initialized (loaded).
     */
    public X509BabelKeyManager(KeyStore keyStore, ProtectionParameter protParam, IdFromCertExtractor idExtractor) throws KeyStoreException {
        this.keyStore = keyStore;
        this.protParam = protParam;
        this.idAliasMapper = new IdAliasMapper();
        idAliasMapper.populateFromPrivateKeyStore(keyStore, protParam, idExtractor);
    }

    /**
     * @throws KeyStoreException if the keystore has not been initialized (loaded).
     */
    public X509BabelKeyManager(KeyStore keyStore, ProtectionParameter protParam, IdAliasMapper idAliasMapper) throws KeyStoreException {
        keyStore.size(); // Trigger KeyStoreException if keyStore was not initialized.

        this.keyStore = keyStore;
        this.protParam = protParam;
        this.idAliasMapper = idAliasMapper;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        try {
            ArrayList<String> aliases;
            aliases = new ArrayList<>(keyStore.size());
            keyStore.aliases().asIterator().forEachRemaining(aliases::add);
            return aliases.toArray(new String[aliases.size()]);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e); // Won't happen
        }
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return getServerAliases(keyType, issuers);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return idAliasMapper.getDefaultAlias();
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return idAliasMapper.getDefaultAlias();
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        try {
            var chain = keyStore.getCertificateChain(alias);

            var x509Chain = new X509Certificate[chain.length];
            for (int i = 0; i < chain.length; i++)
                x509Chain[i] = (X509Certificate) chain[i];

            return x509Chain;
        } catch (ClassCastException e) {
            logger.error("getCertificateChain(%s): Couldn't cast Certificate[] to X509Certificate[]", alias);
            return null;
        } catch (KeyStoreException e) {
            throw new RuntimeException(e); // Won't happen
        }
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        try {
            return ((PrivateKeyEntry) keyStore.getEntry(alias, protParam)).getPrivateKey();
        } catch (ClassCastException e) {
            logger.error("getPrivateKey({}) failed because the alias didn't refer to a private key entry", alias);
            return null;
        } catch (UnrecoverableEntryException | NoSuchAlgorithmException e) {
            logger.error("getPrivateKey({}) failed with exception: {}", alias, e);
            return null;
        } catch (KeyStoreException never) {
            throw new AssertionError(never); // Won't happen
        }
    }

    @Override
    public X509Certificate[] getCertificateChain(byte[] id) {
        return getCertificateChain(idAliasMapper.getAlias(id));
    }

    @Override
    public PrivateKey getPrivateKey(byte[] id) {
        return getPrivateKey(idAliasMapper.getAlias(id));
    }

    @Override
    public String getIdAlias(byte[] id) {
        return idAliasMapper.getAlias(id);
    }

    @Override
    public byte[] getAliasId(String alias) {
        return idAliasMapper.getId(alias);
    }

}
