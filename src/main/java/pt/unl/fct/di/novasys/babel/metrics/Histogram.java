package pt.unl.fct.di.novasys.babel.metrics;


import pt.unl.fct.di.novasys.babel.metrics.exceptions.LabeledMetricException;
import pt.unl.fct.di.novasys.babel.metrics.exporters.CollectOptions;
import pt.unl.fct.di.novasys.babel.metrics.simplemetrics.SimpleHistogram;

import java.util.Arrays;
import java.util.Map;

public class Histogram extends LabeledMetric<SimpleHistogram> {
    private final Object lock = new Object();
    double[] buckets;

    long time;
    boolean timerStarted = false;

    String[] buckets_labels;

    SimpleHistogram unlabeledHistogram;


    public Histogram(String name, String unit, double[] buckets, String... labelNames){
        super(name, unit, MetricType.HISTOGRAM, labelNames);
        this.buckets = new double[buckets.length + 1];
        System.arraycopy(buckets, 0, this.buckets, 0, buckets.length);

        this.buckets[this.buckets.length - 1] = Double.MAX_VALUE;

        this.buckets_labels = new String[this.buckets.length];
        for(int i = 0; i < buckets.length; i++){
            this.buckets_labels[i] = String.valueOf(buckets[i]);
        }
        this.buckets_labels[buckets_labels.length - 1] = "+Inf";

        if(isUnlabeledMetric())
            this.unlabeledHistogram = new SimpleHistogram(this.buckets);

    }


    /**
     * Start the timer for the histogram <br>
     * If the timer was already started, it throws an IllegalStateException
     * If the histogram is labeled, it throws a LabeledMetricException, as labeled histograms must be recorded with the labelValues method
     */
    public void startTimer(){
        if(timerStarted)
            throw new IllegalStateException("Timer already started");

        if(!isUnlabeledMetric())
            throw new LabeledMetricException();

        time = System.nanoTime();
    }

    /**
     * Stops the timer and records the elapsed time in the histogram <br>
     * If the timer was not started, it throws an IllegalStateException <br>
     * If the histogram is labeled, it throws a LabeledMetricException, as labeled histograms must be recorded with the labelValues method
     */
    public void stopTimer(){
        if(!timerStarted)
            throw new IllegalStateException("Timer not started");

        if(!isUnlabeledMetric())
            throw new LabeledMetricException();

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
        if(isUnlabeledMetric())
            unlabeledHistogram.record(value);
        else
            throw new LabeledMetricException();
    }


    /**
     * Reset the histogram, setting the observations for each bucket to 0 <br>
     * If the histogram is labeled, it resets all labeled histograms
     */
    @Override
    protected void reset() {
        if(isUnlabeledMetric()){
            unlabeledHistogram.reset();
        }else{
            for (SimpleHistogram histogram : labelValues.values()){
                histogram.reset();
            }
        }
    }


    /**
     * Returns the SimpleHistogram for the given label values
     * @param labelValues the label values
     * @return the SimpleHistogram for the given label values
     */
    public SimpleHistogram labelValues(String... labelValues) {
        if (labelValues.length != getNumLabels())
            throw new IllegalArgumentException("Invalid number of labels");

        LabelValues lv = new LabelValues(labelValues);
        if (!this.labelValues.containsKey(lv)) {
            SimpleHistogram lc = new SimpleHistogram(this.buckets);
            this.labelValues.put(lv, lc);
            return lc;
        }

        return this.labelValues.get(lv);
    }



    @Override
    protected MetricSample collect(CollectOptions collectOptions)  {
        Sample[] samples;
        if(isUnlabeledMetric()){
            samples = sampleFromSimpleHistogram(this.unlabeledHistogram);
            return MetricSample.builder(getUnit(), getName(), getType()).labelNames("le").build(samples);
        }


        int index = 0;
        samples = new Sample[(buckets.length + 2) * labelValues.size()];

        for (Map.Entry<LabelValues, SimpleHistogram> entry : labelValues.entrySet()) {
            LabelValues sampleLabelValues = entry.getKey();
            SimpleHistogram sampleSimpleHistogram = entry.getValue();
            Sample[] samplesForLabeledHistogram = sampleFromSimpleHistogram(sampleSimpleHistogram, sampleLabelValues.getLabelValues());

            for (int j = 0; j < samplesForLabeledHistogram.length; j++) {
                samples[index++] = samplesForLabeledHistogram[j];
            }
        }

        String[] labelsWithLeBucketLabel = Arrays.copyOf(getLabelNames(), getNumLabels() + 1);
        labelsWithLeBucketLabel[labelsWithLeBucketLabel.length - 1 ] = "le";


        if (collectOptions.getResetOnCollect()){
            reset();
        }


        return MetricSample.builder(getUnit(), getName(), getType()).labelNames(labelsWithLeBucketLabel).build(samples);
    }


    /**
     * Receives a simple histogram and returns an array of samples, one for each bucket, one for the sum and one for the count
     * @param simpleHistogram the simple histogram to sample
     * @param labelValues if the simple histogram is associated with a label, the label values
     * @return an array of samples, one for each bucket, one for the sum and one for the count
     */
    private Sample[] sampleFromSimpleHistogram(SimpleHistogram simpleHistogram, String... labelValues) {
        double[] observations = simpleHistogram.getObservations();
        double sum = simpleHistogram.getSum();
        double count = simpleHistogram.getCount();

        Sample[] samples = new Sample[observations.length + 2];

        //Here we add the labelValues to the sample, these are the labelValues + the bucket upper bound
        for (int i = 0; i < observations.length; i++) {
            String[] labelValuesWithBucketLabel = new String[labelValues.length + 1];

            if(labelValues.length > 0)
                System.arraycopy(labelValues, 0, labelValuesWithBucketLabel, 0, labelValues.length);

            labelValuesWithBucketLabel[labelValuesWithBucketLabel.length - 1 ] = this.buckets_labels[i];
            samples[i] = new Sample(observations[i], labelValuesWithBucketLabel);
        }
        //Second to last sample is the sum, these carry the same label values, without the bucket upper bound
        samples[samples.length - 2] = new Sample(sum, labelValues);

        //Last sample is the count
        samples[samples.length - 1] = new Sample(count, labelValues);


        return samples;
    }
}
