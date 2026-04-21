package pt.unl.fct.di.novasys.babel.metrics.exporters;

/**
 * Marker interface for Babel protocols that embed a {@link ProtocolExporterHelper} to export metrics.
 */
public interface ExporterProtocol {
    /**
     * Returns the {@link ProtocolExporterHelper} used by this protocol to collect and export metrics.
     *
     * @return the exporter helper associated with this protocol
     */
    ProtocolExporterHelper getExporter();
}
