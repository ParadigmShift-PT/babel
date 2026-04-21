package pt.unl.fct.di.novasys.babel.metrics.formatting;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;

import java.util.Map;


/**
 * Formatter that produces a string representation of one or more {@link NodeSample} instances,
 * each identified by a node name.
 */
public interface IdentifiedNodeSampleFormatter {

    /**
     * Formats a single node's sample as a string, prefixed with the node identifier.
     *
     * @param node   the string identifier of the node whose sample is being formatted
     * @param sample the metrics snapshot collected from that node
     * @return a formatted string representation of the node's sample
     */
   String format(String node, NodeSample sample);

    /**
     * Formats a map of per-node samples into a single string representation.
     *
     * @param samples a map from node identifier to its collected {@link NodeSample}
     * @return a formatted string representation of all samples
     */
   String format(Map<String, NodeSample> samples);
}
