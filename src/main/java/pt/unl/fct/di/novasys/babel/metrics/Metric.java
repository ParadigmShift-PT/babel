package pt.unl.fct.di.novasys.babel.metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.IncorrectLabelNumberException;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.UnlabelledMetricException;
import pt.unl.fct.di.novasys.babel.metrics.exporters.CollectOptions;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for all Babel metrics. Supports both unlabeled (single-value) and labeled (multi-dimensional)
 * variants. Labeled metrics maintain one child instance per unique {@link LabelValues} combination.
 *
 * @param <T> the concrete metric subtype
 */
public abstract class Metric<T extends Metric<T>>{

    private static final Logger logger = LogManager.getLogger(Metric.class);

    /**
     * Identifies the kind of metric (counter, gauge, histogram, record, or statsgauge).
     * Pre-defined constants cover all built-in types; custom types may be created via {@link #of}.
     */
    public record MetricType(String type) implements Serializable {
            private static final Map<String, MetricType> REGISTRY = new HashMap<>(5);


            public static final String COUNTER_NAME = "counter";
            public static final String GAUGE_NAME = "gauge";
            public static final String HISTOGRAM_NAME = "histogram";
            public static final String RECORD_NAME = "record";
            public static final String STATSGAUGE_NAME = "statsgauge";


            public static final MetricType COUNTER = new MetricType(COUNTER_NAME);
            public static final MetricType GAUGE = new MetricType(GAUGE_NAME);
            public static final MetricType HISTOGRAM = new MetricType(HISTOGRAM_NAME);
            public static final MetricType RECORD = new MetricType(RECORD_NAME);
            public static final MetricType STATSGAUGE = new MetricType(STATSGAUGE_NAME);

            /**
             * Creates a {@code MetricType} with the given type string and registers it in the global registry.
             *
             * @param type the type identifier string
             */
            public MetricType(String type) {
                this.type = type;
                REGISTRY.put(type.toLowerCase(Locale.ROOT), this);
            }

            /**
             * Returns the {@code MetricType} for the given string, creating and registering it if absent.
             *
             * @param type the type identifier string (case-insensitive)
             * @return the matching or newly created {@code MetricType}
             */
            public static MetricType of(String type) {
                return REGISTRY.computeIfAbsent(type.toLowerCase(Locale.ROOT), MetricType::new);
            }

            @Override
            public String toString() {
                return type;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                MetricType that = (MetricType) o;
                return Objects.equals(type, that.type);
            }
    }

    /**
     * Represents the measurement unit of a metric. Pre-defined constants cover common units;
     * custom units may be created via {@link #of}.
     */
    public record Unit(String type) implements Serializable{
        private static final Map<String, Unit> REGISTRY = new HashMap<>(5);

        public static final String BYTES_STRING = "bytes";
        public static final String KBYTES_STRING = "Kbytes";

        public static final String SECONDS_STRING = "s";
        public static final String MILLISECONDS_STRING = "ms";
        public static final String PERCENTAGE_STRING = "%";
        public static final String EMPTY = "";

        public static final Unit BYTES = new Unit(BYTES_STRING);
        public static final Unit KBYTES = new Unit(KBYTES_STRING);
        public static final Unit SECONDS = new Unit(SECONDS_STRING);
        public static final Unit MILLISECONDS = new Unit(MILLISECONDS_STRING);
        public static final Unit PERCENTAGE = new Unit(PERCENTAGE_STRING);
        public static final Unit NONE = new Unit(EMPTY);

        /**
         * Creates a {@code Unit} with the given type string and registers it in the global registry.
         *
         * @param type the unit identifier string
         */
        public Unit(String type) {
            this.type = type;
            REGISTRY.put(type.toLowerCase(Locale.ROOT), this);
        }

        /**
         * Returns the {@code Unit} for the given string, creating and registering it if absent.
         *
         * @param type the unit identifier string (case-insensitive)
         * @return the matching or newly created {@code Unit}
         */
        public static Unit of(String type) {
            return REGISTRY.computeIfAbsent(type.toLowerCase(Locale.ROOT), Unit::new);
        }

        @Override
        public String toString() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Unit that = (Unit) o;
            return Objects.equals(type, that.type);
        }
    }

    private final String name;
    private final Unit unit;
    private final MetricType type;
    private String description;
    private AtomicBoolean disabled = new AtomicBoolean(false);
    private Map<LabelValues, T> labelValues;
    private final String[] labelNames;
    private final boolean hasLabels;
    private CollectOptions collectOptions = CollectOptions.DEFAULT_COLLECT_OPTIONS;

    protected Metric(String name, Unit unit, MetricType metricType, String... labelNames){
        this.name = name.replace(" ", "_");
        this.unit = unit;
        this.type = metricType;
        this.labelNames = labelNames;
        this.hasLabels = labelNames.length > 0;
        this.description = "";
        if(this.hasLabels){
            this.labelValues = new ConcurrentHashMap<>();
        }
    }


    protected Metric(MetricBuilder<?> builder){
        this(builder.name, builder.unit, builder.type, builder.labelNames);

        if(!builder.description.isEmpty()){
            this.description = builder.description;
        }

        if(builder.collectOptions != null){
            this.collectOptions = builder.collectOptions;
        }
    }

    /**
     * Constructor for unlabeled metrics belonging to a set of labelValues. <br>
     */
    protected Metric(Metric<T> m) {
        this(m.getName(), m.getUnit(), m.getType());
        this.description = m.getDescription();
        this.disabled = m.getDisabled();
    }

    protected abstract static class MetricBuilder<A extends MetricBuilder<A>> {
        private final String name;
        private final Unit unit;
        private final MetricType type;
        private final String[] labelNames;
        private String description = "";
        private CollectOptions collectOptions;

        public MetricBuilder(String name, Unit unit, MetricType metricType, String... labelNames) {
            this.name = name;
            this.unit = unit;
            this.type = metricType;
            this.labelNames = labelNames;
        }

        public A collectOptions(CollectOptions collectOptions) {
            this.collectOptions = collectOptions;
            return self();
        }

        public abstract A self();

        public A description(String description) {
            this.description = description;
            return self();
        }

        public abstract Metric<?> build();
    }


    /**
     * Returns the name of this metric.
     *
     * @return the metric name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the unit of measurement for this metric.
     *
     * @return the metric unit
     */
    public Unit getUnit() {
        return unit;
    }

    protected MetricType getType() {
        return type;
    }

    /**
     * Returns the human-readable description of this metric, or an empty string if none was set.
     *
     * @return the metric description
     */
    public String getDescription() {
        return description;
    }

    protected boolean isDisabled(){return disabled.get();}

    protected AtomicBoolean getDisabled() {
        return disabled;
    }

    private String[] getLabelNames() {
        return Arrays.copyOf(labelNames, labelNames.length);
    }

    protected boolean isUnlabeledMetric() {
        return !hasLabels;
    }

    private int getNumLabels() {
        return labelNames.length;
    }

    /**
     * Resets the metric to its initial state. <br>
     * Must be implemented by subclasses. <br>
     * This method is called by the reset() method of this class. <br>
     */
    protected abstract void resetThisMetric();

    /**
     * Collects the metric data and returns a MetricSample. <br>
     * This method must be implemented by subclasses. <br>
     * @return a MetricSample containing the metric data. <br>
     */
    protected abstract MetricSample collectMetric();

    /**
     * Creates a new unlabeled instance of the metric. <br>
     * This is used when a new label value is added to the metric. <br>
     * This method must be implemented by subclasses. <br>
     * @return a new instance of the metric. <br>
     */
    protected abstract T newInstance();

    /**
     * Resets this metric (and all its labeled child instances) to their initial zero state.
     * Has no effect if the metric is disabled.
     */
    public final synchronized void reset() {
        if (isDisabled()) return;

        if (isUnlabeledMetric()) {
        logger.debug("unlabelled {} reset() called", this.name);
            resetThisMetric(); // Call subclass-specific reset
        } else {
            logger.debug("labeled {} reset() called", this.name);
            for (T metric : labelValues.values()) {
                metric.reset(); // Recursive reset on child metrics
            }
        }
    }

    protected void disable() {
        disabled.set(true);
        logger.debug("{} isDisabled() {}", this.name, disabled.get());
    }

    protected MetricSample collect(){
        return collect(this.collectOptions);
    }

    protected MetricSample collect(CollectOptions collectOptions) {
        if(isDisabled()){
            return sampleBuilder().build(new Sample(0));
        }

        MetricSample resultMetricSample;

        if (isUnlabeledMetric()) {
            resultMetricSample = collectMetric();
        }
        else {
            List<Sample> samples = new ArrayList<>();

            for (Map.Entry<LabelValues, T> entry : labelValues.entrySet()) {
                LabelValues sampleLabelValues = entry.getKey();
                MetricSample metricSample = entry.getValue().collectMetric();
                String[] labelNames;

                if (metricSample.hasLabels()) {
                    int labelLength = getNumLabels() + metricSample.getLabelNames().length;
                    labelNames = new String[labelLength];
                    System.arraycopy(getLabelNames(), 0, labelNames, 0, getNumLabels());
                    System.arraycopy(metricSample.getLabelNames(), 0, labelNames, getNumLabels(), metricSample.getLabelNames().length);
                    for (int i = 0; i < metricSample.getNSamples(); i++) {
                        Sample sample = metricSample.getSamples()[i];
                        String[] labelValues = new String[labelLength];
                        System.arraycopy(sampleLabelValues.getLabelValues(), 0, labelValues, 0, getNumLabels());
                        System.arraycopy(sample.getLabelsValues(), 0, labelValues, getNumLabels(), metricSample.getLabelNames().length);
                        samples.add(new Sample(sample.getValue(), labelNames, labelValues));
                    }
                } else {
                    labelNames = getLabelNames();
                    String[] labelValuesSample = Arrays.copyOf(sampleLabelValues.getLabelValues(), getNumLabels());
                    samples.add(new Sample(metricSample.getSamples()[0].getValue(), labelNames, labelValuesSample));
                }
            }

            resultMetricSample = sampleBuilder().labelNames(getLabelNames()).build(samples.toArray(new Sample[0]));
        }

        if (collectOptions.getResetOnCollect()) {
            reset();
        }

        return resultMetricSample;
    }

    /**
     * Returns a builder for a MetricSample with the same name, unit and type as this metric.<br>
     * If the metric has a description, it will be added to the builder.
     * @return a builder for a MetricSample with the same name, unit and type as this metric
     */
    public MetricSample.Builder sampleBuilder() {
        MetricSample.Builder builder = MetricSample.builder(this.unit, this.name, this.type);

        if(description != null)
            builder = builder.description(description);

        return builder;
    }


    /**
     * Returns the child metric instance for the given label values, creating it if it does not yet exist.
     * Throws {@link pt.unl.fct.di.novasys.babel.metrics.exceptions.UnlabelledMetricException} if called
     * on an unlabeled metric, and
     * {@link pt.unl.fct.di.novasys.babel.metrics.exceptions.IncorrectLabelNumberException} if the number
     * of values does not match the number of declared label names.
     *
     * @param labelValues the label values identifying the desired child instance
     * @return the existing or newly created child metric for these label values
     */
    public T labelValues(String ...labelValues){
        if(isUnlabeledMetric()){
            throw new UnlabelledMetricException(this.getType(), this.getName());
        }

        if(isDisabled()) return newInstance();

        if (labelValues.length != getNumLabels())
            throw new IncorrectLabelNumberException();

        LabelValues lv = new LabelValues(labelValues);
        if (!this.labelValues.containsKey(lv)) {
            T newMetricInstance = newInstance();
            this.labelValues.put(lv, newMetricInstance);
            return newMetricInstance;
        }

        return this.labelValues.get(lv);

    }



}
