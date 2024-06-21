package pt.unl.fct.di.novasys.babel.metrics.simplemetrics;

import java.util.Arrays;

public class SimpleHistogram {

    private static final Object lock = new Object();

    /**
     * The upper bound of each bucket
     */
    double[] buckets;

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

    public SimpleHistogram(double[] buckets){
        this.buckets = buckets;
        this.sum = this.count = 0;
        this.observations_per_bucket = new double[buckets.length];
    }

    /**
     * Start the timer for the histogram <br>
     * If the timer was already started, it throws an IllegalStateException
     */
    public void startTimer(){
        if(timerStarted)
            throw new IllegalStateException("Timer already started");

        time = System.nanoTime();
    }

    /**
     * Stops the timer and records the elapsed time in the histogram <br>
     * If the timer was not started, it throws an IllegalStateException
     */
    public void stopTimer(){
        if(!timerStarted)
            throw new IllegalStateException("Timer not started");

        record(System.nanoTime() - time);
        time = 0;
        timerStarted = false;
    }

    /**
     * Record a value in the histogram, incrementing the corresponding bucket
     * @param value the value to record
     */
    public void record(double value){
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

    /**
     * Reset the histogram, setting the observations for each bucket to 0
     */
    public void reset() {
        synchronized (lock) {
            Arrays.fill(this.observations_per_bucket, 0);
            this.sum = this.count = 0;
        }
    }


    /**
     * Get the number of observations for each bucket
     * @return an array with the number of observations for each bucket
     */
    public double[] getObservations(){
        synchronized (lock) {
            return Arrays.copyOf(this.observations_per_bucket, this.buckets.length);
        }
    }


    /**
     * Get the sum of all observations
     * @return the sum of all observations
     */
    public double getSum() {
        synchronized (lock) {
            return sum;
        }
    }

    /**
     * Get the number of observations
     * @return the number of observations
     */
    public double getCount() {
        synchronized (lock) {
            return count;
        }
    }
}
