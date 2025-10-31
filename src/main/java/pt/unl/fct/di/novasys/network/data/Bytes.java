package pt.unl.fct.di.novasys.network.data;

import java.util.Arrays;
import java.util.Base64;

/**
 * byte[] wrapper with {@link #hashCode()} and {@link #equals(Object)} implementations
 * so byte arrays can be used in hash maps/sets.
 */
public class Bytes implements Cloneable, Comparable<Bytes> {

    private static final Base64.Encoder base64 = Base64.getEncoder();

    private final byte[] bytes;

    public Bytes(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * @return A wrapper for the given array, or {@code null} if the array is null.
     */
    public static Bytes of(byte[] bytes) {
        return bytes == null ? null : new Bytes(bytes);
    }

    /**
     * @return the wrapped byte array.
     */
    public byte[] array() {
        return bytes;
    }

    public int length() {
        return bytes.length;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    public boolean equals(byte[] other) {
        if (other == null)
            return false;
        else
            return Arrays.equals(bytes, other);
    }

    public boolean equals(Bytes other) {
        if (other == null)
            return false;
        else
            return Arrays.equals(bytes, other.bytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (obj instanceof Bytes other)
            return Arrays.equals(bytes, other.bytes);
        else if (obj instanceof byte[] other)
            return Arrays.equals(bytes, other);
        else
            return false;
    }

    @Override
    public int compareTo(Bytes other) {
        return Arrays.compare(this.bytes, other.bytes);
    }

    @Override
    public Bytes clone() {
        return new Bytes(bytes.clone());
    }

    @Override
    public String toString() {
        return base64.encodeToString(bytes);
    }

}
