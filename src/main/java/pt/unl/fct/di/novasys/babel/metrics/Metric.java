package pt.unl.fct.di.novasys.babel.metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.IncorrectLabelNumberException;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.UnlabelledMetricException;
import pt.unl.fct.di.novasys.babel.metrics.exporters.CollectOptions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Metric<T extends Metric<T>>{

    private static final Logger logger = LogManager.getLogger(Metric.class);

    public enum MetricType implements Serializable {
        COUNTER ("counter"),
        GAUGE ("gauge"),
        HISTOGRAM ("histogram"),
        RECORD("record"),
        STATSGAUGE("statsgauge");

        private final String type;

        MetricType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static class Unit {
        public static final String BYTES = "bytes";
        public static final String KBYTES = "Kbytes";
        public static final String SECONDS = "seconds";
        public static final String PERCENTAGE = "%";
        public static final String NONE = "";
    }

    private final String name;
    private final String unit;
    private final MetricType type;
    private String description;
    private AtomicBoolean disabled = new AtomicBoolean(false);
    private Map<LabelValues, T> labelValues;
    private final String[] labelNames;
    private final boolean hasLabels;
    private CollectOptions collectOptions = CollectOptions.DEFAULT_COLLECT_OPTIONS;

    protected Metric(String name, String unit, MetricType metricType, String... labelNames){
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
        private final String unit;
        private final MetricType type;
        private final String[] labelNames;
        private String description = "";
        private CollectOptions collectOptions;

        public MetricBuilder(String name, String unit, MetricType metricType, String... labelNames) {
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


    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    protected MetricType getType() {
        return type;
    }


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
