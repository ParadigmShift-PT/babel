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

    /** Creates an empty {@code NodeSample} with a default-capacity backing map. */
    public NodeSample() {
        this.samplesPerProtocol = new HashMap<>();
    }

    /**
     * Creates an empty {@code NodeSample} with the given initial capacity for the backing map.
     *
     * @param size the expected number of protocol entries
     */
    public NodeSample(int size) {
        this.samplesPerProtocol = new HashMap<>(size);
    }

    /**
     * Adds or replaces the {@link ProtocolSample} for the given protocol ID.
     *
     * @param protocolID     the protocol identifier
     * @param protocolSample the sample to associate with that protocol
     */
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

    /**
     * Returns the {@link ProtocolSample} for the given protocol ID.
     *
     * @param protocolID the protocol identifier to look up
     * @return the sample for that protocol
     * @throws NoSuchProtocolRegistry if no sample exists for the given protocol ID
     */
    public ProtocolSample getProtocolSample(short protocolID) throws NoSuchProtocolRegistry {
        if(!samplesPerProtocol.containsKey(protocolID)){
            throw new NoSuchProtocolRegistry(protocolID);
        }
        return samplesPerProtocol.get(protocolID);

    }

    /**
     * Returns the full map from protocol ID to {@link ProtocolSample}.
     *
     * @return the unmodifiable backing map of protocol samples
     */
    public Map<Short, ProtocolSample> getSamplesPerProtocol() {
        return samplesPerProtocol;
    }

    /**
     * Deserializes a {@code NodeSample} from its Java-serialized byte representation.
     *
     * @param data the serialized bytes previously produced by {@link #toByteArray()}
     * @return the deserialized {@code NodeSample}
     * @throws RuntimeException if deserialization fails
     */
    public static NodeSample fromByteArray(byte[] data){
        try {
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
            return (NodeSample) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serializes this {@code NodeSample} to a Java-serialized byte array for network transmission or storage.
     *
     * @return the serialized bytes
     * @throws RuntimeException if serialization fails
     */
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
