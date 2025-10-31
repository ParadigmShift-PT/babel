package pt.unl.fct.di.novasys.channel.secure;

import pt.unl.fct.di.novasys.channel.IChannel;
import pt.unl.fct.di.novasys.network.data.Host;

public interface SecureIChannel<T> extends IChannel<T> {

    /**
     * TODO better docs?<p>
     * Possible exceptions returned to the listener on message failure are
     * {@link InvalidKeyException}, {@link NoSuchAlgorithmException}, {@link IOException}
     * or {@link IllegalStateException}.
     */
    void sendMessage(T msg, byte[] peerId, int connection);

    void closeConnection(byte[] peerId, int connection);

    void openConnection(Host peer, byte[] peerId, int connection);
}
