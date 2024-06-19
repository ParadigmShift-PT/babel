package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;

import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;

// TODO remove
public class PrivateIdStore extends IdStore {
    public static final String DEFAULT_ALIAS = "default";

    public PrivateIdStore() {
        super(new KeyStore.PasswordProtection(new char[0])); //TODO using empty passwords for now
    }

    public PrivateKeyEntry getCredential(String alias) {
        try {
            return (PrivateKeyEntry) keyStore.getEntry(alias, protParam);
        } catch (NoSuchAlgorithmException|UnrecoverableEntryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (KeyStoreException never) {
            assert false; // The keystore has to have been initialized at this point
            return null;
        }
    }

    public PrivateKeyEntry getDefaultCredential() {
        return getCredential(DEFAULT_ALIAS);
    }

    public PrivateKeyEntry getCredential(byte[] id) {
        return getCredential(PeerIdEncoder.encodeToString(id));
    }

    public void setCredential(byte[] id, PrivateKey privateKey, Certificate certificate) {
        try {
            var entry = new KeyStore.PrivateKeyEntry(privateKey, new Certificate[]{certificate});
            keyStore.setEntry(PeerIdEncoder.encodeToString(id), entry, protParam);
            if (!keyStore.containsAlias(DEFAULT_ALIAS))
                keyStore.setEntry(DEFAULT_ALIAS, entry, protParam);
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
