package pt.unl.fct.di.novasys.babel.metrics.instant;

import pt.unl.fct.di.novasys.babel.metrics.MetricSample;

/**
 * Interface to be extended by exporters that export MetricSamples as soon as they are provided
 */
public interface InstantExporter extends Runnable {

    /**
     * Enqueues a {@link MetricSample} for immediate export.
     *
     * @param ms the metric sample to export
     */
    void addMetricSample(MetricSample ms);

    /**
     * Returns the name that identifies this exporter instance.
     *
     * @return the exporter name
     */
    String getExporterName();


}
