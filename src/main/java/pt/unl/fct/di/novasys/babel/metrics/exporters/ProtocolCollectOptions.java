package pt.unl.fct.di.novasys.babel.metrics.exporters;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that holds the collect options for a protocol's metrics.
 */
public class ProtocolCollectOptions {
    private Map<String, CollectOptions> perMetricCollectOptions;


    /**
     * Creates collect options for a protocol with a pre-populated per-metric options map.
     *
     * @param perMetricCollectOptions map from metric name to its {@link CollectOptions}
     */
    public ProtocolCollectOptions(Map<String, CollectOptions> perMetricCollectOptions) {
        this.perMetricCollectOptions = perMetricCollectOptions;
    }

    /**
     * Creates collect options for a protocol with an empty per-metric options map.
     */
    public ProtocolCollectOptions(){
        this.perMetricCollectOptions = new HashMap<>();
    }

    /**
     * Associates the given {@link CollectOptions} with the named metric.
     *
     * @param metricName    the name of the metric
     * @param collectOptions the collect options to apply to that metric
     */
    public void addCollectOptions(String metricName, CollectOptions collectOptions){
        this.perMetricCollectOptions.put(metricName, collectOptions);
    }

    /**
     * Returns the {@link CollectOptions} for the named metric, or {@code null} if none were configured.
     *
     * @param metricName the name of the metric
     * @return the associated {@link CollectOptions}, or {@code null}
     */
    public CollectOptions getCollectOptions(String metricName){
        return this.perMetricCollectOptions.get(metricName);
    }

    /**
     * Returns a human-readable listing of each metric name and its collect options.
     *
     * @return string representation of all per-metric collect options
     */
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, CollectOptions> entry : perMetricCollectOptions.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue().toString()).append("\n");
        }
        return sb.toString();
    }

}
