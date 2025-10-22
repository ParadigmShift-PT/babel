package pt.unl.fct.di.novasys.babel.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

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


    public Metric.Unit getMetricUnit() {
        return this.metricUnit;
    }

    public String getMetricName() {
        return this.metricName;
    }

    public Metric.MetricType getMetricType() {
        return this.metricType;
    }

    public String[] getLabelNames() {
        return this.labelNames;
    }

    public boolean hasLabels() {
        return this.hasLabels;
    }

    public boolean hasDescription() {
        return !this.description.isEmpty();
    }

    public Sample[] getSamples() {
        return this.samples;
    }

    public String getDescription() {
        return this.description;
    }

    public int getNSamples() { return this.nSamples; }

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

    public static Builder builder(Metric.Unit metricUnit, String metricName, Metric.MetricType metricType) {
        return new Builder(metricUnit, metricName, metricType);
    }

    public static class Builder {
        private final Metric.Unit metricUnit;
        private final String metricName;
        private final Metric.MetricType metricType;
        private String description = "";

        private Sample sample;

        public Builder(Metric.Unit metricUnit, String metricName, Metric.MetricType metricType) {
            this.metricUnit = metricUnit;
            this.metricName = metricName;
            this.metricType = metricType;

        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public LabeledBuilder labelNames(String... labelNames) {
            return new LabeledBuilder(this, labelNames);
        }

        public MetricSample build(Sample sample) {
            this.sample = sample;
            return new MetricSample(this);
        }

    }

    public static class LabeledBuilder{
        private Builder builder;
        private final String[] labelNames;

        private Sample[] samples;

        public LabeledBuilder(Builder builder, String[] labelNames) {
            this.builder = builder;
            this.labelNames = labelNames;
        }

        public MetricSample build(Sample... samples) {
            this.samples = samples;
            return new MetricSample(this);
        }
    }


}