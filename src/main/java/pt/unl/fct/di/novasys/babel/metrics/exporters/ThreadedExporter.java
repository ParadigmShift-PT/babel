package pt.unl.fct.di.novasys.babel.metrics.exporters;

/**
 * Class to be extended by exporters that are to have their own thread <br>
 * Exporters that are meant to be initialized by the Metrics Manager (i.e. not meant to be protocols) should extend this class
 */
public abstract class ThreadedExporter extends Exporter implements Runnable{
    /**
     * Constructs a threaded exporter from the supplied builder, delegating configuration loading
     * to the parent {@link Exporter}.
     *
     * @param exporterBuilder the fully configured builder
     */
    public ThreadedExporter(ExporterBuilder exporterBuilder) {
        super(exporterBuilder);
    }

    /**
     * Method that specifies the logic of the exporter.<br>
     * This method is called when the exporter thread is started.<br>
     * It should contain the logic to collect and export metrics.
     */
    public abstract void run();
}
