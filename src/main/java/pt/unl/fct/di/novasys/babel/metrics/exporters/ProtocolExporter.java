package pt.unl.fct.di.novasys.babel.metrics.exporters;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.metrics.MetricsManager;
import pt.unl.fct.di.novasys.babel.metrics.NodeSample;

public abstract class ProtocolExporter extends GenericProtocol {
    ProtocolExporterHelper protocolExporterHelper;

    static final NodeSample EMPTY_NODE_SAMPLE = new NodeSample(0);

    public ProtocolExporter(String protoName, short protoId, ExporterCollectOptions options) {
        super(protoName, protoId);
        this.protocolExporterHelper = new ProtocolExporterHelper.Builder(protoName)
                .exporterCollectOptions(options)
                .build();
        MetricsManager.getInstance().registerExporters(this.protocolExporterHelper);
    }

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

    public boolean isExporterDisabled() {
        return protocolExporterHelper.isDisabled();
    }
}