package pt.unl.fct.di.novasys.babel.metrics;
import pt.unl.fct.di.novasys.babel.metrics.generic.os.OSMetrics;


/**
 * Abstract base class for metrics that read their values from the operating system (e.g. via {@code /proc}).
 * Subclasses implement {@link #collectMetric()} to sample the specific OS resource they represent.
 */
public abstract class OSMetric extends Metric<OSMetric>{
    OSMetrics.MetricType mt;

    OSMetrics osm;

    /**
     * Constructs an {@code OSMetric} with the given identity and binds it to the shared {@link OSMetrics} source.
     *
     * @param name  the metric name
     * @param unit  the measurement unit
     * @param mt    the Babel metric type (e.g. {@link Metric.MetricType#GAUGE})
     * @param osm   the shared OS metrics reader
     * @param osmt  the OS metric type this instance represents
     */
    public OSMetric(String name, Unit unit,MetricType mt, OSMetrics osm, OSMetrics.MetricType osmt) {
        super(name, unit, mt);
        this.osm = osm;
        this.mt = osmt;
    }

    /**
     * Returns the shared {@link OSMetrics} reader used to poll OS-level data.
     *
     * @return the OS metrics source
     */
    protected OSMetrics getOsMetrics() {
        return osm;
    }


    @Override
    protected void resetThisMetric() {

    }

    @Override
    protected OSMetric newInstance() {
        return null;
    }
}
