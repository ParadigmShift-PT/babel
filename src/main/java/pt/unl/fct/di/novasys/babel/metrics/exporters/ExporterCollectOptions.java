package pt.unl.fct.di.novasys.babel.metrics.exporters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ExporterCollectOptions {
    private final Map<Short, ProtocolCollectOptions> perProtocolCollectOptions;
    boolean collectMetricsAllProtocols;

    short[] protocolsToCollect;

    boolean collectOSMetrics;



    /**
     * Builder for ExporterCollectOptions<br>
     * By default, all protocol metrics are collected along with the OS metrics.<br>
     * If you want to collect only specific protocols, use the protocolsToCollect method.<br>
     * If you want to specify {@link CollectOptions} for a protocol, use the perProtocolCollectOptions method.
     */
    public static class Builder{
        private Map<Short, ProtocolCollectOptions> perProtocolCollectOptions = new HashMap<>();
        private boolean collectAllMetrics = true;
        private short[] protocolsToCollect = new short[0];
        private boolean collectOSMetrics = true;

        public Builder(){}

        public Builder collectAllMetrics(boolean collectAllMetrics){
            this.collectAllMetrics = collectAllMetrics;
            return this;
        }

        public Builder collectOSMetrics(boolean collectOSMetrics){
            this.collectOSMetrics = collectOSMetrics;
            return this;
        }

        public Builder protocolsToCollect(short... protocolsToCollect){
            if(protocolsToCollect == null) return this;
            this.collectAllMetrics = false;
            this.protocolsToCollect = protocolsToCollect;
            return this;
        }

        public Builder perProtocolCollectOptions(Map<Short, ProtocolCollectOptions> perProtocolCollectOptions){
            this.perProtocolCollectOptions = perProtocolCollectOptions;
            return this;
        }

        public ExporterCollectOptions build(){
            return new ExporterCollectOptions(this);
        }
    }

    /**
     * Returns a new Builder for ExporterCollectOptions
     * @return a new Builder for ExporterCollectOptions
     */
    public static Builder builder(){
        return new Builder();
    }

    private ExporterCollectOptions(Builder builder){
        this.perProtocolCollectOptions = builder.perProtocolCollectOptions;
        this.collectMetricsAllProtocols = builder.collectAllMetrics;
        this.protocolsToCollect = builder.protocolsToCollect;
        this.collectOSMetrics = builder.collectOSMetrics;
    }

    public void addRegistryCollectOptions(short protocolId, ProtocolCollectOptions protocolCollectOptions){
        this.perProtocolCollectOptions.put(protocolId, protocolCollectOptions);
    }

    public ProtocolCollectOptions getProtocolCollectOptions(Short protocolId){
        return this.perProtocolCollectOptions.get(protocolId);
    }

    public boolean isCollectMetricsAllProtocols() {
        return collectMetricsAllProtocols;
    }


    public short[] getProtocolsToCollect() {
        return Arrays.copyOf(protocolsToCollect, protocolsToCollect.length);
    }

    public boolean isCollectOSMetrics() {
        return collectOSMetrics;
    }



    public String toString() {
        return "ExporterCollectOptions{" +
                "perRegistryCollectOptions=" + perProtocolCollectOptions +
                ", collectMetricsAllProtocols=" + collectMetricsAllProtocols +
                ", protocolsToCollect=" + Arrays.toString(protocolsToCollect) +
                ", collectOSMetrics=" + collectOSMetrics +
                '}';
    }

}
