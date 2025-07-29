package pt.unl.fct.di.novasys.babel.metrics.monitor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.metrics.ProtocolSample;
import pt.unl.fct.di.novasys.babel.metrics.MetricSample;
import pt.unl.fct.di.novasys.babel.metrics.NodeSample;

import java.util.*;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the aggregation of samples collected by a {@link Monitor}.<br>
 * After an aggregation is performed, the samples are cleared.<br>
 * Metric samples which are not aggregated but added to the manager will be returned in the result of the aggregation.<br>
 */
public class AggregationManager {
    private static final Logger logger = LogManager.getLogger(AggregationManager.class);

    private final Map<MetricIdentifier, Map<String, MetricSample>> samples;

    private final List<Aggregation> aggregationsToPerform;

    private final Set<MetricIdentifier> metricsNotAggregated;

    private final Set<MetricIdentifier> metricsBelongingToAggregation;

    private final Map <Short, String> protocolIdsToNames;

    public AggregationManager() {
        this.samples = new ConcurrentHashMap<>();
        this.aggregationsToPerform = new LinkedList<>();
        this.metricsNotAggregated = new HashSet<>();
        this.metricsBelongingToAggregation = new HashSet<>();
        this.protocolIdsToNames = new ConcurrentHashMap<>();
    }

    /**
     * Returns the map of samples to be aggregated.<br>
     * This method is intended for debugging potential issues with the aggregation process. Any modifications
     * to the returned Map will be reflected in the aggregation process.<br>
     * @return Map of MetricIdentifier to a map of host to MetricSample.<br>
     */
    public Map<MetricIdentifier, Map<String, MetricSample>> getSamples() {
        return samples;
    }

    /**
     * Returns the list of aggregations to be performed.<br>
     * @return List of Aggregations to be performed
     */
    public List<Aggregation> getAggregationsToPerform() {
        return aggregationsToPerform;
    }

    /**
     * Returns the set of metrics that are not associated with any aggregation.<br>
     * These metrics will be returned in the result of the aggregation.<br>
     * @return Set of MetricIdentifier that were not aggregated
     */
    public Set<MetricIdentifier> getMetricsNotAggregated() {
        return metricsNotAggregated;
    }


    /**
     * Adds a sample to be considered in the next aggregation round.<br>
     * After an aggregation is performed, the samples are cleared.<br>
     * @param host Host where the sample was collected
     * @param sample NodeSample containing the samples per protocol.<br>
     */
    public synchronized void addSample(String host, NodeSample sample) {
        for(Entry<Short, ProtocolSample> entry : sample.getSamplesPerProtocol().entrySet()){
            this.protocolIdsToNames.put(entry.getKey(), entry.getValue().getProtocolName());
            for(MetricSample metricEntry : entry.getValue().getMetricSamples()){
                MetricIdentifier identifier = new MetricIdentifier(metricEntry.getMetricName(), entry.getKey());
                if(!metricsBelongingToAggregation.contains(identifier)) {
                    metricsNotAggregated.add(identifier);
                }
                if(!samples.containsKey(identifier)){
                    samples.put(identifier, new HashMap<>());
                }
                samples.get(identifier).put(host, metricEntry);
            }
        }
    }

    /**
     * Adds an aggregation to be performed.
     * This aggregation will be performed when {@link #performAggregations()} is called.<br>
     * @param aggregation Aggregation to be performed.<br>
     */
    public synchronized void addAggregation(Aggregation aggregation) {
        this.aggregationsToPerform.add(aggregation);
        for(MetricIdentifier mi : aggregation.getMetricIdentifiers()){
            metricsBelongingToAggregation.add(mi);
            metricsNotAggregated.remove(mi);
        }
    }

    /**
     * Performs all added aggregations, returning a map of NodeSample.<br>
     * The result will be time-stamped with the start time of the aggregation in UNIX epoch milliseconds.<br>
     * To specify a timestamp use the method {@link #performAggregations(long)}.<br>
     * The samples are cleared after the aggregation is performed.<br>
     * @return Map of NodeSample, containing the aggregated samples per host.
     */
    public Map<String, NodeSample> performAggregations() {
        return performAggregations(System.currentTimeMillis());
    }

    /**
     * Performs all added aggregations, returning a map of NodeSample.<br>
     * The samples are cleared after the aggregation is performed.<br>
     * @param timestamp Timestamp to be used for the aggregation result, in UNIX epoch milliseconds.<br>
     * @return Map of NodeSample, containing the aggregated samples per host.
     */
    public synchronized Map<String, NodeSample> performAggregations(long timestamp) {
        List<Thread> threads = new LinkedList<>();
        Object lock = new Object();
        Map<String, NodeSample> resultSamples = new HashMap<>();

        if(timestamp <= 0){
            throw new IllegalArgumentException("Timestamp must be a positive value");
        }

        if(this.samples.isEmpty()){
            logger.warn("No samples to aggregate, returning empty result");
            return resultSamples;
        }

        List<AggregationResult> results = new LinkedList<>();

        //Perform all aggregations, each in a separate thread
        for (Aggregation aggregation : this.aggregationsToPerform){
            Thread t = new Thread(() -> {
                AggregationInput ai = new AggregationInput();
                AggregationResult ar = new AggregationResult(timestamp, protocolIdsToNames);

                for(MetricIdentifier mi : aggregation.getMetricIdentifiers()){
                    Map<String, MetricSample> metricSamples = this.samples.getOrDefault(mi, new HashMap<>());
                    if(metricSamples.isEmpty()){
                        logger.warn("Aggregation {} - metric {} was not present in the samples", aggregation.getClass().getName(), mi);
                    }

                    if(metricSamples.size() == 1){
                        logger.warn("Aggregation {} - metric {} was only present for one host", aggregation.getClass().getName(), mi);
                    }

                    for(Entry<String, MetricSample> entry : metricSamples.entrySet()){
                        ai.addMetricSample(entry.getKey(), mi, entry.getValue());
                    }
                }


                AggregationResult result = aggregation.aggregate(ai, ar);
                synchronized(lock){
                    if(result != null) {
                        results.add(result);
                    }
                }
            });

            threads.add(t);
            t.start();
        }

        //Wait for all threads to finish
        for(Thread t : threads){
            try {
                t.join();
            } catch (InterruptedException ignored) {
            }
        }

        //No aggregation is performed on the remainder metrics
        AggregationResult ar = new AggregationResult(timestamp, protocolIdsToNames);
        for(MetricIdentifier mi : metricsNotAggregated){
            for(Entry<String, MetricSample> entry : this.samples.get(mi).entrySet()){
                ar.addSample(entry.getValue(), mi.getProtocolId(), entry.getKey());
            }
        }
        results.add(ar);

        for(AggregationResult result : results){
            for(Entry<HostProtocolIdentifier, List<MetricSample>> entry : result.getAggregatedSamples().entrySet()){
                if(!resultSamples.containsKey(entry.getKey().getHost())){
                    resultSamples.put(entry.getKey().getHost(), new NodeSample());
                }
                short protoID = entry.getKey().getProtocolId();
                if(!resultSamples.get(entry.getKey().getHost()).getProtocols().contains(protoID)) {
                    ProtocolSample es = new ProtocolSample(protoID, this.protocolIdsToNames.get(protoID), entry.getValue());
                    resultSamples.get(entry.getKey().getHost()).addProtocolSample(protoID, es);
                } else {
                    // If the protocol sample already exists, add the samples to it
                    for (MetricSample metricSample : entry.getValue()) {
                        resultSamples.get(entry.getKey().getHost()).getProtocolSample(protoID).addMetricSample(metricSample);
                    }
                }



            }
        }


        //Delete all samples from current round of aggregation
        this.samples.clear();

        return resultSamples;
    }








}