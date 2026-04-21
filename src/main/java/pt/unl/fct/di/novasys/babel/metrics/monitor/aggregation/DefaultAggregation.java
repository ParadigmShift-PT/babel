package pt.unl.fct.di.novasys.babel.metrics.monitor.aggregation;

import pt.unl.fct.di.novasys.babel.metrics.MetricSample;
import pt.unl.fct.di.novasys.babel.metrics.Sample;
import pt.unl.fct.di.novasys.babel.metrics.StatsGauge;
import pt.unl.fct.di.novasys.babel.metrics.monitor.Aggregation;
import pt.unl.fct.di.novasys.babel.metrics.monitor.AggregationInput;
import pt.unl.fct.di.novasys.babel.metrics.monitor.AggregationResult;
import pt.unl.fct.di.novasys.babel.metrics.monitor.MetricIdentifier;

import java.util.*;

import static pt.unl.fct.di.novasys.babel.metrics.Metric.MetricType.*;


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

    String globalID ="";

    /**
     * Constructs a {@code DefaultAggregation} for a single metric; the result is placed under
     * the global host identifier.
     *
     * @param protocolId the protocol ID of the metric to aggregate
     * @param metricName the name of the metric to aggregate
     */
    public DefaultAggregation(short protocolId, String metricName) {
        super(protocolId, metricName);
    }

    /**
     * Constructs a {@code DefaultAggregation} for a single metric; the result is attributed to
     * the given {@code globalID} host string instead of the default global identifier.
     *
     * @param protocolId the protocol ID of the metric to aggregate
     * @param metricName the name of the metric to aggregate
     * @param globalID   the host identifier to use for the aggregated result
     */
    public DefaultAggregation(short protocolId, String metricName, String globalID){
        super(protocolId, metricName);
        this.globalID = globalID;
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

    private Map<String, Double> aggregateStatsGauge(Iterator<MetricSample> samples) {
        Map<String, Double> resultSamples = new HashMap<>();
        MetricSample sample;

        double accCount = 0;
        double accCountTimesAvg = 0;
        double finalMin = Double.MAX_VALUE;
        double finalmax = Double.MIN_VALUE;

        Set<StatsGauge.StatType> aggregatedStats = new HashSet<>();

        while (samples.hasNext()) {
            sample = samples.next();
            double thisCount = 0;
            double thisAvg = 0;
            for (int i = 0; i < sample.getSamples().length; i++) {

                String stat = sample.getSamples()[i].getLabelsValues()[0];
                double value = sample.getSamples()[i].getValue();

                switch (stat) {
                    case "count":
                        accCount += value;
                        thisCount = value;
                        aggregatedStats.add(StatsGauge.StatType.COUNT);
                        break;
                    case "avg":
                        thisAvg = value;
                        aggregatedStats.add(StatsGauge.StatType.AVG);
                        break;
                    case "min":
                        finalMin = Math.min(value, finalMin);
                        aggregatedStats.add(StatsGauge.StatType.MIN);
                        break;
                    case "max":
                        finalmax = Math.max(value, finalmax);
                        aggregatedStats.add(StatsGauge.StatType.MAX);
                        break;
                    default:
                        // Ignore other stats like stddev and percentiles for now
                        break;
                }
            }

            accCountTimesAvg += (thisCount * thisAvg);
        }

        for (StatsGauge.StatType statType : aggregatedStats) {
            switch (statType) {
                case COUNT:
                    resultSamples.put("count", accCount);
                    break;
                case AVG:
                    if (accCount != 0) {
                        resultSamples.put("avg", accCountTimesAvg / accCount);
                    }
                    break;
                case MIN:
                    resultSamples.put("min", finalMin);
                    break;
                case MAX:
                    resultSamples.put("max", finalmax);
                    break;
                default:
                    // Ignore other stats like stddev and percentiles for now
                    break;
            }
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


    /**
     * Aggregates a single metric from all hosts in the input according to the metric type:
     * counters and histograms are summed, gauges are averaged, stats-gauges merge count/avg/min/max,
     * and records are concatenated.
     *
     * @param aggregationInput  the per-host samples to aggregate; must contain exactly one metric
     * @param aggregationResult the result object to which the aggregated sample is added
     * @return the updated {@code aggregationResult}
     * @throws IllegalArgumentException if the input contains more or fewer than one metric
     */
    public AggregationResult aggregate(AggregationInput aggregationInput, AggregationResult aggregationResult) {
        if (aggregationInput.getMetrics().size() != 1) {
            throw new IllegalArgumentException("DefaultAggregation can only aggregate one metric");
        }

        MetricIdentifier mid = aggregationInput.getMetrics().get(0);

        List<MetricSample> metricSampleMap = aggregationInput.getSamples(mid.getProtocolId(), mid.getMetricName());

        if(metricSampleMap.isEmpty()) {
            return aggregationResult;
        }


        int nHosts = metricSampleMap.size();
        MetricSample sample = metricSampleMap.get(0);
        String[] labelNames = sample.getLabelNames();
        Iterator<MetricSample> it = metricSampleMap.iterator();


        Map<String, Double> resultingSamples;
        switch (sample.getMetricType().toString()){
                case COUNTER_NAME:
                    resultingSamples = aggregateCounter(it);
                    break;
                case GAUGE_NAME:
                    resultingSamples = aggregateGauge(it, nHosts);
                    break;
                case HISTOGRAM_NAME:
                    resultingSamples = aggregateHistogram(it);
                    //Histogram Aggregation
                    break;
                case RECORD_NAME:
                    resultingSamples = aggregateRecord(it);
                    //Record Aggregation
                    break;
            case STATSGAUGE_NAME:
                    resultingSamples = aggregateStatsGauge(it);
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

        if(!globalID.isEmpty()){
            aggregationResult.addSample(resultingMetricSample, mid.getProtocolId(), this.globalID);
        }else {
            aggregationResult.addGlobalSample(resultingMetricSample, mid.getProtocolId());
        }

        return aggregationResult;
    }


}
