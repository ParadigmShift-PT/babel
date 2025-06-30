package pt.unl.fct.di.novasys.babel.metrics.formatting;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;

public interface NodeSampleFormatter {

    String getFormatterName();

    String format(NodeSample sample) throws NoSuchProtocolRegistry;
}
