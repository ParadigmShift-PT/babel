package pt.unl.fct.di.novasys.babel.metrics.exporters;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.metrics.MetricsManager;
import pt.unl.fct.di.novasys.babel.metrics.NodeSample;

/**
 * Base class for Babel protocol exporters. Wraps a {@link ProtocolExporterHelper} and exposes
 * metric-collection helpers that return an empty snapshot when the exporter has been disabled.
 * Subclasses implement the actual export logic as a Babel {@code GenericProtocol}.
 */
public abstract class ProtocolExporter extends GenericProtocol {
    ProtocolExporterHelper protocolExporterHelper;

    static final NodeSample EMPTY_NODE_SAMPLE = new NodeSample(0);

    /**
     * Creates a protocol exporter with the specified name, ID, and collect options, registering
     * the underlying {@link ProtocolExporterHelper} with the {@link pt.unl.fct.di.novasys.babel.metrics.MetricsManager}.
     *
     * @param protoName the human-readable name of this protocol
     * @param protoId   the numeric Babel protocol ID
     * @param options   collect options controlling which metrics are gathered
     */
    public ProtocolExporter(String protoName, short protoId, ExporterCollectOptions options) {
        super(protoName, protoId);
        this.protocolExporterHelper = new ProtocolExporterHelper.Builder(protoName)
                .exporterCollectOptions(options)
                .build();
        MetricsManager.getInstance().registerExporters(this.protocolExporterHelper);
    }

    /**
     * Creates a protocol exporter with default collect options (all protocols, OS metrics included).
     *
     * @param protoName the human-readable name of this protocol
     * @param protoId   the numeric Babel protocol ID
     */
    public ProtocolExporter(String protoName, short protoId){
        this(protoName, protoId, new ExporterCollectOptions.Builder().build());
    }

    /**
     * Refer to {@link Exporter#collectMetrics()}} for documentation
     */
    public NodeSample collectMetrics() {
        if (protocolExporterHelper.isDisabled()) {
            return EMPTY_NODE_SAMPLE;
        }
        return protocolExporterHelper.collectMetrics();
    }

    /**
     * Refer to {@link Exporter#collectAllMetrics()}} for documentation
     */
    public NodeSample collectAllMetrics() {
        if (protocolExporterHelper.isDisabled()) {
            return EMPTY_NODE_SAMPLE;
        }
        return protocolExporterHelper.collectAllMetrics();
    }

    /**
     * Refer to {@link Exporter#collectMetrics(boolean, short...)} for documentation
     * @param collectOSMetrics if true, OS metrics will be collected
     * @param protocolIDs the protocol IDs for which metrics should be collected
     * @return a NodeSample containing the collected metrics
     */
    public NodeSample collectMetrics(boolean collectOSMetrics, short... protocolIDs) {
        if (protocolExporterHelper.isDisabled()) {
            return EMPTY_NODE_SAMPLE;
        }
        return protocolExporterHelper.collectMetrics(collectOSMetrics, protocolIDs);
    }

    /**
     * Returns {@code true} if the underlying exporter helper has been disabled.
     *
     * @return {@code true} when the exporter is disabled and metric collection is suppressed
     */
    public boolean isExporterDisabled() {
        return protocolExporterHelper.isDisabled();
    }
}