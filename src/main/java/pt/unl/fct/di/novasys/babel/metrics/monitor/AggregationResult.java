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

    public void addSample(MetricSample ms, short protocolID, String host){

        HostProtocolIdentifier npi = new HostProtocolIdentifier(host, protocolID);
        if(!samplesPerNode.containsKey(npi)){
            samplesPerNode.put(npi, new LinkedList<>());
        }
        samplesPerNode.get(npi).add(ms);
    }

    public void addGlobalSample(MetricSample ms, short protocolID){
        this.addSample(ms, protocolID, MetricsManager.GLOBAL_HOST_IDENTIFIER);
    }

    public Map <HostProtocolIdentifier, List<MetricSample>> getAggregatedSamples() {
        return samplesPerNode;
    }
}
