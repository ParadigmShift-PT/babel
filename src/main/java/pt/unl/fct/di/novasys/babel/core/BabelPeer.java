package pt.unl.fct.di.novasys.babel.core;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import pt.unl.fct.di.novasys.babel.internal.PeerIdEncoder;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * TODO docs
 */
public class BabelPeer {
    private static final int DEFAULT_HOSTS_NUM = 1;

    private final byte[] peerId;
    private final Set<Host> hosts;

    public BabelPeer(byte[] peerId) {
        this.peerId = peerId;
        this.hosts = HashSet.newHashSet(DEFAULT_HOSTS_NUM);
    }

    public BabelPeer(byte[] peerId, PublicKey publicKey) {
        this.peerId = peerId;
        this.hosts = ConcurrentHashMap.newKeySet(DEFAULT_HOSTS_NUM);
    }

    public byte[] getId() {
        byte[] copy = new byte[peerId.length];
        System.arraycopy(peerId, 0, copy, 0, peerId.length);
        return copy;
    }

    public Host getHost() {
        return hosts.iterator().next();
    }

    public Iterator<Host> getHosts() {
        return hosts.iterator();
    }

    public void addHost(Host newHost) {
        hosts.add(newHost);
    }

    public boolean removeHost(Host host) {
        return hosts.remove(host);
    }

    /**
     * A string encoded form of this peer's id.
     */
    @Override
    public String toString() {
        return PeerIdEncoder.encodeToString(peerId);
    }
}
