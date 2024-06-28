package pt.unl.fct.di.novasys.babel.metrics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class LabeledMetric<T> extends Metric {


    protected final Map<LabelValues, T> labelValues = new HashMap<>();

    private final String[] labelNames;

    private final boolean hasLabels;

    LabeledMetric(String name, String unit, MetricType metricType, String... labelNames){
        super(name, unit, metricType);

        this.labelNames = labelNames;
        this.hasLabels = labelNames.length > 0;
    }

    public String[] getLabelNames() {
        return Arrays.copyOf(labelNames, labelNames.length);
    }

    public boolean isUnlabeledMetric() {
        return !hasLabels;
    }

    public int getNumLabels() {
        return labelNames.length;
    }


    public abstract T labelValues(String ...labelValues);



}
