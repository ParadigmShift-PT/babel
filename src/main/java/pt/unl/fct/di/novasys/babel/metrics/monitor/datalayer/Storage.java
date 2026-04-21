package pt.unl.fct.di.novasys.babel.metrics.monitor.datalayer;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;

import java.util.Map;

/**
 * Persistence abstraction for storing aggregated metric samples produced by a {@link pt.unl.fct.di.novasys.babel.metrics.monitor.Monitor}.
 */
public interface Storage {

    /**
     * Stores the metrics snapshot for a single node.
     *
     * @param host       the string identifier of the node
     * @param nodeSample the metrics snapshot to store
     */
    void store(String host, NodeSample nodeSample);

    /**
     * Stores a map of per-node metrics snapshots produced by a single aggregation round.
     *
     * @param samples a map from node identifier to its collected {@link NodeSample}
     */
    void store(Map<String, NodeSample> samples);

}
