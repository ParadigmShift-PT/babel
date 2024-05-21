package pt.unl.fct.di.novasys.babel.internal.security;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

/**
 * TODO docs
 */
public abstract class IdStore {
    protected static String KEY_STORE_TYPE = "PKCS12";
    protected static final String KEY_STORE_PROVIDER = "BC";

    protected final KeyStore keyStore;
    protected ProtectionParameter protParam;

    public IdStore(ProtectionParameter protParam) {
        KeyStore keyStore = null;
        try {
            try {
                keyStore = KeyStore.getInstance(KEY_STORE_TYPE, KEY_STORE_PROVIDER);
            } catch (NoSuchProviderException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            keyStore.load(null, null); //TODO change this, obviously
        } catch (NoSuchAlgorithmException|CertificateException|IOException e) {
            // TODO ignore for now
            e.printStackTrace();
        } catch (KeyStoreException never) { assert false; }
        this.keyStore = keyStore;
        this.protParam = protParam;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    //TODO load(String filepath)
    //TODO store(String filepath)

    /*
    private static final char ALIAS_SEP = '.';
    private static String alias(String... args) {
        int length = args.length-1;
        for (String s : args)
            if (s != null)
                length += s.length();
        StringBuilder str = new StringBuilder(length);
        str.append(args[0]);
        for (int i = 1; i < args.length; i++)
            if (args[i] != null)
                str.append(ALIAS_SEP).append(args[i]);
        return str.toString();
    }
    private static final String PRIV_KEY_ALIAS = "p";
    private static final String SECR_KEY_ALIAS = "s";
    private static final String CERT_ALIAS = "c";

    /**
     * Aliases are defined in a directory-like fashion, with the default for each
     * cryptgraphic type being simply the name of the "directory". E.g.: <ul>
     * <li> My default private key: {@link PRIV_KEY_ALIAS} </li>
     * <li> My private key 203: {@link PRIV_KEY_ALIAS}.203 </li>
     * <li> Node abc1234's default certificate: {@link CERT_ALIAS}.abc1234 </li>
     * <li> Node abc1234's certificate 123: {@link CERT_ALIAS}.abc1234.123 </li>
     * </ul>
     * /
    private final KeyStore keyStore; //TODO maybe separate into session kesyStore and persistentKeyStore?
    private ProtectionParameter protParam;

    private final CryptUtils crypt;

    private static BabelKeyStore instance;
    public static BabelKeyStore getInstance() {
        if (instance == null)
            instance = new BabelKeyStore();
        return instance;
    }

    private BabelKeyStore() {
        KeyStore keyStore = null;
        try {
            try {
                keyStore = KeyStore.getInstance(KEY_STORE_TYPE, KEY_STORE_PROVIDER);
            } catch (NoSuchProviderException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            keyStore.load(null, null); //TODO change this, obviously
        } catch (NoSuchAlgorithmException|CertificateException|IOException e) {
            // TODO ignore for now
            e.printStackTrace();
        } catch (KeyStoreException never) { assert false; }
        this.keyStore = keyStore;
        this.protParam = new KeyStore.PasswordProtection(new char[0]); //TODO using empty passwords for now
        this.crypt = CryptUtils.getInstance();
    }

    public void putPrivateKey(byte[] id, PrivateKey privKey, Certificate certificate) throws InvalidKeyException {
        putPrivateKey(id, privKey, new Certificate[]{certificate});
    }


    public void putPrivateKey(byte[] id, PrivateKey privKey, Certificate[] certChain) throws InvalidKeyException {
        putPrivateKey(Hex.toHexString(id), privKey, certChain);
    }

    public PrivateKeyEntry getPrivateKey(byte[] peerId) {
        return getPrivateKey(Hex.toHexString(peerId));
    }

    public Certificate getCert(BabelPeer peer) {
        return getCert(peer, null);
    }

    public Certificate getCert(BabelPeer peer, Short certId) {
        return getCert(alias(peer.getIdHex(), Short.toString(certId)));
    }

    //----------------- Actually do stuff
    private void putPrivateKey(String alias, PrivateKey privKey, Certificate[] certChain) throws InvalidKeyException {
        try {
            KeyStore.PrivateKeyEntry pKeyEntry = new KeyStore.PrivateKeyEntry(privKey, certChain); // TODO should we store associated attributes?
            keyStore.setEntry(alias(PRIV_KEY_ALIAS,alias), pKeyEntry, protParam);
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private PrivateKeyEntry getPrivateKey(String alias) {
        try {
            return (PrivateKeyEntry) keyStore.getEntry(alias(PRIV_KEY_ALIAS,alias), protParam);
        } catch (KeyStoreException|NoSuchAlgorithmException|UnrecoverableEntryException e) {
            // TODO ignore for now
            e.printStackTrace();
            return null;
        }
    }

    private void putCert(String alias, Certificate cert) {
        try {
            // TODO should this be protected by protParam?
            keyStore.setCertificateEntry(alias(CERT_ALIAS,alias), cert);
        } catch (KeyStoreException e) {
            // TODO ignore for now
            e.printStackTrace();
        }
    }

    private Certificate getCert(String alias) {
        try {
            return keyStore.getCertificate(alias(CERT_ALIAS,alias));
        } catch (KeyStoreException e) {
            // TODO ignore for now
            e.printStackTrace();
            return null;
        }
    }

    private void putSecretKey(String alias, SecretKey sKey) {
        try {
            KeyStore.SecretKeyEntry sKeyEntry = new KeyStore.SecretKeyEntry(sKey);
            keyStore.setEntry(alias(SECR_KEY_ALIAS,alias), sKeyEntry, protParam);
        } catch (KeyStoreException e) {
            // TODO ignore for now
            e.printStackTrace();
        }
    }

    private SecretKey getSecretKey(String alias) {
        try {
            return (SecretKey) keyStore.getEntry(alias(SECR_KEY_ALIAS,alias), protParam);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
    */
}
