package pt.unl.fct.di.novasys.babel.metrics.messages;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;

import java.io.*;

public class NetworkHostMetricsMessage{

    private NodeSample sample;
    private final String host;

    public NetworkHostMetricsMessage(String host, NodeSample sample) {
        this.sample = sample;
        this.host = host;
    }

    public NodeSample getSample() {
        return this.sample;
    }

    public String getHost() {
        return this.host;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeUTF(this.host);
        dos.write(sample.toByteArray());
        return out.toByteArray();
    }

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
