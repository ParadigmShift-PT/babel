package pt.unl.fct.di.novasys.babel.metrics.monitor;

import java.util.List;

public abstract class Aggregation {

    List<MetricIdentifier> metricIdentifiers;


    public Aggregation(short protocolId, String metricName) {
        this.metricIdentifiers = List.of(new MetricIdentifier(metricName, protocolId));
    }

    public Aggregation(MetricIdentifier... metrics) {
        this.metricIdentifiers = List.of(metrics);
    }

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
