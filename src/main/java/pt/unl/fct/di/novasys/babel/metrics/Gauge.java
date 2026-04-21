package pt.unl.fct.di.novasys.babel.metrics;

import pt.unl.fct.di.novasys.babel.metrics.exceptions.LabeledMetricException;

/**
 * A gauge metric that holds a single mutable {@code double} value.
 * Unlike a counter, a gauge may be set to any value, including decreasing values.
 */
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

    /** Builder for {@link Gauge} metrics. */
    public static class Builder extends MetricBuilder<Builder> {

        /**
         * Creates a gauge builder with the given name, unit, and optional label names.
         *
         * @param name       the metric name
         * @param unit       the measurement unit
         * @param labelNames optional dimension label names for a labeled gauge
         */
        public Builder(String name, Unit unit, String... labelNames) {
            super(name, unit, MetricType.GAUGE, labelNames);
        }

        /**
         * Creates a gauge builder with the given name, unit string, and optional label names.
         *
         * @param name       the metric name
         * @param unit       the measurement unit as a string
         * @param labelNames optional dimension label names for a labeled gauge
         */
        public Builder(String name, String unit, String... labelNames) {
            super(name, Unit.of(unit), MetricType.GAUGE, labelNames);
        }

        @Override
        public Builder self() {
            return this;
        }

        /**
         * Builds and returns a new {@link Gauge} instance.
         *
         * @return a new {@link Gauge}
         */
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

    /**
     * Sets the gauge to the given value.
     *
     * @param value the new gauge value
     * @throws pt.unl.fct.di.novasys.babel.metrics.exceptions.LabeledMetricException if this is a labeled metric; use {@link #labelValues} instead
     */
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
