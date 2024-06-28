package pt.unl.fct.di.novasys.babel.metrics.formatting;

import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.metrics.*;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;

import java.util.Set;

/**
 * Produces a simple string representation of the metrics<br>
 * Example:<br>
 * protocol1 metric1 1.0 123456<br>
 * protocol1 metric2{label1="value1",label2="value2"} 2.0 123456<br>
 */
public class SimpleFormatter implements Formatter{
    @Override
    public String format(MultiRegistryEpochSample sample) throws NoSuchProtocolRegistry {
        StringBuilder sb = new StringBuilder();
        Set<Short> registryIds = sample.getRegistryIds();
        for (short registryId : registryIds) {
            sb.append(formatProtocolMetrics(MetricsManager.getInstance().getProtoNameById(registryId), sample.getRegistrySample(registryId)));

        }

        return sb.toString();
    }


    private StringBuilder formatProtocolMetrics(String protocolName, EpochSample sample) {
        StringBuilder sb = new StringBuilder();

        String cleanProtoName = protocolName.replace(" ", "_");

        for(MetricSample metricSample : sample.getMetricSamples()){
            if(metricSample.hasLabels()){
                sb.append(formatLabeledMetric(cleanProtoName, metricSample, sample.getEpoch()));

            }else{
                sb.append(sample.getEpoch());
                sb.append(" ");
                sb.append(cleanProtoName);
                sb.append(" ");
                sb.append(metricSample.getMetricName());
                sb.append(" ");
                sb.append(metricSample.getSamples()[0].getValueSample());
            }
            sb.append("\n");
        }
        return sb;
    }


    private StringBuilder formatLabeledMetric(String cleanProtocolName, MetricSample metricSample, long timestamp) {
        StringBuilder sb = new StringBuilder();
        String[] labelNames = metricSample.getLabelNames();
        sb.append(timestamp);
        sb.append(" ");
        sb.append(cleanProtocolName);
        sb.append(" ");
        for (Sample sample : metricSample.getSamples()) {
            for (int i = 0; i < labelNames.length; i++) {
                sb.append(metricSample.getMetricName());
                sb.append("{");
                sb.append(labelNames[i]);
                sb.append("=");
                sb.append("\"");
                sb.append(sample.getLabelValues()[i]); //Broken for histogram since sum and count have one less label
                sb.append("\"");
                sb.append(",");
            }

            sb.deleteCharAt(sb.length() - 1);
            sb.append("}");

            sb.append(" ");
            sb.append(sample.getValueSample());
            sb.append("\n");
        }

        return sb;
    }
}
