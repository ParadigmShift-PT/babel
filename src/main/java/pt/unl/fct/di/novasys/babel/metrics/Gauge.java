package pt.unl.fct.di.novasys.babel.metrics;

import pt.unl.fct.di.novasys.babel.metrics.exceptions.LabeledMetricException;

public class Gauge extends Metric<Gauge> {

    private double value;

    private final Object lock = new Object();

    protected Gauge(Builder builder) {
        super(builder);
        if(isUnlabeledMetric())
            this.value = 0;
    }

    private Gauge(Metric<Gauge> metric){
        super(metric);
        this.value = 0;
    }

    public static class Builder extends MetricBuilder<Builder> {

        public Builder(String name, Unit unit, String... labelNames) {
            super(name, unit, MetricType.GAUGE, labelNames);
        }

        public Builder(String name, String unit, String... labelNames) {
            super(name, Unit.of(unit), MetricType.GAUGE, labelNames);
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public Gauge build() {
            return new Gauge(this);
        }
    }

    @Override
    protected void resetThisMetric() {
        synchronized (lock){
            this.value = 0;
        }
    }

    @Override
    protected MetricSample collectMetric() {
        Sample sample;
        synchronized (lock) {
            sample = new Sample(this.value);
        }
        return sampleBuilder().build(sample);
    }

    @Override
    protected Gauge newInstance() {
        return new Gauge(this);
    }

    public void set(double value) {
        if(isDisabled()) return;
        if(isUnlabeledMetric()){
            synchronized (lock){
                this.value = value;
            }
        }
        else{
            throw new LabeledMetricException();
        }
    }
}
