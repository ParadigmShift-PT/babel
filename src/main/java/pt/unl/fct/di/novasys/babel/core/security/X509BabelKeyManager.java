package pt.unl.fct.di.novasys.babel.core.security;

import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pt.unl.fct.di.novasys.babel.internal.security.keystore.IdAliasMapper;
import pt.unl.fct.di.novasys.babel.internal.security.keystore.PeerIdAliasMapper;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;

// TODO make a Babel keystore generator program for BabelCommons?
public class X509BabelKeyManager extends X509IKeyManager {
    private final static Logger logger = LogManager.getLogger(X509BabelKeyManager.class);

    static final String DEFAULT_ID_ALIAS = "default";

    private final IdAliasMapper idAliasMapper;

    private final KeyStore keyStore;
    private final char[] pwd;

    /**
     * Constructs a new X509BabelKeyManager with {@link PeerIdAliasMapper}.
     * 
     * @throws KeyStoreException if the keystore has not been initialized (loaded).
     */
    public X509BabelKeyManager(KeyStore keyStore, String pwd) throws KeyStoreException {
        this(keyStore, pwd.toCharArray());
    }

    /**
     * Constructs a new X509BabelKeyManager with {@link PeerIdAliasMapper}.
     * 
     * @throws KeyStoreException if the keystore has not been initialized (loaded).
     */
    public X509BabelKeyManager(KeyStore keyStore, char[] pwd) throws KeyStoreException {
        this(keyStore, pwd, new PeerIdAliasMapper());
    }

    /**
     * @throws KeyStoreException if the keystore has not been initialized (loaded).
     */
    public X509BabelKeyManager(KeyStore keyStore, String pwd, IdAliasMapper idAliasMapper) throws KeyStoreException {
        this(keyStore, pwd.toCharArray(), idAliasMapper);
    }

    /**
     * @throws KeyStoreException if the keystore has not been initialized (loaded).
     */
    public X509BabelKeyManager(KeyStore keyStore, char[] pwd, IdAliasMapper idAliasMapper) throws KeyStoreException {
        keyStore.size(); // Trigger KeyStoreException

        this.keyStore = keyStore;
        this.pwd = pwd;
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
            return (X509Certificate[]) keyStore.getCertificateChain(alias);
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
            return (PrivateKey) keyStore.getKey(alias, pwd);
        } catch (ClassCastException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
            logger.error("getPrivateKey(%s) failed with exception: %s", alias, e);
            return null;
        } catch (KeyStoreException e) {
            throw new RuntimeException(e); // Won't happen
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
