package pt.unl.fct.di.novasys.network.security;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

import pt.unl.fct.di.novasys.network.data.Bytes;

public abstract class X509ITrustManager extends X509ExtendedTrustManager {

    /** @see X509TrustManager#checkClientTrusted(X509Certificate[], String) */
    abstract public void checkClientTrusted(X509Certificate[] chain, byte[] expectedId, String authType)
            throws CertificateException;

    /** @see X509TrustManager#checkServerTrusted(X509Certificate[], String) */
    abstract public void checkServerTrusted(X509Certificate[] chain, byte[] expectedId, String authType)
            throws CertificateException;

    abstract public byte[] extractIdFromCertificate(X509Certificate certificate) throws CertificateException;

    /**
     * @return All the peer ids accepted by this trust manager, or {@code null} if
     *         any id is trusted as long as its consistent with the certificate.
     */
    abstract public Set<Bytes> getTrustedIds();

    /**
     * Adds an id to be trusted.
     */
    abstract public void addTrustedId(byte[] id);

    /**
     * Removes an id to be trusted.
     */
    abstract public void removeTrustedId(byte[] id);

    /**
     * Calls {@link #checkClientTrusted(X509Certificate[], String)} by
     * default. Override this implementation if needed.
     * <p>
     * <b>From {@link X509ExtendedTrustManager}:</b> <br>
     * {@inheritDoc}
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
            throws CertificateException {
        checkClientTrusted(chain, authType);
    }

    /**
     * Calls {@link #checkClientTrusted(X509Certificate[], String)} by
     * default. Override this implementation if needed.
     * <p>
     * <b>From {@link X509ExtendedTrustManager}:</b> <br>
     * {@inheritDoc}
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
            throws CertificateException {
        checkClientTrusted(chain, authType);
    }

    /**
     * Calls {@link #checkServerTrusted(X509Certificate[], String, Socket)} by
     * default. Override this implementation if needed.
     * <p>
     * <b>From {@link X509ExtendedTrustManager}:</b> <br>
     * {@inheritDoc}
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
            throws CertificateException {
        checkServerTrusted(chain, authType);
    }

    /**
     * Calls {@link #checkServerTrusted(X509Certificate[], String, Socket)} by
     * default. Override this implementation if needed.
     * <p>
     * <b>From {@link X509ExtendedTrustManager}:</b> <br>
     * {@inheritDoc}
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
            throws CertificateException {
        checkClientTrusted(chain, authType);
    }

    /**
     * Wraps this trust manager inside one that will only accept {@code trustedId}
     */
    public X509ITrustManager singleTrustManager(byte[] trustedId) {
        return new X509SingleTrustManager(this, trustedId);
    }

}
