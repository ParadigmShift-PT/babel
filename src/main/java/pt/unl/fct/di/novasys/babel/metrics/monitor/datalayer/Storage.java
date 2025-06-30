package pt.unl.fct.di.novasys.babel.metrics.monitor.datalayer;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;

import java.util.Map;

public interface Storage {

    void store(String host, NodeSample nodeSample);

    void store(Map<String, NodeSample> samples);

}
