package pt.unl.fct.di.novasys.babel.metrics.formatting;

import pt.unl.fct.di.novasys.babel.metrics.MultiRegistryEpochSample;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;

public interface Formatter {
    enum Format {
        PROMETHEUS
    }

    String format(MultiRegistryEpochSample sample) throws NoSuchProtocolRegistry;
}
