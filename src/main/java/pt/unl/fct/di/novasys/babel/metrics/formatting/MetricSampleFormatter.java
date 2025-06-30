package pt.unl.fct.di.novasys.babel.metrics.formatting;

import pt.unl.fct.di.novasys.babel.metrics.MetricSample;

public interface MetricSampleFormatter {

    String getFormatterName();

    String format(MetricSample sample);
}
