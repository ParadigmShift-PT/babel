package pt.unl.fct.di.novasys.babel.metrics;
import pt.unl.fct.di.novasys.babel.metrics.generic.os.OSMetrics;


public abstract class OSMetric extends Metric{
    OSMetrics.MetricType mt;

    OSMetrics osm;

    public OSMetric(String name, String unit,MetricType mt, OSMetrics osm, OSMetrics.MetricType osmt) {
        super(name, unit, mt);
        this.osm = osm;
        this.mt = osmt;
    }

    protected OSMetrics getOsMetrics() {
        return osm;
    }

}
