package pt.unl.fct.di.novasys.babel.channels;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Class representing a Peer. It shall be extended in the future for other
 * communication layers other than structured IP networks.
 */
public class Peer {
    private final InetSocketAddress peerAddress;

    public Peer(InetSocketAddress peerAddress) {
        this.peerAddress = peerAddress;
    }

    /**
     * @return the {@link InetSocketAddress} associated with this peer
     */
    public InetSocketAddress getPeerAddress() {
        return peerAddress;
    }

    /**
     * Encodes this <code>Peer</code> into a byte array
     * 
     * @return the byte array with the representation
     */
    public byte[] encode() {
        var addressBytes = peerAddress.getAddress().getAddress();
        var port = peerAddress.getPort();
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + addressBytes.length + Short.BYTES);
        buffer.putInt(addressBytes.length)
                .put(addressBytes)
                .putShort((short) port);
        return buffer.array();
    }

    /**
     * Decodes the given byte array into a <code>Peer</code> object
     * 
     * @param bytes the byte array with the information
     * @throws UnknownHostException if IP address is of illegal length
     * @return the decoded <code>Peer</code> object
     */
    public static Peer decode(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int addressLength = buffer.getInt();
        byte[] addressBytes = new byte[addressLength];
        buffer.get(addressBytes);
        int port = buffer.getShort();
        try {
            return new Peer(new InetSocketAddress(InetAddress.getByAddress(addressBytes), port));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
