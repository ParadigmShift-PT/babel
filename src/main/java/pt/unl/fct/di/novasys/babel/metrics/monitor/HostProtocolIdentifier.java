package pt.unl.fct.di.novasys.babel.metrics.monitor;

public class HostProtocolIdentifier {
        private String node;
        private short protocolId;

        public HostProtocolIdentifier(String host, short protocolId) {
            this.node = host;
            this.protocolId = protocolId;
        }

        public String getHost() {
            return node;
        }

        public short getProtocolId() {
            return protocolId;
        }

        @Override
        public int hashCode() {
            return node.hashCode() + protocolId;
        }

}
