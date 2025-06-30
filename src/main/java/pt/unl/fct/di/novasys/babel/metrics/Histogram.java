package pt.unl.fct.di.novasys.babel.metrics;

import pt.unl.fct.di.novasys.babel.metrics.exceptions.LabeledMetricException;
import java.util.Arrays;

public class Histogram extends Metric<Histogram> {
    private final Object lock = new Object();

    /**
     * The upper bound of each bucket
     */
    double[] buckets;

    /**
     * Upper bound of each bucket as a string, last bucket is "+Inf"
     */
    String[] buckets_labels;

    /**
     * The number of observations for each of the buckets with the upper bound specified in the <i>buckets</i> array
     */
    double[] observations_per_bucket;

    /**
     * The sum of all observations
     */
    double sum;

    /**
     * The number of observations
     */
    double count;


    long time;
    boolean timerStarted = false;


    String[] labelNames = new String[]{"le"};



    protected Histogram(Builder builder){
        super(builder);
        init(builder.buckets, false);
    }

    private Histogram(Metric<Histogram> metric, double[] buckets, String[] bucket_labels ) {
        super(metric);
        this.buckets = buckets;
        this.buckets_labels = bucket_labels;
        init(buckets, true);
    }

    private void init(double[] buckets, boolean bucketsReady) {
        if(!bucketsReady) {
            this.buckets = new double[buckets.length + 1];
            System.arraycopy(buckets, 0, this.buckets, 0, buckets.length);

            this.buckets[this.buckets.length - 1] = Double.MAX_VALUE;

            this.buckets_labels = new String[this.buckets.length];
            for (int i = 0; i < this.buckets.length - 1; i++) {
                this.buckets_labels[i] = String.valueOf(buckets[i]);
            }
            this.buckets_labels[this.buckets.length - 1] = "+Inf";
        }
        if(isUnlabeledMetric()) {
            this.observations_per_bucket = new double[this.buckets.length];
            this.sum = this.count = 0;
        }
    }

    public static class Builder extends MetricBuilder<Builder> {

        double[] buckets;

        public Builder(String name, String unit, double[] buckets, String... labelNames) {
            super(name, unit, MetricType.HISTOGRAM, labelNames);
            this.buckets = buckets;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public Histogram build() {
            return new Histogram(this);
        }
    }


    /**
     * Start the timer for the histogram <br>
     * If the timer was already started, it throws an IllegalStateException
     * If the histogram is labeled, it throws a LabeledMetricException, as labeled histograms must be recorded with the labelValues method
     */
    public void startTimer(){
        if(isDisabled()) return;

        if(!isUnlabeledMetric())
            throw new LabeledMetricException();

        if(timerStarted)
            throw new IllegalStateException("Timer already started");


        time = System.nanoTime();
    }

    /**
     * Stops the timer and records the elapsed time in the histogram <br>
     * If the timer was not started, it throws an IllegalStateException <br>
     * If the histogram is labeled, it throws a LabeledMetricException, as labeled histograms must be recorded with the labelValues method
     */
    public void stopTimer(){
        if(isDisabled()) return;

        if(!isUnlabeledMetric())
            throw new LabeledMetricException();

        if(!timerStarted)
            throw new IllegalStateException("Timer not started");


        record(System.nanoTime() - time);
        time = 0;
        timerStarted = false;
    }


    /**
     * Record a value in the histogram, incrementing the corresponding bucket <br>
     * If the histogram is labeled, it throws a LabeledMetricException, as labeled histograms must be recorded with the labelValues method
     * @param value the value to record
     */
    public void record(double value){
        if(isDisabled()) return;
        if(isUnlabeledMetric()){
            synchronized (lock) {
                for (int i = 0; i < buckets.length; i++) {
                    if (value <= buckets[i]) {
                        observations_per_bucket[i]++;
                        break;
                    }
                }
                sum += value;
                count++;
            }
        }
        else
            throw new LabeledMetricException();
    }


    /**
     * Returns an array of samples, one for each bucket, one for the sum and one for the count
     * @return an array of samples, one for each bucket, one for the sum and one for the count
     */
    private Sample[] sampleFromSimpleHistogram(String[] labelNames) {
        double[] observations = this.observations_per_bucket;
        double sum = this.sum;
        double count = this.count;

        Sample[] samples = new Sample[observations.length + 2];

        //Here we add the labelValues to the sample, these are the labelValues + the bucket upper bound
        for (int i = 0; i < observations.length; i++) {
            samples[i] = new Sample(observations[i], labelNames, new String[]{this.buckets_labels[i]});
        }

        //Second to last sample is the sum, these carry the same label values, without the bucket upper bound
        samples[samples.length - 2] = new Sample(sum, labelNames, new String[]{"sum"});

        //Last sample is the count
        samples[samples.length - 1] = new Sample(count, labelNames, new String[]{"count"});

        return samples;
    }

    @Override
    protected void resetThisMetric() {
        synchronized (lock) {
            Arrays.fill(this.observations_per_bucket, 0);
            this.sum = this.count = 0;
        }
    }

    @Override
    protected MetricSample collectMetric() {
        Sample[] samples;
        synchronized (lock) {
            samples = sampleFromSimpleHistogram(this.labelNames);
        }
        return sampleBuilder().labelNames(this.labelNames).build(samples);
    }

    @Override
    protected Histogram newInstance() {
        return new Histogram(this, this.buckets, this.buckets_labels);
    }
}
