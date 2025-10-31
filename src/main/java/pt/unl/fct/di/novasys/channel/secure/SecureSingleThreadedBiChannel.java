package pt.unl.fct.di.novasys.channel.secure;

import pt.unl.fct.di.novasys.channel.base.SingleThreadedBiChannel;
import pt.unl.fct.di.novasys.network.data.Host;

public abstract class SecureSingleThreadedBiChannel<T, Y> extends SingleThreadedBiChannel<T, Y>
        implements SecureIChannel<T> {

    //private static final Logger logger = LogManager.getLogger(SecureSingleThreadedBiChannel.class);

    public SecureSingleThreadedBiChannel(String threadName) {
        super(threadName);
    }

    @Override
    public void sendMessage(T msg, byte[] peerId, int connection) {
        loop.execute(() -> onSendMessage(msg, peerId, connection));
    }

    protected abstract void onSendMessage(T msg, byte[] peerId, int connection);

    @Override
    public void closeConnection(byte[] peerId, int connection) {
        loop.execute(() -> onCloseConnection(peerId, connection));
    }

    protected abstract void onCloseConnection(byte[] peerId, int connection);

    @Override
    public void openConnection(Host peer, byte[] peerId, int connection) {
        loop.execute(() -> onOpenConnection(peer, peerId, connection));
    }

    protected abstract void onOpenConnection(Host peer, byte[] peerId, int connection);

}
