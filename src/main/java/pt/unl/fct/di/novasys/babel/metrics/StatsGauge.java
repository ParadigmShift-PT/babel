package pt.unl.fct.di.novasys.babel.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsGauge extends Metric<Gauge> {
    public enum StatType  {
        /**
         * Average value of the samples
         */
        AVG,

        /**
         * Minimum value of the samples
         */
        MIN,
        /**
         * Maximum value of the samples
         */
        MAX,
        /**
         * Standard deviation of the samples
         */
        STD_DEV,
        /**
         * Count of the samples
         */
        COUNT;
    }

    private static final String LABEL_NAME = "stat_type";

    private Map<String, Long> timedEvents;
    private boolean percentilesEnabled;
    private int[] percentiles;

    private volatile double sum, avg, min, max;
    private volatile long count;

    private final Object lock = new Object();

    private volatile double m2; // sum of squares of differences from the mean

    private List<Double> valuesRegistered;

    private final StatType[] statTypes;


    protected StatsGauge(Builder builder) {
        super(builder);

        if(builder.percentilesEnabled){
            this.percentilesEnabled = true;
            this.percentiles = builder.percentiles;
            this.valuesRegistered = new ArrayList<>(50);
        }
        this.statTypes = builder.statTypes;

        this.timedEvents = new HashMap<>();
        this.doubleInits();

    }

    private void doubleInits(){
        this.sum = 0;
        this.avg = 0;
        this.min = Double.MAX_VALUE;
        this.max = -Double.MAX_VALUE;
        this.count = 0;
        this.m2 = 0.0;
    }


    public void updateStats(double value) {
        if(isDisabled()) return;

        if(value < min) {
            min = value;
        }
        if(value > max) {
            max = value;
        }

        sum += value;
        count++;

        double delta = value - avg;
        avg += delta / count;
        m2 += delta * (value - avg);



        if(percentilesEnabled) {
            synchronized (lock) {
                valuesRegistered.add(value);
            }
        }

    }

    public void startTimedEvent(String eventID){
        if(isDisabled()) return;
        if(timedEvents.containsKey(eventID)){
            return; // Event already started
        }
        timedEvents.put(eventID, System.currentTimeMillis());
    }

    public void stopTimedEvent(String eventID){
        if(isDisabled()) return;
        if(!timedEvents.containsKey(eventID)){
            return;
        }
        double elapsedTime = System.currentTimeMillis() - timedEvents.remove(eventID);
        updateStats(elapsedTime);
    }

    public void observe(double value) {
        if(isDisabled()) return;
        updateStats(value);
    }


    @Override
    protected void resetThisMetric() {
        this.doubleInits();

            if(percentilesEnabled) {
        synchronized (lock) {
                valuesRegistered.clear();
            }
        }
    }

    private double calculatePercentile(List<Double> valuesRegistered, int percentile){
        //Using nearest rank method for percentile calculation
        if (valuesRegistered.isEmpty()) {
            return 0.0; // No values registered, return 0
        }

        int index = (int) Math.ceil((percentile / 100.0) * valuesRegistered.size()) - 1;


        return valuesRegistered.get(Math.max(0, index));
    }

    @Override
    protected MetricSample collectMetric() {
        Sample[] samples = new Sample[statTypes.length + (percentilesEnabled ? percentiles.length : 0)];
        String[] labelNames = new String[]{LABEL_NAME};
        int index = 0;

        for (StatType statType : statTypes) {
            switch (statType) {
                case AVG:
                    samples[index++] = new Sample(avg, labelNames, new String[]{"avg"});
                    break;
                case MIN:
                    samples[index++] = new Sample(min,labelNames, new String[]{"min"});
                    break;
                case MAX:
                    samples[index++] = new Sample(max, labelNames, new String[]{"max"});
                    break;
                case STD_DEV:
                    double stdDev = count > 1 ? Math.sqrt(m2 / (count - 1)) : 0.0;
                    samples[index++] = new Sample(stdDev, labelNames, new String[]{"std_dev"});
                    break;
                case COUNT:
                    samples[index++] = new Sample(count, labelNames, new String[]{"count"});
                    break;
            }
        }

        if(percentilesEnabled) {
            List<Double> valuesRegisteredSnapshot;
            synchronized (lock) {
                valuesRegisteredSnapshot = new ArrayList<>(valuesRegistered);
            }
            valuesRegisteredSnapshot.sort(Double::compareTo);
            
            for (int percentile : percentiles) {
                double pValue = calculatePercentile(valuesRegisteredSnapshot,percentile);
                samples[index++] = new Sample(pValue,labelNames, new String[]{"p"+percentile});
            }
        }

        return sampleBuilder().labelNames(labelNames).build(samples);
    }

    @Override
    protected Gauge newInstance() {
        return new Gauge.Builder(getName(), getUnit()).build();
    }



    public static class Builder extends MetricBuilder<Builder> {

        private StatType[] statTypes = new StatType[]{StatType.AVG, StatType.MIN, StatType.MAX};
        private boolean percentilesEnabled = false;
        private int[] percentiles;


        public Builder(String name, String unit) {
            super(name, unit, MetricType.GAUGE);
        }

        public Builder statTypes(StatType... statTypes) {
            this.statTypes = statTypes;
            return this;
        }

        public Builder percentiles(int... percentile) {
            if (percentile == null || percentile.length == 0) {
                throw new IllegalArgumentException("Percentiles cannot be null or empty");
            }
            for (int p : percentile) {
                if (p < 0 || p > 100) {
                    throw new IllegalArgumentException("Percentiles must be between 0 and 100");
                }
            }
            this.percentilesEnabled = true;
            this.percentiles = percentile;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public StatsGauge build() {
            return new StatsGauge(this);
        }
    }
}
