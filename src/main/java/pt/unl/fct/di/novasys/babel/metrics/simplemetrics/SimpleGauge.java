package pt.unl.fct.di.novasys.babel.metrics.simplemetrics;

public class SimpleGauge {
    private double value;

    private final Object lock = new Object();

    public SimpleGauge() {
        this.value = 0;
    }

    public void set(double value) {
        synchronized (lock){
            this.value = value;
        }

    }

    public double getValue() {
        synchronized (lock){
            return value;
        }
    }


    public void reset(){
        synchronized (lock){
            this.value = 0;
        }
    }
}
