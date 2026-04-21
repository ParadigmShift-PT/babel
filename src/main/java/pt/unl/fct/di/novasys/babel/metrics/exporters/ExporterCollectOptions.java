package pt.unl.fct.di.novasys.babel.metrics.exporters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Specifies which protocols and OS metrics an exporter should collect, along with per-metric
 * {@link CollectOptions} (e.g. reset-on-collect).
 * Use {@link #builder()} to construct instances.
 */
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

        /**
         * Creates a builder with default settings: collect all protocols and OS metrics.
         */
        public Builder(){}

        /**
         * Controls whether metrics from all registered protocols are collected.
         * Setting this to {@code false} requires specifying protocols via {@link #protocolsToCollect}.
         *
         * @param collectAllMetrics {@code true} to collect every registered protocol
         * @return this builder
         */
        public Builder collectAllMetrics(boolean collectAllMetrics){
            this.collectAllMetrics = collectAllMetrics;
            return this;
        }

        /**
         * Controls whether OS-level metrics (CPU, memory, etc.) are included in each collection.
         *
         * @param collectOSMetrics {@code true} to include OS metrics
         * @return this builder
         */
        public Builder collectOSMetrics(boolean collectOSMetrics){
            this.collectOSMetrics = collectOSMetrics;
            return this;
        }

        /**
         * Restricts collection to the specified protocol IDs and implicitly sets
         * {@code collectAllMetrics} to {@code false}.
         *
         * @param protocolsToCollect protocol IDs whose metrics should be gathered
         * @return this builder
         */
        public Builder protocolsToCollect(short... protocolsToCollect){
            if(protocolsToCollect == null) return this;
            this.collectAllMetrics = false;
            this.protocolsToCollect = protocolsToCollect;
            return this;
        }

        /**
         * Sets per-protocol {@link ProtocolCollectOptions}, replacing any previously accumulated options.
         *
         * @param perProtocolCollectOptions map from protocol ID to its collect options
         * @return this builder
         */
        public Builder perProtocolCollectOptions(Map<Short, ProtocolCollectOptions> perProtocolCollectOptions){
            this.perProtocolCollectOptions = perProtocolCollectOptions;
            return this;
        }

        /**
         * Adds collect options for a specific metric of a certain protocol.<br>
         * @param protocolId the protocol ID to which the metric belongs
         * @param metricName the name of the metric for which collect options are specified
         * @param collectOptions the collect options for the specified metric<br>
         * @return the Builder instance for method chaining<br>
         */
        public Builder metricCollectOptions(short protocolId, String metricName, CollectOptions collectOptions){
            ProtocolCollectOptions options;
            if(!perProtocolCollectOptions.containsKey(protocolId)){
                options = new ProtocolCollectOptions();
            }
            else{
                options = perProtocolCollectOptions.get(protocolId);
            }
            options.addCollectOptions(metricName, collectOptions);
            perProtocolCollectOptions.put(protocolId, options);
            return this;
        }

        /**
         * Builds and returns the configured {@link ExporterCollectOptions} instance.
         *
         * @return a new {@code ExporterCollectOptions}
         */
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

    /**
     * Associates or replaces the {@link ProtocolCollectOptions} for the given protocol.
     *
     * @param protocolId            the numeric ID of the protocol
     * @param protocolCollectOptions the per-metric collect options for that protocol
     */
    public void addRegistryCollectOptions(short protocolId, ProtocolCollectOptions protocolCollectOptions){
        this.perProtocolCollectOptions.put(protocolId, protocolCollectOptions);
    }

    /**
     * Returns the per-metric collect options for the specified protocol, or {@code null} if none were set.
     *
     * @param protocolId the numeric ID of the protocol
     * @return the {@link ProtocolCollectOptions} for that protocol, or {@code null}
     */
    public ProtocolCollectOptions getProtocolCollectOptions(Short protocolId){
        return this.perProtocolCollectOptions.get(protocolId);
    }

    /**
     * Returns {@code true} if metrics from all registered protocols should be collected.
     *
     * @return {@code true} when collecting all protocols
     */
    public boolean isCollectMetricsAllProtocols() {
        return collectMetricsAllProtocols;
    }

    /**
     * Returns a copy of the array of protocol IDs that should be collected when not collecting all.
     *
     * @return array of protocol IDs to collect
     */
    public short[] getProtocolsToCollect() {
        return Arrays.copyOf(protocolsToCollect, protocolsToCollect.length);
    }

    /**
     * Returns {@code true} if OS-level metrics should be included in each collection.
     *
     * @return {@code true} when OS metrics collection is enabled
     */
    public boolean isCollectOSMetrics() {
        return collectOSMetrics;
    }

    /**
     * Returns a human-readable representation of these collect options.
     *
     * @return string describing all fields of this instance
     */
    public String toString() {
        return "ExporterCollectOptions{" +
                "perRegistryCollectOptions=" + perProtocolCollectOptions +
                ", collectMetricsAllProtocols=" + collectMetricsAllProtocols +
                ", protocolsToCollect=" + Arrays.toString(protocolsToCollect) +
                ", collectOSMetrics=" + collectOSMetrics +
                '}';
    }

}
