package pt.unl.fct.di.novasys.channel.secure.tls;

import pt.unl.fct.di.novasys.network.Connection;
import pt.unl.fct.di.novasys.network.data.Host;

import java.util.LinkedList;
import java.util.Queue;

public class IdentifiedConnectionState<T> {

    private final Connection<T> connection;
    private boolean connected;
    private final Queue<T> queue;

    private final Host peerSocket;

    private byte[] peerId;

    public IdentifiedConnectionState(Connection<T> conn, Host peerSocket) {
        this.connection = conn;
        this.connected = false;
        this.queue = new LinkedList<>();
        this.peerSocket = peerSocket;
    }

    public IdentifiedConnectionState(Connection<T> conn, Host peerSocket, Queue<T> initialQueue) {
        this.connection = conn;
        this.connected = false;
        this.queue = new LinkedList<>(initialQueue);
        this.peerSocket = peerSocket;
    }

    public Connection<T> getConnection() {
        return connection;
    }

    public long getConnectionId() {
        return connection.getConnectionId();
    }

    public Queue<T> getQueue() {
        return queue;
    }

    public Host getPeerListenAddress() {
        return peerSocket;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected() {
        connected = true;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public void setPeerId(byte[] peerId) {
        this.peerId = peerId;
    }

    public void disconnect() {
        queue.clear();
        connection.disconnect();
        connected = false;
    }

}
