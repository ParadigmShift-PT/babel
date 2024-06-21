package pt.unl.fct.di.novasys.babel.metrics;


import pt.unl.fct.di.novasys.babel.metrics.exceptions.LabeledMetricException;
import pt.unl.fct.di.novasys.babel.metrics.exporters.CollectOptions;
import pt.unl.fct.di.novasys.babel.metrics.simplemetrics.SimpleCounter;

import java.util.Arrays;
import java.util.Map;

public class Counter extends LabeledMetric<SimpleCounter> {

    private SimpleCounter unlabeledCounter;

//    private Object collectLock = new Object();


    public Counter(String name, String unit, String... labelNames) {
        super(name, unit, MetricType.COUNTER, labelNames);
        if(isUnlabeledMetric())
            this.unlabeledCounter = new SimpleCounter();
    }


    public void inc() {
        if(isUnlabeledMetric())
            this.unlabeledCounter.inc();
        else
            throw new LabeledMetricException();
    }


    public void inc(long n) {
       if(isUnlabeledMetric())
            this.unlabeledCounter.inc();
       else
            throw new LabeledMetricException();
    }


    public SimpleCounter labelValues(String... labelValues) {
        if (labelValues.length != getNumLabels())
            throw new IllegalArgumentException("Invalid number of labels");

        LabelValues lv = new LabelValues(labelValues);
        if (!this.labelValues.containsKey(lv)) {
            SimpleCounter lc = new SimpleCounter();
            this.labelValues.put(lv, lc);
            return lc;
        }

        return this.labelValues.get(lv);
    }


    @Override
    public synchronized void reset() {
        if(isUnlabeledMetric())
            this.unlabeledCounter.reset();
        else {
            for (SimpleCounter counter : labelValues.values())
                counter.reset();
        }
    }


    @Override
    protected MetricSample collect(CollectOptions collectOptions) {

        if(isUnlabeledMetric())
            return MetricSample.builder(getUnit(), getName(), getType()).build(new Sample(this.unlabeledCounter.getValue()));

        Sample[] samples = new Sample[labelValues.size()];
        int index = 0;

        for (Map.Entry<LabelValues, SimpleCounter> entry : labelValues.entrySet()) {
            LabelValues sampleLabelValues = entry.getKey();
            SimpleCounter sampleSimpleCounter = entry.getValue();

            String[] labelValuesSample = Arrays.copyOf(sampleLabelValues.getLabelValues(), getNumLabels());

            double valueSample = sampleSimpleCounter.getValue();

            samples[index++] = new Sample(valueSample, labelValuesSample);
        }

        if (collectOptions.getResetOnCollect()){
            reset();
        }

        return MetricSample.builder(getUnit(), getName(), getType()).labelNames(getLabelNames()).build(samples);    
    }
}
