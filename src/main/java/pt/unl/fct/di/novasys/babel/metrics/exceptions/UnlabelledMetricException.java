package pt.unl.fct.di.novasys.babel.metrics.exceptions;

import pt.unl.fct.di.novasys.babel.metrics.Metric.MetricType;

public class UnlabelledMetricException extends RuntimeException {
    public UnlabelledMetricException(MetricType metricType, String metricName) {
        super(String.format("Unlabelled %s %s used as labelled metric!", metricType.type(), metricName));
    }
}
