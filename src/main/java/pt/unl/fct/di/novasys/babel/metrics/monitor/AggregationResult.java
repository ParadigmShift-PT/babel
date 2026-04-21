package pt.unl.fct.di.novasys.babel.metrics.monitor;

import pt.unl.fct.di.novasys.babel.metrics.*;
import pt.unl.fct.di.novasys.babel.metrics.exporters.ProtocolCollectOptions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class that represents the result of an aggregation process<br>
 * Holds the samples for each host, indexed by the host and the protocol ID
 */
public class AggregationResult {

    private Map <HostProtocolIdentifier, List<MetricSample>> samplesPerNode;
    private long timestamp;
    private Map<Short, String> protoIdToName;

    /**
     * Constructs an empty {@code AggregationResult} timestamped at the given epoch milliseconds.
     *
     * @param timestamp     the aggregation timestamp in UNIX epoch milliseconds
     * @param protoIdToName mapping from protocol ID to protocol name, used when creating new metrics
     */
    public AggregationResult(long timestamp, Map<Short, String> protoIdToName) {
        this.samplesPerNode = new HashMap<>();
        this.timestamp = timestamp;
        this.protoIdToName = protoIdToName;
    }



    /**
     * If a metric is created in the aggregation process, it can be added using this method, and it will be added to the result
     * @param m Metric to be added
     * @param protocolId Protocol ID of the metric
     * @param host Host to which this resulting metric belongs, if it is a global metric, use the GLOBAL constant or the addGlobalMetric method
     */
    public void addMetricToSample(Metric m, short protocolId, String host){

        String protocolName = protoIdToName.get(protocolId);
        if (protocolName == null) {
            protocolName = "";
        }

        ProtocolMetrics protocolMetrics = new ProtocolMetrics(protocolId, protocolName);
        protocolMetrics.register(m);

        ProtocolSample es = protocolMetrics.collect(new ProtocolCollectOptions());

        addSample(es.getMetricSample(m.getName()), protocolId, host);
    }

    /**
     * Same behaviour as {@link #addMetricToSample(Metric, short, String)}, but this metric is considered global (aggregate of all hosts)
     * @param m
     * @param protocolID
     */
    public void addGlobalMetricToSample(Metric m, short protocolID)  {
        this.addMetricToSample(m, protocolID, MetricsManager.GLOBAL_HOST_IDENTIFIER);
    }

    /**
     * Adds a pre-built {@link MetricSample} to the result, associated with the given host and protocol.
     *
     * @param ms         the metric sample to add
     * @param protocolID the protocol ID this sample belongs to
     * @param host       the host identifier this sample is attributed to
     */
    public void addSample(MetricSample ms, short protocolID, String host){

        HostProtocolIdentifier npi = new HostProtocolIdentifier(host, protocolID);
        if(!samplesPerNode.containsKey(npi)){
            samplesPerNode.put(npi, new LinkedList<>());
        }
        samplesPerNode.get(npi).add(ms);
    }

    /**
     * Adds a pre-built {@link MetricSample} to the result under the global host identifier,
     * indicating it represents an aggregate across all nodes.
     *
     * @param ms         the metric sample to add
     * @param protocolID the protocol ID this sample belongs to
     */
    public void addGlobalSample(MetricSample ms, short protocolID){
        this.addSample(ms, protocolID, MetricsManager.GLOBAL_HOST_IDENTIFIER);
    }

    /**
     * Returns the map of aggregated samples, keyed by a {@link HostProtocolIdentifier} combining
     * the host string and protocol ID.
     *
     * @return map from {@link HostProtocolIdentifier} to the list of aggregated {@link MetricSample} objects
     */
    public Map <HostProtocolIdentifier, List<MetricSample>> getAggregatedSamples() {
        return samplesPerNode;
    }
}
