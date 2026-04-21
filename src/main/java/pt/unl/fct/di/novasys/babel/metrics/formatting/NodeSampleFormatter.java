package pt.unl.fct.di.novasys.babel.metrics.formatting;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;

/**
 * Formatter that serialises a {@link NodeSample} to a string.
 */
public interface NodeSampleFormatter {

    /**
     * Returns the unique name that identifies this formatter implementation.
     *
     * @return the formatter name
     */
    String getFormatterName();

    /**
     * Formats a {@link NodeSample} as a string.
     *
     * @param sample the node sample to format
     * @return a formatted string representation of the sample
     * @throws NoSuchProtocolRegistry if a referenced protocol registry cannot be found
     */
    String format(NodeSample sample) throws NoSuchProtocolRegistry;
}
