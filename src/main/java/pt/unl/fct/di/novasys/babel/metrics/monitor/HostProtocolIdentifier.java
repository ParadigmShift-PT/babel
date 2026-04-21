package pt.unl.fct.di.novasys.babel.metrics.monitor;

/**
 * Composite key that uniquely identifies a (host, protocolId) pair within an aggregation result,
 * used to bucket per-host per-protocol metric samples.
 */
public class HostProtocolIdentifier {
        private String node;
        private short protocolId;

        /**
         * Constructs a new {@code HostProtocolIdentifier} for the given host and protocol ID.
         *
         * @param host       the string identifier of the host
         * @param protocolId the protocol ID
         */
        public HostProtocolIdentifier(String host, short protocolId) {
            this.node = host;
            this.protocolId = protocolId;
        }

        /**
         * Returns the host identifier.
         *
         * @return the host string
         */
        public String getHost() {
            return node;
        }

        /**
         * Returns the protocol ID.
         *
         * @return the protocol ID
         */
        public short getProtocolId() {
            return protocolId;
        }

        @Override
        public int hashCode() {
            return node.hashCode() + protocolId;
        }

}
