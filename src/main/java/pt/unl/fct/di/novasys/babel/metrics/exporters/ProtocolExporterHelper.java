package pt.unl.fct.di.novasys.babel.metrics.exporters;

import java.util.Properties;

/**
 * Exporter to be instantiated by protocols that want to export metrics.<br>
 * If the protocol is to be instantiated by the MetricsManager using the json configs, the protocol will receive in the init method the properties that are in the properties file given in the json config.
 * It must also have a constructor that receives a ProtocolExporter Object.<br>
 * Any protocol exporter should periodically check if the metrics were disabled using the isDisabled method.
 */
public class ProtocolExporterHelper extends Exporter {

    public static class Builder extends ExporterBuilder<Builder> {
        public Builder(String exporterName) {
            super(exporterName);
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public ProtocolExporterHelper build() {
            return new ProtocolExporterHelper(this);
        }
    }

    private ProtocolExporterHelper(Builder exporterBuilder) {
        super(exporterBuilder);
    }

    @Override
    public Properties loadDefaults() {
        return new Properties();
    }

}
