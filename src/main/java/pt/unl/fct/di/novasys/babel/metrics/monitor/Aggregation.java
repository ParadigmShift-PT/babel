package pt.unl.fct.di.novasys.babel.metrics.monitor;

import java.util.List;

/**
 * Base class for metric aggregations performed by an {@link AggregationManager}.
 * Subclasses implement the {@link #aggregate(AggregationInput, AggregationResult)} method to
 * define how samples from multiple hosts are combined into a single result.
 */
public abstract class Aggregation {

    List<MetricIdentifier> metricIdentifiers;

    /**
     * Constructs an aggregation that operates on a single metric identified by protocol and name.
     *
     * @param protocolId the protocol ID of the metric to aggregate
     * @param metricName the name of the metric to aggregate
     */
    public Aggregation(short protocolId, String metricName) {
        this.metricIdentifiers = List.of(new MetricIdentifier(metricName, protocolId));
    }

    /**
     * Constructs an aggregation that operates on multiple metrics identified by the given identifiers.
     *
     * @param metrics one or more {@link MetricIdentifier} instances describing the metrics to aggregate
     */
    public Aggregation(MetricIdentifier... metrics) {
        this.metricIdentifiers = List.of(metrics);
    }

    /**
     * Returns the list of metric identifiers that this aggregation operates on.
     *
     * @return an immutable list of {@link MetricIdentifier} instances
     */
    public List<MetricIdentifier> getMetricIdentifiers() {
        return metricIdentifiers;
    }

    /**
     * Performs the aggregation, updating the aggregationResult with the new data
     * @param aggregationInput The input data to aggregate
     * @param aggregationResult The result of the aggregation
     * @return The updated aggregationResult
     */
    public abstract AggregationResult aggregate(AggregationInput aggregationInput, AggregationResult aggregationResult);
}
