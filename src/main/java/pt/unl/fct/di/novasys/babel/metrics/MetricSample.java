package pt.unl.fct.di.novasys.babel.metrics;

public class MetricSample {
    private final String metricUnit;
    private final String metricName;

    private final Metric.MetricType metricType;
    private final String[] labelNames;

    private final boolean hasLabels;
    private final Sample[] samples;
    private final int nSamples;

//    private final boolean hasTimestamp;

//    private final long timestamp;

//    public MetricSample(String metricUnit, String metricName, Metric.MetricType metricType, Sample[] samples, String[] labelNames, int nSamples) {
//        this.metricUnit = metricUnit;
//        this.metricName = metricName;
//        this.metricType = metricType;
//        this.labelNames = labelNames;
////        this.timestamp = timestamp;
//        this.hasLabels = true;
//        this.samples = samples;
//        this.nSamples = nSamples;
//    }
//
//
//
//    /**
//     * In the case of unlabelled metrics, there is only one sample
//     * @param metricUnit
//     * @param metricName
//     * @param sample
//     */
//    public MetricSample(String metricUnit, String metricName, Metric.MetricType metricType, Sample sample) {
//        this.metricUnit = metricUnit;
//        this.metricName = metricName;
//        this.metricType = metricType;
////        this.timestamp = timestamp;
//        this.labelNames = new String[]{};
//        this.hasLabels = false;
//        this.samples = new Sample[]{sample};
//        this.nSamples = 1;
//    }


    private MetricSample(Builder builder) {
        this.metricUnit = builder.metricUnit;
        this.metricName = builder.metricName;
        this.metricType = builder.metricType;
        this.labelNames = new String[]{};
        this.hasLabels = false;
        this.samples = new Sample[]{builder.sample};
        this.nSamples = 1;
//        this.hasTimestamp = builder.timestamp != -1;
//        this.timestamp = builder.timestamp;
    }

    private MetricSample(LabeledBuilder labeledBuilder) {
        this.metricUnit = labeledBuilder.builder.metricUnit;
        this.metricName = labeledBuilder.builder.metricName;
        this.metricType = labeledBuilder.builder.metricType;
        this.labelNames = labeledBuilder.labelNames;
        this.hasLabels = true;
        this.samples = labeledBuilder.samples;
        this.nSamples = labeledBuilder.samples.length;
//        this.hasTimestamp = labeledBuilder.builder.timestamp != -1;
//        this.timestamp = labeledBuilder.builder.timestamp;
    }


    public String getMetricUnit() {
        return metricUnit;
    }

    public String getMetricName() {
        return metricName;
    }

    public Metric.MetricType getMetricType() {
        return metricType;
    }

    public String[] getLabelNames() {
        return labelNames;
    }

    public boolean hasLabels() {
        return hasLabels;
    }

    public Sample[] getSamples() {
        return samples;
    }

    public int getNSamples() { return nSamples; }

//    public boolean hasTimestamp() {
//        return hasTimestamp;
//    }

//    public long getTimestamp() {
//        return timestamp;
//    }


    public static Builder builder(String metricUnit, String metricName, Metric.MetricType metricType) {
        return new Builder(metricUnit, metricName, metricType);
    }




    public static class Builder {
        private final String metricUnit;
        private final String metricName;
        private final Metric.MetricType metricType;

        private Sample sample;

        //opt
        private long timestamp = -1;

        public Builder(String metricUnit, String metricName, Metric.MetricType metricType) {
            this.metricUnit = metricUnit;
            this.metricName = metricName;
            this.metricType = metricType;
        }

        public LabeledBuilder labelNames(String... labelNames) {
            return new LabeledBuilder(this, labelNames);
        }

//        public Builder timestamp(long timestamp) {
//            this.timestamp = timestamp;
//            return this;
//        }

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

//        public LabeledBuilder timestamp(long timestamp) {
//            this.builder = builder.timestamp(timestamp);
//            return this;
//        }


        public MetricSample build(Sample... samples) {
            this.samples = samples;
            return new MetricSample(this);
        }
    }



}