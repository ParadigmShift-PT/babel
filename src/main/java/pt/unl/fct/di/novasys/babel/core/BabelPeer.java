package pt.unl.fct.di.novasys.babel.core;

import java.security.cert.Certificate;
import java.util.HashSet;
import java.util.Set;

import org.bouncycastle.util.encoders.Hex;

import pt.unl.fct.di.novasys.network.data.Host;

/**
 * TODO This class does nothing for now. It's just a placeholder until I think of how IDs should be stored
 */
public class BabelPeer {
    private static final int DEFAULT_HOSTS_SIZE = 1;

    private final byte[] peerId;
    private final Set<Host> hosts;
    private Certificate certificate;

    public BabelPeer(byte[] peerId) {
        this.peerId = peerId;
        this.hosts = new HashSet<>(DEFAULT_HOSTS_SIZE);
    }

    public BabelPeer(byte[] peerId, Certificate certificate) {
        this.peerId = peerId;
        this.hosts = new HashSet<>(DEFAULT_HOSTS_SIZE);
        this.certificate = certificate;
    }


    public byte[] getId() {
        byte[] copy = new byte[peerId.length];
        System.arraycopy(peerId, 0, copy, 0, peerId.length);
        return copy;
    }

    public String getIdHex() {
        return Hex.toHexString(peerId);
    }

    public Host getHost() {
        return hosts.iterator().next();
    }

    // TODO throw no certificate exception
    public Certificate getCertificate() {
        return certificate;
    }

    public Certificate setCertificate(Certificate newCertificate) {
        var old = certificate;
        certificate = newCertificate;
        return old;
    }
}
