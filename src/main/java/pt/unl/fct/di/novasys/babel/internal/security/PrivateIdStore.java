package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;

import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;

public class PrivateIdStore extends IdStore {
    public PrivateIdStore() {
        super(new KeyStore.PasswordProtection(new char[0])); //TODO using empty passwords for now
    }

    public PrivateKeyEntry getCredential(byte[] id) {
        try {
            return (PrivateKeyEntry) keyStore.getEntry(PeerIdEncoder.encodeToString(id), protParam);
        } catch (NoSuchAlgorithmException|UnrecoverableEntryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (KeyStoreException never) {
            assert false; // The keystore has to have been initialized at this point
            return null;
        }
    }

    public void setCredential(byte[] id, PrivateKey privateKey, Certificate certificate) {
        try {
            keyStore.setEntry(
                PeerIdEncoder.encodeToString(id),
                new KeyStore.PrivateKeyEntry(privateKey, new Certificate[]{certificate}),
                protParam);
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
