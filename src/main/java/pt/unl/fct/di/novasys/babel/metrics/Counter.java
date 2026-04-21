package pt.unl.fct.di.novasys.babel.metrics;


import pt.unl.fct.di.novasys.babel.metrics.exceptions.LabeledMetricException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A monotonically increasing counter metric backed by an {@link AtomicLong}.
 * The counter value can only be incremented; attempting to decrement it throws
 * an {@link IllegalArgumentException}.
 */
public class Counter extends Metric<Counter> {
    private AtomicLong value;

    protected Counter(Builder builder) {
        super(builder);
        if(isUnlabeledMetric())
            this.value = new AtomicLong();
    }

    private Counter(Metric<Counter> metric){
        super(metric);
        this.value = new AtomicLong();
    }

    /** Builder for {@link Counter} metrics. */
    public static class Builder extends MetricBuilder<Builder>{

        /**
         * Creates a counter builder with the given name, unit, and optional label names.
         *
         * @param name       the metric name (spaces are replaced with underscores)
         * @param unit       the measurement unit
         * @param labelNames optional dimension label names for a labeled counter
         */
        public Builder(String name, Unit unit, String... labelNames) {
            super(name, unit, MetricType.COUNTER, labelNames);
        }

        /**
         * Creates a counter builder with the given name, unit string, and optional label names.
         *
         * @param name       the metric name
         * @param unit       the measurement unit as a string (looked up or created via {@link Unit#of})
         * @param labelNames optional dimension label names for a labeled counter
         */
        public Builder(String name, String unit, String... labelNames) {
            super(name, Unit.of(unit), MetricType.COUNTER, labelNames);
        }

        @Override
        public Builder self() {
            return this;
        }

        /**
         * Builds and returns a new {@link Counter} instance.
         *
         * @return a new {@link Counter}
         */
        @Override
        public Counter build() {
            return new Counter(this);
        }
    }

    /**
     * Increments the counter by 1.
     *
     * @throws pt.unl.fct.di.novasys.babel.metrics.exceptions.LabeledMetricException if this is a labeled metric; use {@link #labelValues} instead
     */
    public void inc() {
       inc(1);
    }

    /**
     * Increments the counter by {@code n}.
     *
     * @param n the amount to add; must be non-negative
     * @throws IllegalArgumentException if {@code n} is negative
     * @throws pt.unl.fct.di.novasys.babel.metrics.exceptions.LabeledMetricException if this is a labeled metric
     */
    public void inc(long n) {
       if(isDisabled()) return;
       if(isUnlabeledMetric()) {
           if (n < 0)
               throw new IllegalArgumentException("Counter cannot be decremented");
           this.value.addAndGet(n);
       }
       else
            throw new LabeledMetricException();
    }
    

    @Override
    protected void resetThisMetric() {
        this.value.set(0);
    }

    protected Counter newInstance() {
        return new Counter(this);
    }

    protected MetricSample collectMetric(){
        return sampleBuilder().build(new Sample(this.value.doubleValue()));
    }

}
