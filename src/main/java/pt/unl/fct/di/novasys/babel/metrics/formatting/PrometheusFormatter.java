package pt.unl.fct.di.novasys.babel.metrics.formatting;

import pt.unl.fct.di.novasys.babel.metrics.*;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;

import java.util.Map;

import static pt.unl.fct.di.novasys.babel.metrics.Metric.*;
import static pt.unl.fct.di.novasys.babel.metrics.MetricsManager.OS_METRIC_PROTOCOL_ID;

/**
 * Formats a {@link NodeSample} in the Prometheus text exposition format.
 * Each metric is prefixed with {@code Protocol_<id>_} or {@code OS_} and counter names
 * receive the mandatory {@code _total} suffix as required by OpenMetrics.
 */
public class PrometheusFormatter implements NodeSampleFormatter {

    public static final String NAME = "PrometheusFormatter";

    private static final int SUM = 1;
    private static final int COUNT = 2;

    /**
     * Constructs a new {@code PrometheusFormatter}.
     */
    public PrometheusFormatter() {}



    private StringBuilder formatSample(String metricName, String[] labelNames, long timestamp, Sample sample){
        StringBuilder sb = new StringBuilder();

        //If the metric has labels, we need to append each of the labels to the metric name
        if(labelNames.length > 0){
                sb.append(metricName);
                sb.append("{");
            for(Map.Entry<String, String> entry : sample.getLabels().entrySet()){
                sb.append(entry.getKey());
                sb.append("=");
                sb.append("\"");
                sb.append(entry.getValue());
                sb.append("\"");
                sb.append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("}");
        }
        else{
            //If not labels, only the name
            sb.append(metricName);
        }

        sb.append(" ");
        sb.append(sample.getValue());
        if(timestamp != -1){
            sb.append(" ");
            sb.append(timestamp);
        }
        return sb;
    }

    private StringBuilder formatMetricSample(short protocolID, MetricSample metricSample, long timestamp){
        StringBuilder sb = new StringBuilder();

        //Prometheus doesn't support multiple protocols, so we need to append the protocolID to the metric name,
        // also units are to be added to the end of the metric name
        StringBuilder nameSb = new StringBuilder();

        //Prefix depends on if it is a protocol metric or OS metric
        if(protocolID == OS_METRIC_PROTOCOL_ID){
            nameSb.append("OS_");
        }else{
            nameSb.append("Protocol_");
            nameSb.append(protocolID);
            nameSb.append("_");
        }

        nameSb.append(metricSample.getMetricName());


        if(!metricSample.getMetricUnit().equals(Metric.Unit.NONE)){
            nameSb.append("_");
            nameSb.append(metricSample.getMetricUnit());
        }else{
            //As defined in OpenMetrics, counter metric names must have the _total suffix. If you create a counter without the _total suffix the suffix will be appended automatically.
            // TODO: can we do this only for metrics with no unit?
            if(metricSample.getMetricType() == MetricType.COUNTER && !metricSample.getMetricName().endsWith("_total")){
                nameSb.append("_total");
            }
        }

        String metricName = nameSb.toString();

        //HELP line
        if(metricSample.hasDescription()){
            sb.append("# HELP ");
            sb.append(metricName);
            sb.append(" ");
            sb.append(metricSample.getDescription());
            sb.append("\n");
        }

        //TYPE line
        sb.append("# TYPE ");
        sb.append(nameSb);
        sb.append(" ");
        sb.append(metricSample.getMetricType().type());
        sb.append("\n");


        //SAMPLE

        if(!metricSample.hasLabels()){
            sb.append(nameSb);
            sb.append(" ");
            sb.append(metricSample.getSamples()[0].getValue());
            sb.append("\n");
            return sb;
        }

        switch (metricSample.getMetricType().toString()){
            case MetricType.COUNTER_NAME:
            case MetricType.GAUGE_NAME:
                for(int i = 0; i < metricSample.getNSamples(); i++){
                    sb.append(formatSample(metricName, metricSample.getLabelNames(), timestamp, metricSample.getSamples()[i]));
                    sb.append("\n");
                }
                break;
            case MetricType.HISTOGRAM_NAME:
                /**
                 * http_request_duration_seconds_bucket{method="GET",status="200",le="0.1"} 100
                 * http_request_duration_seconds_bucket{method="GET",status="200",le="0.2"} 150
                 * http_request_duration_seconds_bucket{method="GET",status="200",le="0.5"} 200
                 * http_request_duration_seconds_bucket{method="GET",status="200",le="1.0"} 250
                 * http_request_duration_seconds_bucket{method="GET",status="200",le="+Inf"} 300
                 * http_request_duration_seconds_sum{method="GET",status="200"} 120
                 * http_request_duration_seconds_count{method="GET",status="200"} 300
                 * http_request_duration_seconds_bucket{method="POST",status="200",le="0.1"} 50
                 * http_request_duration_seconds_bucket{method="POST",status="200",le="0.2"} 80
                 * http_request_duration_seconds_bucket{method="POST",status="200",le="0.5"} 100
                 * http_request_duration_seconds_bucket{method="POST",status="200",le="1.0"} 120
                 * http_request_duration_seconds_bucket{method="POST",status="200",le="+Inf"} 150
                 * http_request_duration_seconds_sum{method="POST",status="200"} 55
                 * http_request_duration_seconds_count{method="POST",status="200"} 150
                 */

                int count_or_sum = SUM;

                //the last label refers to the buckets, so we remove the last label
                String[] labelNamesWithoutLe = new String[metricSample.getLabelNames().length - 1];
                System.arraycopy(metricSample.getLabelNames(), 0, labelNamesWithoutLe, 0, metricSample.getLabelNames().length - 1);


                for(int i = 0; i < metricSample.getNSamples(); i++){
                    Sample sample = metricSample.getSamples()[i];
                    /*
                    * If the value of the last label is "sum" or "count", we remove that label and append "sum" or "count" to the metric name
                     */

                    String[] histogramLabelValues = sample.getLabelsValues();
                    String lastLabelValue = histogramLabelValues[histogramLabelValues.length - 1];
                    if(lastLabelValue.equals("sum") || lastLabelValue.equals("count")){
                        if(count_or_sum == SUM){
                            sb.append(formatSample(metricName + "_sum", labelNamesWithoutLe, timestamp, sample));
                            count_or_sum = COUNT;
                        }
                        else{
                            sb.append(formatSample(metricName + "_count", labelNamesWithoutLe, timestamp, sample));
                            count_or_sum = SUM;
                        }
                        sb.append("\n");
                    }else{
                        sb.append(formatSample(metricName + "_bucket", metricSample.getLabelNames(), timestamp, sample));
                        sb.append("\n");
                    }
                }




                //SUM
              }


        return sb;
    }


    @Override
    public String getFormatterName() {
        return NAME;
    }

    public String format(NodeSample sample) throws NoSuchProtocolRegistry {
        StringBuilder sb = new StringBuilder();

        // Since prometheus doesn't support multiple protocols, we need to iterate over the protocolIds, and append the protocolId to the metric name
        for(short protocolID : sample.getProtocols()){
            ProtocolSample protocolSample = sample.getProtocolSample(protocolID);

            //Timestamp is the epoch of the sample, so we use it for all the metrics in the sample
            long timestamp = protocolSample.getTimestamp();

            for(MetricSample metricSample : protocolSample.getMetricSamples()){
                //We don't want to format record metrics, as they are not supported by Prometheus
                if(metricSample.getMetricType() != Metric.MetricType.RECORD) {
                    sb.append(formatMetricSample(protocolID, metricSample, timestamp));
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }
}
