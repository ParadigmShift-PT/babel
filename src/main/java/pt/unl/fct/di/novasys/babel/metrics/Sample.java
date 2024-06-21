package pt.unl.fct.di.novasys.babel.metrics;

public class Sample {
    private final String[] labelValues;
    private final double valueSample;

    /**
     *
     * @param sample
     * @param labelValues
     */
    public Sample(double sample, String... labelValues) {
        this.valueSample = sample;
        this.labelValues = labelValues;
    }

    /**
     *
     * @param sample
     */
    public Sample(double sample) {
        this.valueSample = sample;
        this.labelValues = new String[0];
    }


    /**
     * Returns the label values of the sample
     * @return label values of the sample
     */
    public String[] getLabelValues() {
        return labelValues;
    }

    /**
     * Returns the value of the sample
     * @return value of the sample
     */
    public double getValueSample() {
        return valueSample;
    }
}
