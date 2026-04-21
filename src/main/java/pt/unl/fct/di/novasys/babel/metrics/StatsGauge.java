package pt.unl.fct.di.novasys.babel.metrics;

import java.util.*;

/**
 * A gauge metric that accumulates a stream of observed values and reports configurable
 * statistics (avg, min, max, std-dev, count, sum) and optional percentiles on collection.
 * All statistical aggregates are computed incrementally using Welford's online algorithm for
 * variance.
 */
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
        COUNT,
        /**
         * Sum of the samples
         */
        SUM;
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


    /**
     * Incorporates {@code value} into all running aggregates (min, max, sum, count, mean, M2).
     * If percentile tracking is enabled, also appends the value to the raw sample list.
     *
     * @param value the observed value to incorporate
     */
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

    /**
     * Records the start time for the named event. If an event with the same ID is already in progress,
     * the call is silently ignored.
     *
     * @param eventID an application-defined identifier for the event being timed
     */
    public void startTimedEvent(String eventID){
        if(isDisabled()) return;
        if(timedEvents.containsKey(eventID)){
            return; // Event already started
        }
        timedEvents.put(eventID, System.currentTimeMillis());
    }

    /**
     * Stops the named event timer, computes the elapsed milliseconds since {@link #startTimedEvent},
     * and passes the duration to {@link #updateStats}. If the event was never started, the call is
     * silently ignored.
     *
     * @param eventID the identifier used when {@link #startTimedEvent} was called
     */
    public void stopTimedEvent(String eventID){
        if(isDisabled()) return;
        if(!timedEvents.containsKey(eventID)){
            return;
        }
        double elapsedTime = System.currentTimeMillis() - timedEvents.remove(eventID);
        updateStats(elapsedTime);
    }

    /**
     * Records an observed value, updating all running statistics.
     * Equivalent to calling {@link #updateStats(double)} directly.
     *
     * @param value the value to observe
     */
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
                case COUNT:
                    samples[index++] = new Sample(count, labelNames, new String[]{"count"});
                    break;
                case AVG:
                    samples[index++] = new Sample(avg, labelNames, new String[]{"avg"});
                    break;
                case SUM:
                    samples[index++] = new Sample(sum, labelNames, new String[]{"sum"});
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



    /** Builder for {@link StatsGauge} metrics. */
    public static class Builder extends MetricBuilder<Builder> {

        private StatType[] statTypes = new StatType[]{StatType.AVG, StatType.MIN, StatType.MAX};
        private boolean percentilesEnabled = false;
        private int[] percentiles;

        /**
         * Creates a builder for a {@link StatsGauge} with the given name and unit.
         *
         * @param name the metric name
         * @param unit the measurement unit
         */
        public Builder(String name, Unit unit) {
            super(name, unit, MetricType.STATSGAUGE);
        }

        /**
         * Creates a builder for a {@link StatsGauge} with the given name and unit string.
         *
         * @param name the metric name
         * @param unit the measurement unit as a string
         */
        public Builder(String name, String unit) {
            super(name, Unit.of(unit), MetricType.STATSGAUGE);
        }

        /**
         * Sets the statistical aggregates to report on collection. If {@link StatType#AVG} is included,
         * {@link StatType#COUNT} is automatically added because it is required to compute the mean.
         *
         * @param statTypes the stat types to report
         * @return this builder
         */
        public Builder statTypes(StatType... statTypes) {
            Set<StatType> statTypeSet = new HashSet<>();
            for (StatType st : statTypes) {
                statTypeSet.add(st);
                if(st.equals(StatType.AVG)) {
                    statTypeSet.add(StatType.COUNT);
                }
            }
            this.statTypes = statTypeSet.toArray(new StatType[0]);

            return this;
        }

        /**
         * Enables percentile reporting and sets the percentile ranks to compute on collection.
         * All values are retained in memory between resets to support exact percentile calculation.
         *
         * @param percentile one or more integer percentile ranks in the range [0, 100]
         * @return this builder
         * @throws IllegalArgumentException if the array is null/empty or any value is outside [0, 100]
         */
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

        /**
         * Builds and returns a new {@link StatsGauge} instance.
         *
         * @return a new {@link StatsGauge}
         */
        @Override
        public StatsGauge build() {
            return new StatsGauge(this);
        }
    }
}
