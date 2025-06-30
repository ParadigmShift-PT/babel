package pt.unl.fct.di.novasys.babel.metrics;

import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a sample of metrics collected from a node at a specific point in time.<br>
 * This class holds samples for different protocols identified by their protocol IDs.
 */
public class NodeSample implements Serializable {

    private final Map<Short, ProtocolSample> samplesPerProtocol;

    public NodeSample() {
        this.samplesPerProtocol = new HashMap<>();
    }

    public void addProtocolSample(short protocolID, ProtocolSample protocolSample){
        this.samplesPerProtocol.put(protocolID, protocolSample);
    }

    /**
     * Returns the set of protocol IDs for which samples are available in this node sample.
     * @return a set of protocol IDs
     */
    public Set<Short> getProtocols(){
        return samplesPerProtocol.keySet();
    }

    public ProtocolSample getProtocolSample(short protocolID) throws NoSuchProtocolRegistry {
        if(!samplesPerProtocol.containsKey(protocolID)){
            throw new NoSuchProtocolRegistry(protocolID);
        }
        return samplesPerProtocol.get(protocolID);

    }

    public Map<Short, ProtocolSample> getSamplesPerProtocol() {
        return samplesPerProtocol;
    }

    public static NodeSample fromByteArray(byte[] data){
        try {
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
            return (NodeSample) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] toByteArray(){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(this);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
