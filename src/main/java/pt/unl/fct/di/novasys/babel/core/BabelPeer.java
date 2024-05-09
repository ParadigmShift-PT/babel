package pt.unl.fct.di.novasys.babel.core;

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import pt.unl.fct.di.novasys.network.data.Host;

/**
 * TODO docs
 */
public class BabelPeer {
    private static final int DEFAULT_HOSTS_NUM = 1;

    private final byte[] peerId;
    private final Set<Host> hosts;
    // TODO should this be a certificate instead? And should this be a set or just a single element?
    private PublicKey publicKey;

    public BabelPeer(byte[] peerId) {
        this.peerId = peerId;
        this.hosts = HashSet.newHashSet(DEFAULT_HOSTS_NUM);
    }

    public BabelPeer(byte[] peerId, PublicKey publicKey) {
        this.peerId = peerId;
        this.hosts = ConcurrentHashMap.newKeySet(DEFAULT_HOSTS_NUM);
        this.publicKey = publicKey;
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

    // TODO throw no public key exception
    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey newPublicKey) {
        publicKey = newPublicKey;
    }

    /**
     * A base64 encoded string of this peer's id.
     */
    @Override
    public String toString() {
        return Base64.getEncoder().encodeToString(peerId);
    }
}
