package pt.unl.fct.di.novasys.babel.metrics.exporters;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that holds the collect options for a protocol's metrics.
 */
public class ProtocolCollectOptions {
    private Map<String, CollectOptions> perMetricCollectOptions;


    public ProtocolCollectOptions(Map<String, CollectOptions> perMetricCollectOptions) {
        this.perMetricCollectOptions = perMetricCollectOptions;
    }

    public ProtocolCollectOptions(){
        this.perMetricCollectOptions = new HashMap<>();
    }

    public void addCollectOptions(String metricName, CollectOptions collectOptions){
        this.perMetricCollectOptions.put(metricName, collectOptions);
    }

    public CollectOptions getCollectOptions(String metricName){
        return this.perMetricCollectOptions.get(metricName);
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, CollectOptions> entry : perMetricCollectOptions.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue().toString()).append("\n");
        }
        return sb.toString();
    }

}
