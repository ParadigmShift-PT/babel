package pt.unl.fct.di.novasys.babel.metrics.simplemetrics;

import java.util.concurrent.atomic.AtomicLong;

public class SimpleCounter {
    private AtomicLong value;

    public SimpleCounter() {
        value = new AtomicLong();
    }

    public void inc(long n) {
        this.value.addAndGet(n);
    }

    public void inc() {
        inc(1);
    }

    public void reset(){this.value.set(0);}


    public double getValue() {
        return (double) value.get();
    }
}
