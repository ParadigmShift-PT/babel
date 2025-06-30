package pt.unl.fct.di.novasys.babel.metrics.formatting;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;

import java.util.Map;


public interface IdentifiedNodeSampleFormatter {
   String format(String node, NodeSample sample);

   String format(Map<String, NodeSample> samples);
}
