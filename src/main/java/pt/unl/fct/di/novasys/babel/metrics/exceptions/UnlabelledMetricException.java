package pt.unl.fct.di.novasys.babel.metrics.exceptions;

import pt.unl.fct.di.novasys.babel.metrics.Metric.MetricType;

/**
 * Thrown when an unlabelled metric is accessed through an API intended for labelled metrics.
 */
public class UnlabelledMetricException extends RuntimeException {
    /**
     * Creates a new exception identifying the metric that was misused.
     *
     * @param metricType the type of the metric (e.g. counter, gauge)
     * @param metricName the name of the unlabelled metric that was accessed as labelled
     */
    public UnlabelledMetricException(MetricType metricType, String metricName) {
        super(String.format("Unlabelled %s %s used as labelled metric!", metricType.type(), metricName));
    }
}
