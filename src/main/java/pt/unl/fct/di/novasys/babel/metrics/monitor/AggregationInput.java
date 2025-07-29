package pt.unl.fct.di.novasys.babel.metrics.monitor;

import pt.unl.fct.di.novasys.babel.metrics.MetricSample;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class that holds the input for an aggregation<br>
 * Holds a map containing the samples for each host, where the samples are indexed by the metric identifier<br>
 * MetricSamples are references to the actual samples, so they should not be modified, to modify them call clone() first
 * @see MetricIdentifier
 */
public class AggregationInput {
    private final Map<MetricIdentifier,Map<String, MetricSample>> samplesPerHost;
    private final Map<MetricIdentifier, List<MetricSample>> samplesList;

    public AggregationInput() {
        this.samplesPerHost = new HashMap<>();
        this.samplesList = new HashMap<>();
    }

    /**
     * Get all samples for a given metric, indexed by the host
     * @param protocol Protocol of the metric
     * @param metricName Name of the metric
     * @return Map containing the samples for the given metric identifier, indexed by the host, or an empty map if no samples are found
     */
    public Map<String, MetricSample> getSamplesIndexedPerHost(short protocol, String metricName){
        return samplesPerHost.getOrDefault(new MetricIdentifier(metricName, protocol), new HashMap<>());
    }

    /**
     * Returns a list containing all samples for a given metric, without any host information
     * @param protocol Protocol of the metric
     * @param metricName Name of the metric
     * @return List containing all samples for the given metric identifier, or an empty list if no samples are found
     */
    public List<MetricSample> getSamples(short protocol, String metricName){
        return samplesList.getOrDefault(new MetricIdentifier(metricName, protocol), new LinkedList<>());
    }

    /**
     * Get the number of hosts that have samples for a given metric<br>
     * This is useful to know how many hosts contributed samples for of the given metric for the current aggregation run.<br>
     * @param protocol Protocol of the metric
     * @param metricName  Name of the metric
     * @return Number of hosts that have samples for the given metric
     */
    public int getNumberOfHostsForMetric(short protocol, String metricName) {
        Map<String, MetricSample> samples = samplesPerHost.get(new MetricIdentifier(metricName, protocol));
        if (samples == null) {
            return 0;
        }
        return samples.size();
    }

    /**
     * Get all samples, indexed by the metric identifier, separated by host
     * @return Map containing the samples, indexed by the metric identifier
     */
    public Map<MetricIdentifier, Map<String, MetricSample>> getSamplesPerHost() {
        return samplesPerHost;
    }

    /**
     * Get all samples, indexed by the metric identifier, without any host information
     * @return Map containing the samples, indexed by the metric identifier
     */
    public Map<MetricIdentifier, List<MetricSample>> getSamplesList() {
        return samplesList;
    }

    /**
     * Get list of all metric identifiers present in this input
     * @return List of all metric identifiers present in this input
     */
    public List<MetricIdentifier> getMetrics() {
        return List.copyOf(samplesPerHost.keySet());
    }

    protected void addMetricSample(String host, MetricIdentifier metric, MetricSample sample){
        if(!samplesPerHost.containsKey(metric)){
            samplesPerHost.put(metric, new HashMap<>());
        }
        samplesPerHost.get(metric).put(host, sample);

        if(!samplesList.containsKey(metric)){
            samplesList.put(metric, new LinkedList<>());
        }
        samplesList.get(metric).add(sample);

    }
}