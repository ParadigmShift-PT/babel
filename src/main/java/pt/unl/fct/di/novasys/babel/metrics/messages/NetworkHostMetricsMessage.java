package pt.unl.fct.di.novasys.babel.metrics.messages;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;

import java.io.*;

/**
 * A network message carrying a {@link NodeSample} together with the string identifier of the
 * originating host, serialisable to and from a raw byte array for transport over a socket.
 */
public class NetworkHostMetricsMessage{

    private NodeSample sample;
    private final String host;

    /**
     * Constructs a new {@code NetworkHostMetricsMessage}.
     *
     * @param host   string identifier of the host that produced the sample
     * @param sample the metrics snapshot from that host
     */
    public NetworkHostMetricsMessage(String host, NodeSample sample) {
        this.sample = sample;
        this.host = host;
    }

    /**
     * Returns the metrics snapshot carried by this message.
     *
     * @return the {@link NodeSample}
     */
    public NodeSample getSample() {
        return this.sample;
    }

    /**
     * Returns the string identifier of the host that produced the sample.
     *
     * @return the host identifier
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Serialises this message to a byte array, writing the host string followed by the serialised sample.
     *
     * @return a byte array representing this message
     * @throws IOException if serialisation fails
     */
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeUTF(this.host);
        dos.write(sample.toByteArray());
        return out.toByteArray();
    }

    /**
     * Deserialises a {@code NetworkHostMetricsMessage} from a byte array previously produced by {@link #toByteArray()}.
     *
     * @param data the byte array to deserialise
     * @return the reconstructed {@code NetworkHostMetricsMessage}
     * @throws IOException if deserialisation fails
     */
    public static NetworkHostMetricsMessage fromByteArray(byte[] data) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);

        String host = dis.readUTF();
        byte[] sampleData = new byte[dis.available()];
        dis.read(sampleData);
        NodeSample mres = NodeSample.fromByteArray(sampleData);
        return new NetworkHostMetricsMessage(host, mres);
    }




}
