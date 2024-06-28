package pt.unl.fct.di.novasys.babel.metrics;

import pt.unl.fct.di.novasys.babel.metrics.exceptions.DuplicatedProtocolMetric;
import pt.unl.fct.di.novasys.babel.metrics.exporters.CollectOptions;
import pt.unl.fct.di.novasys.babel.metrics.exporters.RegistryCollectOptions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MetricRegistry {

    //Each protocol will have one of these, as multiple protocols may have same metrics, and may make easier to export metrics for a single protocol
    //Will this have performance implications :))) TBD

    //Protocol ID to which registry corresponds, what for idk yet :))
    private final short protocolID;


    //TODO: type of epoch must come from before!!!!
    private Epoch epoch = new Epoch(Epoch.EpochType.MS);

    //TODO: USING HERE STRING AS IDENTIFIER OR DOUBLE MAY MAKE A DIFFERENCE???? WHAT ABOUT CONCURRENCY???
    private Map<String, Metric> metrics = new HashMap<>();


    public MetricRegistry(short protocolID) {
        this.protocolID = protocolID;
    }

    public void setEpoch(Epoch epoch){
        this.epoch = epoch;
    }


    public void register(Metric metric) throws DuplicatedProtocolMetric {
        //a metric with same name already exists!!
        if(metrics.containsKey(metric.getName())){
            throw new DuplicatedProtocolMetric(protocolID, metric.getName());
        }
        metrics.put(metric.getName(), metric);
    }


    //TODO: Concurrency Who is that :))?
    public EpochSample collect(RegistryCollectOptions registryCollectOptions, boolean tickEpoch){
        if(tickEpoch){
            this.epoch.tick();
        }

        //TODO: Linked list should be fine as we just add and iterate later
        List<MetricSample> samples = new LinkedList<>();
        //TODO: For each metric at this registry collect a sample, this will carry timestamp, but should there be other timestamp considerations?????
        for (Map.Entry<String, Metric> entry : metrics.entrySet()) {
            CollectOptions co = registryCollectOptions.getCollectOptions(entry.getKey());
            if(co == null ){
                co = new CollectOptions();
            }else{
                //If it cannot tick epoch that means it also cannot reset metrics on collect
                if(!tickEpoch){
                    co = new CollectOptions(co, false);
                }
            }




            samples.add(entry.getValue().collect(co));
        }
        return new EpochSample(this.epoch.getEpoch(), this.protocolID, samples);
    }
}
