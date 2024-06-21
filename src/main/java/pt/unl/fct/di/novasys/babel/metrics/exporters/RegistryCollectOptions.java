package pt.unl.fct.di.novasys.babel.metrics.exporters;

import java.util.HashMap;
import java.util.Map;

public class RegistryCollectOptions {
    private Map<String, CollectOptions> perMetricCollectOptions;


    public RegistryCollectOptions(Map<String, CollectOptions> perMetricCollectOptions) {
        this.perMetricCollectOptions = perMetricCollectOptions;
    }

    public RegistryCollectOptions(){
        this.perMetricCollectOptions = new HashMap<>();
    }

    public void addCollectOptions(String metricName, CollectOptions collectOptions){
        this.perMetricCollectOptions.put(metricName, collectOptions);
    }

    public CollectOptions getCollectOptions(String metricName){
        return this.perMetricCollectOptions.get(metricName);
    }

}
