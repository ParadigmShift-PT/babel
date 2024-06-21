package pt.unl.fct.di.novasys.babel.metrics;

import pt.unl.fct.di.novasys.babel.metrics.exceptions.LabeledMetricException;
import pt.unl.fct.di.novasys.babel.metrics.exporters.CollectOptions;
import pt.unl.fct.di.novasys.babel.metrics.simplemetrics.SimpleGauge;

import java.util.Arrays;
import java.util.Map;

public class Gauge extends LabeledMetric<SimpleGauge> {

   // private final Object metricLock = new Object();

    private SimpleGauge unlabelledGauge;



    public Gauge(String name, String unit, String... labelNames) {
        super(name, unit, MetricType.GAUGE, labelNames);
        if(isUnlabeledMetric())
            this.unlabelledGauge = new SimpleGauge();
    }

    @Override
    protected void reset() {
        if(isUnlabeledMetric()){
            this.unlabelledGauge.reset();
        }else{
            for (SimpleGauge gauge : labelValues.values()){
                gauge.reset();
            }
        }
    }



    public void set(double value) {
        if(isUnlabeledMetric()){
            this.unlabelledGauge.set(value);
        }
        else{
            throw new LabeledMetricException();
        }
    }



    public SimpleGauge labelValues(String... labelValues) {
        if (labelValues.length != getNumLabels())
            throw new IllegalArgumentException("Invalid number of labels");

        LabelValues lv = new LabelValues(labelValues);
        if (!this.labelValues.containsKey(lv)) {
            SimpleGauge lg = new SimpleGauge();
            this.labelValues.put(lv, lg);
            return lg;
        }

        return this.labelValues.get(lv);
    }


    @Override
    protected MetricSample collect(CollectOptions collectOptions)  {
        if (isUnlabeledMetric())
            return MetricSample.builder(getUnit(), getName(), getType()).build(new Sample(this.unlabelledGauge.getValue()));


        Sample[] samples = new Sample[labelValues.size()];
        int index = 0;

        for (Map.Entry<LabelValues, SimpleGauge> entry : labelValues.entrySet()) {
            LabelValues sampleLabelValues = entry.getKey();
            SimpleGauge sampleLabeledCounter = entry.getValue();

            String[] labelValuesSample = Arrays.copyOf(sampleLabelValues.getLabelValues(), getNumLabels());

            double valueSample = sampleLabeledCounter.getValue();

            samples[index++] = new Sample(valueSample, labelValuesSample);
        }

        if(collectOptions.getResetOnCollect()){
            reset();
        }


        return MetricSample.builder(getUnit(), getName(), getType()).labelNames(getLabelNames()).build(samples);
    }

}
