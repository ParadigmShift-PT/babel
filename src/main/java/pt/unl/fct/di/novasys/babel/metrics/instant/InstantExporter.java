package pt.unl.fct.di.novasys.babel.metrics.instant;

import pt.unl.fct.di.novasys.babel.metrics.MetricSample;

/**
 * Interface to be extended by exporters that export MetricSamples as soon as they are provided
 */
public interface InstantExporter extends Runnable {
    void addMetricSample(MetricSample ms);
    String getExporterName();


}
