package pt.unl.fct.di.novasys.babel.metrics;

import pt.unl.fct.di.novasys.babel.metrics.exporters.CollectOptions;

public abstract class Metric {

    public enum MetricType {
        COUNTER ("counter"),
        GAUGE ("gauge"),
        HISTOGRAM ("histogram");

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
        public static final String SECONDS = "seconds";
        public static final String PERCENTAGE = "percentage";
        public static final String NONE = "";
    }

    private final String name;
    private final String unit;

    private final MetricType type;

    
    //private Consumer<Metric> onChangeHandler;

    public Metric(String name, String unit, MetricType metricType) {
        this.name = name.replace(" ", "_");
        this.unit = unit;
        this.type = metricType;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public MetricType getType() {
        return type;
    }



//    protected void setOnChangeHandler(Consumer<Metric> handler) {
//        this.onChangeHandler = handler;
//    }
//
//    protected void onChange() {
//        if (onChangeHandler != null)
//            onChangeHandler.accept(this);
//    }

    protected abstract void reset();

    //protected abstract String computeValue();

    protected abstract MetricSample collect(CollectOptions collectOptions);

}
