package pt.unl.fct.di.novasys.babel.metrics;


import pt.unl.fct.di.novasys.babel.metrics.exceptions.LabeledMetricException;
import java.util.concurrent.atomic.AtomicLong;

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

    public static class Builder extends MetricBuilder<Builder>{

        public Builder(String name, String unit, String... labelNames) {
            super(name, unit, MetricType.COUNTER, labelNames);
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public Counter build() {
            return new Counter(this);
        }
    }

    public void inc() {
       inc(1);
    }

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
