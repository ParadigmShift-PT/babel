package pt.unl.fct.di.novasys.babel.metrics.exporters;

import java.util.Properties;

/**
 * Exporter to be instantiated by protocols that want to export metrics.<br>
 * If the protocol is to be instantiated by the MetricsManager using the json configs, the protocol will receive in the init method the properties that are in the properties file given in the json config.
 * It must also have a constructor that receives a ProtocolExporter Object.<br>
 * Any protocol exporter should periodically check if the metrics were disabled using the isDisabled method.
 */
public class ProtocolExporterHelper extends Exporter {

    /**
     * Builder for {@link ProtocolExporterHelper}.
     */
    public static class Builder extends ExporterBuilder<Builder> {
        /**
         * Creates a builder for a {@link ProtocolExporterHelper} with the given name.
         *
         * @param exporterName logical name used to identify this exporter helper
         */
        public Builder(String exporterName) {
            super(exporterName);
        }

        /**
         * Returns this builder instance (required by the covariant builder pattern).
         *
         * @return this builder
         */
        @Override
        public Builder self() {
            return this;
        }

        /**
         * Builds and returns a configured {@link ProtocolExporterHelper}.
         *
         * @return a new {@code ProtocolExporterHelper}
         */
        @Override
        public ProtocolExporterHelper build() {
            return new ProtocolExporterHelper(this);
        }
    }

    private ProtocolExporterHelper(Builder exporterBuilder) {
        super(exporterBuilder);
    }

    /**
     * Returns an empty {@link Properties} object as this helper requires no default configuration.
     *
     * @return an empty properties set
     */
    @Override
    public Properties loadDefaults() {
        return new Properties();
    }

}
