
package pt.unl.fct.di.novasys.babel.internal.security;

import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.KeyStoreException;
import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;

// This is separate from PrivateIdStore because its contents change frequently,
// and would probably be stored in different files, if at all.
// TODO should there also be one like for symmetric keys?
public class PublicIdStore extends IdStore {
    public PublicIdStore() {
        super(new KeyStore.PasswordProtection(new char[0]));
    }

    public Certificate getCertificate(byte[] peerId) {
        try {
            return keyStore.getCertificate(PeerIdEncoder.encodeToString(peerId));
        } catch (KeyStoreException never) {
            assert false; // The keystore has to have been initialized at this point
            return null;
        }
    }

    public void setCertificate(byte[] peerId, Certificate certificate) {
        try {
            keyStore.setCertificateEntry(PeerIdEncoder.encodeToString(peerId), certificate);
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
