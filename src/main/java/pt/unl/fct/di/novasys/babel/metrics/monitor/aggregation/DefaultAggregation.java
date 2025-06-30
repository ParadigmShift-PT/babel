package pt.unl.fct.di.novasys.babel.metrics.monitor.aggregation;

import pt.unl.fct.di.novasys.babel.metrics.MetricSample;
import pt.unl.fct.di.novasys.babel.metrics.Sample;
import pt.unl.fct.di.novasys.babel.metrics.monitor.Aggregation;
import pt.unl.fct.di.novasys.babel.metrics.monitor.AggregationInput;
import pt.unl.fct.di.novasys.babel.metrics.monitor.AggregationResult;
import pt.unl.fct.di.novasys.babel.metrics.monitor.MetricIdentifier;

import java.util.*;


/**
 * Default aggregation which depends on the metric type
 *<ul>
 *     <li>Counter: Sum all the values
 *     <li>Gauge: Average all the values
 *     <li>Histogram: Sum all buckets
 *     <li>Record: Append all records
 *</ul>
 */
public class DefaultAggregation extends Aggregation {

    private static final String DELIMITER = ";";

    public DefaultAggregation(short protocolId, String metricName) {
        super(protocolId, metricName);
    }

    /**
     * Aggregates counter metric from all hosts
     * This is done by adding all the values of the same label values
     * @param samples Iterator of MetricSamples
     * @return Map of labelValues with the aggregated value
     */
    private Map<String, Double> aggregateCounter(Iterator<MetricSample> samples){
        Map<String, Double> resultSamples = new HashMap<>();
        MetricSample sample;
        while(samples.hasNext()){
            sample = samples.next();
            for(int i=0; i<sample.getSamples().length; i++){
                String labelValuesWithPlusSeparator = String.join(DELIMITER, sample.getSamples()[i].getLabelsValues());
                double value = 0;
                if(resultSamples.containsKey(labelValuesWithPlusSeparator)){
                    value = resultSamples.get(labelValuesWithPlusSeparator);
                }
                resultSamples.put(labelValuesWithPlusSeparator, value + sample.getSamples()[i].getValue());
            }
        }
        return resultSamples;
    }


    /**
     * Aggregates gauge metric from all hosts
     * This is done by averaging all the values of the same label values
     * @return Map of labelValues with the aggregated value
     */
    private Map<String, Double> aggregateGauge(Iterator<MetricSample> samples, int nHosts){
        Map<String, Double> resultSamples = aggregateCounter(samples);
        for(Map.Entry<String, Double> entry : resultSamples.entrySet()){
            entry.setValue(entry.getValue()/nHosts);
        }
        return resultSamples;
    }

    /**
     * Aggregates histogram metric from all hosts
     * This is done by summing all the values of the same label values
     * @return Map of labelValues with the aggregated value
     */
    private Map<String, Double> aggregateHistogram(Iterator<MetricSample> samples) {
        return aggregateCounter(samples);
    }

    /**
     * Aggregates record metric from all hosts
     * This is done by appending all the values of the same label values
     * @return Map of labelValues with the aggregated value
     */
    private Map<String, Double> aggregateRecord(Iterator<MetricSample> samples) {
        return aggregateCounter(samples);
    }


    public AggregationResult aggregate(AggregationInput aggregationInput, AggregationResult aggregationResult) {
        if (aggregationInput.getMetrics().size() != 1) {
            throw new IllegalArgumentException("DefaultAggregation can only aggregate one metric");
        }

        MetricIdentifier mid = aggregationInput.getMetrics().getFirst();

        List<MetricSample> metricSampleMap = aggregationInput.getSamples(mid.getProtocolId(), mid.getMetricName());

        if(metricSampleMap.isEmpty()) {
            return aggregationResult;
        }


        int nHosts = metricSampleMap.size();
        MetricSample sample = metricSampleMap.getFirst();
        String[] labelNames = sample.getLabelNames();
        Iterator<MetricSample> it = metricSampleMap.iterator();


        Map<String, Double> resultingSamples;
        switch (sample.getMetricType()){
                case COUNTER:
                    resultingSamples = aggregateCounter(it);
                    break;
                case GAUGE:
                    resultingSamples = aggregateGauge(it, nHosts);
                    break;
                case HISTOGRAM:
                    resultingSamples = aggregateHistogram(it);
                    //Histogram Aggregation
                    break;
                case RECORD:
                    resultingSamples = aggregateRecord(it);
                    //Record Aggregation
                    break;
                default:
                    throw new IllegalArgumentException("Unknown metric type");
        }

        Sample[] samplesArray = new Sample[resultingSamples.size()];
        int i = 0;
        for(Map.Entry<String, Double> entry : resultingSamples.entrySet()){
            String[] labels = entry.getKey().split(DELIMITER);
            samplesArray[i] = new Sample(entry.getValue(), labelNames, labels);
            i++;
        }

        MetricSample resultingMetricSample;
        if(sample.hasLabels()){
            resultingMetricSample = MetricSample.builder(sample.getMetricUnit() , sample.getMetricName(), sample.getMetricType())
                    .labelNames(labelNames)
                    .build(samplesArray);
        }else{
            resultingMetricSample = MetricSample.builder(sample.getMetricUnit() , sample.getMetricName(), sample.getMetricType())
                    .build(samplesArray[0]);
        }

        aggregationResult.addGlobalSample(resultingMetricSample, mid.getProtocolId());


        return aggregationResult;
    }
}
