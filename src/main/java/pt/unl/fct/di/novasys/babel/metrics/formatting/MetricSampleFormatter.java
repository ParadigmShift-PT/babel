package pt.unl.fct.di.novasys.babel.metrics.formatting;

import pt.unl.fct.di.novasys.babel.metrics.MetricSample;

/**
 * Formatter that serialises a single {@link MetricSample} to a string.
 */
public interface MetricSampleFormatter {

    /**
     * Returns the unique name that identifies this formatter implementation.
     *
     * @return the formatter name
     */
    String getFormatterName();

    /**
     * Formats a single {@link MetricSample} as a string.
     *
     * @param sample the metric sample to format
     * @return a formatted string representation of the sample
     */
    String format(MetricSample sample);
}
