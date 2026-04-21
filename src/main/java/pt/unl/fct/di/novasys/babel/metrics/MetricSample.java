package pt.unl.fct.di.novasys.babel.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * An immutable snapshot of a single metric collected at a point in time.
 * Unlabeled metrics produce one {@link Sample}; labeled metrics and histograms produce multiple.
 * Use {@link #builder} to construct instances.
 */
public class MetricSample implements Serializable {
    private final Metric.Unit metricUnit;
    private final String metricName;
    private final Metric.MetricType metricType;
    private final String description;

    @JsonIgnore
    private final String[] labelNames;

    private final boolean hasLabels;
    private final Sample[] samples;
    private final int nSamples;


    private MetricSample(Builder builder) {
        this.metricUnit = builder.metricUnit;
        this.metricName = builder.metricName;
        this.metricType = builder.metricType;
        this.description = builder.description;
        this.labelNames = new String[]{};
        this.hasLabels = false;
        this.samples = new Sample[]{builder.sample};
        this.nSamples = 1;
    }

    private MetricSample(LabeledBuilder labeledBuilder) {
        this.metricUnit = labeledBuilder.builder.metricUnit;
        this.metricName = labeledBuilder.builder.metricName;
        this.metricType = labeledBuilder.builder.metricType;
        this.description = labeledBuilder.builder.description;
        this.labelNames = labeledBuilder.labelNames;
        this.hasLabels = true;
        this.samples = labeledBuilder.samples;
        this.nSamples = labeledBuilder.samples.length;
    }


    /**
     * Returns the measurement unit of the metric that produced this sample.
     *
     * @return the metric unit
     */
    public Metric.Unit getMetricUnit() {
        return this.metricUnit;
    }

    /**
     * Returns the name of the metric that produced this sample.
     *
     * @return the metric name
     */
    public String getMetricName() {
        return this.metricName;
    }

    /**
     * Returns the type of the metric that produced this sample.
     *
     * @return the metric type
     */
    public Metric.MetricType getMetricType() {
        return this.metricType;
    }

    /**
     * Returns the label names associated with this sample, or an empty array if the metric is unlabeled.
     *
     * @return the label names array
     */
    public String[] getLabelNames() {
        return this.labelNames;
    }

    /**
     * Returns {@code true} if this sample carries label names and values.
     *
     * @return {@code true} for labeled samples
     */
    public boolean hasLabels() {
        return this.hasLabels;
    }

    /**
     * Returns {@code true} if a non-empty description is associated with this sample.
     *
     * @return {@code true} when a description is present
     */
    public boolean hasDescription() {
        return !this.description.isEmpty();
    }

    /**
     * Returns the array of individual data points captured in this sample.
     *
     * @return the samples array
     */
    public Sample[] getSamples() {
        return this.samples;
    }

    /**
     * Returns the description associated with this metric sample, or an empty string if none was set.
     *
     * @return the description string
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the number of {@link Sample} objects held by this metric sample.
     *
     * @return the sample count
     */
    public int getNSamples() { return this.nSamples; }

    /**
     * Returns a deep copy of this {@code MetricSample} with cloned {@link Sample} instances.
     *
     * @return a new {@code MetricSample} with the same data
     */
    public MetricSample clone() {
        Sample[] samples = new Sample[this.samples.length];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = this.samples[i].clone();
        }

        if(this.hasLabels){
            String[] labelNames = new String[this.labelNames.length];
            System.arraycopy(this.labelNames, 0, labelNames, 0, this.labelNames.length);
            MetricSample.builder(metricUnit, metricName, metricType)
                    .description(description)
                    .labelNames(labelNames)
                    .build(samples);
        }

        return new Builder(metricUnit, metricName, metricType)
                .description(description)
                .build(samples[0]);
    }

    /**
     * Creates a new {@link Builder} for constructing a {@code MetricSample} with the given identity fields.
     *
     * @param metricUnit the measurement unit
     * @param metricName the metric name
     * @param metricType the metric type
     * @return a new {@link Builder}
     */
    public static Builder builder(Metric.Unit metricUnit, String metricName, Metric.MetricType metricType) {
        return new Builder(metricUnit, metricName, metricType);
    }

    /** Builder for unlabeled and labeled {@link MetricSample} instances. */
    public static class Builder {
        private final Metric.Unit metricUnit;
        private final String metricName;
        private final Metric.MetricType metricType;
        private String description = "";

        private Sample sample;

        /**
         * Creates a builder pre-populated with the metric identity fields.
         *
         * @param metricUnit the measurement unit
         * @param metricName the metric name
         * @param metricType the metric type
         */
        public Builder(Metric.Unit metricUnit, String metricName, Metric.MetricType metricType) {
            this.metricUnit = metricUnit;
            this.metricName = metricName;
            this.metricType = metricType;

        }

        /**
         * Sets the human-readable description for the metric sample.
         *
         * @param description the description text
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Transitions to a {@link LabeledBuilder} for building a multi-sample (labeled) metric sample.
         *
         * @param labelNames the label dimension names
         * @return a {@link LabeledBuilder} configured with this builder's fields and the given label names
         */
        public LabeledBuilder labelNames(String... labelNames) {
            return new LabeledBuilder(this, labelNames);
        }

        /**
         * Builds an unlabeled {@link MetricSample} containing the given single {@link Sample}.
         *
         * @param sample the data point
         * @return a new {@link MetricSample}
         */
        public MetricSample build(Sample sample) {
            this.sample = sample;
            return new MetricSample(this);
        }

    }

    /** Builder for labeled {@link MetricSample} instances that carry multiple {@link Sample} objects. */
    public static class LabeledBuilder{
        private Builder builder;
        private final String[] labelNames;

        private Sample[] samples;

        /**
         * Creates a {@code LabeledBuilder} wrapping the given base builder and label names.
         *
         * @param builder    the parent builder carrying the metric identity fields
         * @param labelNames the label dimension names
         */
        public LabeledBuilder(Builder builder, String[] labelNames) {
            this.builder = builder;
            this.labelNames = labelNames;
        }

        /**
         * Builds a labeled {@link MetricSample} containing the given array of {@link Sample} objects.
         *
         * @param samples the data points, one per label value combination
         * @return a new labeled {@link MetricSample}
         */
        public MetricSample build(Sample... samples) {
            this.samples = samples;
            return new MetricSample(this);
        }
    }


}