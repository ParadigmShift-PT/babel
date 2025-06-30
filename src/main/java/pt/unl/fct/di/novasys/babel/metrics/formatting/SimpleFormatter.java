package pt.unl.fct.di.novasys.babel.metrics.formatting;

import pt.unl.fct.di.novasys.babel.metrics.*;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;

import java.util.Map;
import java.util.Set;

/**
 * Produces a simple string representation of the metrics<br>
 * protocolId timestamp metricName {label1="label1value1",...} value unit<br>
 * Example:<br>
 * protocol1 123456 metric1 1.0 <br>
 * protocol1 123456 metric2{label1="value1",label2="value2"} 2.0 Kbytes<br>
 *
 * For NodeSampleFormatter:<br>
 * NODE=node1 123456 protocol1 metric1 1.0 <br>
 * GLOBAL 123456 protocol1 metric1 1.0<br>
 */
public class SimpleFormatter implements NodeSampleFormatter, IdentifiedNodeSampleFormatter {
    public static final String NAME = "SimpleFormatter";
    public static final String NO_HOST_PREFIX = "";

    @Override
    public String getFormatterName() {
        return NAME;
    }

    @Override
    public String format(String host, NodeSample sample){
        StringBuilder sbhost = new StringBuilder();
        Set<Short> protocols = sample.getProtocols();
            if(host.equals(MetricsManager.GLOBAL_HOST_IDENTIFIER)){
                sbhost.append(MetricsManager.GLOBAL_HOST_IDENTIFIER);
            }else{
                sbhost.append("NODE=");
                sbhost.append(host);
            }
        String host_string = sbhost.toString();


        StringBuilder sb = new StringBuilder();
        for (short protocolID : protocols) {
            ProtocolSample protocolSample = sample.getProtocolSample(protocolID);
            sb.append(formatProtocolMetrics(host_string, protocolSample.getProtocolName(), protocolSample));
        }
        return sb.toString();
    }

    @Override
    public String format(Map<String, NodeSample> samples) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, NodeSample> entry : samples.entrySet()) {
            String host = entry.getKey();
            NodeSample sample = entry.getValue();
            sb.append(format(host, sample));
        }
        return sb.toString();
    }


    @Override
    public String format(NodeSample sample) throws NoSuchProtocolRegistry {
        StringBuilder sb = new StringBuilder();
        Set<Short> protocolIDs = sample.getProtocols();
        for (short protocolID : protocolIDs) {
            ProtocolSample protocolSample = sample.getProtocolSample(protocolID);
            sb.append(formatProtocolMetrics(NO_HOST_PREFIX, protocolSample.getProtocolName(), protocolSample));
        }
        return sb.toString();
    }





    private StringBuilder formatProtocolMetrics(String host_string, String protocolName, ProtocolSample sample) {
        StringBuilder sb = new StringBuilder();

        String cleanProtoName = protocolName.replace(" ", "_");

        for(MetricSample metricSample : sample.getMetricSamples()){
            if(metricSample.hasLabels()){
                sb.append(formatLabeledMetric(host_string, cleanProtoName, metricSample, sample.getTimestamp()));

            }else{
                if(!host_string.equals(NO_HOST_PREFIX)){
                    sb.append(host_string);
                    sb.append(" ");
                }
                sb.append(sample.getTimestamp());
                sb.append(" ");
                sb.append(cleanProtoName);
                sb.append(" ");
                sb.append(metricSample.getMetricName());
                sb.append(" ");
                sb.append(metricSample.getSamples()[0].getValue());
                if(!metricSample.getMetricUnit().equals(Metric.Unit.NONE)){
                    sb.append(" ");
                    sb.append(metricSample.getMetricUnit());
                }
            }
            sb.append("\n");
        }
        return sb;
    }


    private StringBuilder formatLabeledMetric(String host_string, String cleanProtocolName, MetricSample metricSample, long timestamp) {
        StringBuilder sb = new StringBuilder();
        for (Sample sample : metricSample.getSamples()) {
                if(!host_string.equals(NO_HOST_PREFIX)){
                    sb.append(host_string);
                    sb.append(" ");
                }
                sb.append(timestamp);
                sb.append(" ");
                sb.append(cleanProtocolName);
                sb.append(" ");
                sb.append(metricSample.getMetricName());
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

            sb.append(" ");
            sb.append(sample.getValue());
            if(!metricSample.getMetricUnit().equals(Metric.Unit.NONE)){
                sb.append(" ");
                sb.append(metricSample.getMetricUnit());
            }
            sb.append("\n");
        }

        return sb;
    }
}
