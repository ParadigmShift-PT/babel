package pt.unl.fct.di.novasys.babel.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Sample implements Serializable {
    private final Map<String,String> labels;
    private final double value;

    /**
     * Creates a sample with the given value and labels
     * @param sampledValue sampled value of the metric
     * @param labelNames names of the labels
     * @param labelValues values of the labels
     */
    public Sample(double sampledValue, String[] labelNames, String[] labelValues) {
        this.value = sampledValue;
        this.labels = new HashMap<>();
        for (int i = 0; i < labelNames.length; i++) {
            this.labels.put(labelNames[i], labelValues[i]);
        }
    }

    /**
     * Creates a sample with the given value and no labels
     * @param sampledValue sampled value of the metric
     */
    public Sample(double sampledValue) {
        this.value = sampledValue;
        this.labels = new HashMap<>();
    }


    /**
     * Returns the map containing the labels and corresponding label value<br>
     * Example: key = "protocol", value = "TCP"
     * @return map containing the labels and values of the sample
     */
    public Map<String,String> getLabels() {
        return this.labels;
    }

    @JsonIgnore
    public String[] getLabelsNames() {
        return labels.keySet().toArray(new String[0]);
    }

    @JsonIgnore
    public String[] getLabelsValues() {
        return labels.values().toArray(new String[0]);
    }
    /**
     * Returns the value of the sample
     * @return value of the sample
     */
    public double getValue() {
        return value;
    }

    public Sample clone(){
        return new Sample(this.value, this.getLabelsNames(), this.getLabelsValues());
    }
}
