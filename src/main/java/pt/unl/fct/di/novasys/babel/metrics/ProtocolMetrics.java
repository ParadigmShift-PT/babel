package pt.unl.fct.di.novasys.babel.metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.DuplicatedProtocolMetric;
import pt.unl.fct.di.novasys.babel.metrics.exporters.CollectOptions;
import pt.unl.fct.di.novasys.babel.metrics.exporters.ProtocolCollectOptions;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An instance of ProtocolMetrics is a collection of metrics that belong to the same protocol.<br>
 * It is used to collect samples of the metrics registered for that protocol.<br>
 */
public class ProtocolMetrics {
    private static final Logger logger = LogManager.getLogger(ProtocolMetrics.class);

    //Each protocol will have one of these, as multiple protocols may have same metrics, and may make easier to export metrics for a single protocol
    //Will this have performance implications :))) TBD

    private final short protocolID;

    private final String protocolName;


    private final Map<String, Metric> metrics;

    public ProtocolMetrics(short protocolID, String protoName) {
        this.protocolID = protocolID;
        this.metrics = new ConcurrentHashMap<>();
        this.protocolName = protoName;
    }



    public void register(Metric metric) throws DuplicatedProtocolMetric {
        //a metric with same name already exists!!
        if(metrics.containsKey(metric.getName())){
            throw new DuplicatedProtocolMetric(protocolID, metric.getName());
        }
        metrics.put(metric.getName(), metric);
    }


    //TODO: Concurrency Who is that :))?
    public ProtocolSample collect(ProtocolCollectOptions protocolCollectOptions) {

        List<MetricSample> samples = new LinkedList<>();
        for (Map.Entry<String, Metric> entry : metrics.entrySet()) {
            CollectOptions co = protocolCollectOptions.getCollectOptions(entry.getKey());

            if(co == null ) {
                logger.debug("Metric {} has no collect options", entry.getKey());
                co = new CollectOptions();
            }
            logger.debug("Metric {} collect with reset metric: {}", entry.getKey(), co.getResetOnCollect());
            samples.add(entry.getValue().collect(co));
        }
        return new ProtocolSample(this.protocolID, this.protocolName, samples);
    }

    public void disable() {
        for (Metric metric : metrics.values()) {
            metric.disable();
        }
    }
}
