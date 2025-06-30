package pt.unl.fct.di.novasys.babel.metrics.monitor;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.metrics.NodeSample;

import java.util.Map;

/**
 * Base class for all monitor protocols in Babel metrics.<br>
 * A monitor is a protocol which receives metrics from other nodes and aggregates them.<br>
 * This base class provides methods to manage aggregations and samples.<br>
 */
public abstract class Monitor extends GenericProtocol {
    private AggregationManager aggregationManager;

    public Monitor(String protoName, short protoId) {
        super(protoName, protoId);
    }

    protected AggregationManager getAggregationManager() {
        if (aggregationManager == null) {
            aggregationManager = new AggregationManager();
        }
        return aggregationManager;
    }

    /**
     * Refer to {@link AggregationManager#addSample(String, NodeSample)} for documentation
     */
    protected void addSampleToAggregate(String host, NodeSample sample) {
        getAggregationManager().addSample(host, sample);
    }

    /**
     * Refer to {@link AggregationManager#performAggregations()} for documentation
     */
    protected Map<String, NodeSample> performAggregations() {
        return getAggregationManager().performAggregations();
    }

    /**
     * Refer to {@link AggregationManager#performAggregations(long)} for documentation
     */
    protected Map<String, NodeSample> performAggregations(long timestamp) {
        return getAggregationManager().performAggregations(timestamp);
    }

    /**
     * Refer to {@link AggregationManager#addAggregation(Aggregation)} for documentation
     */
    public void addAggregation(Aggregation aggregation) {
        getAggregationManager().addAggregation(aggregation);
    }
}
