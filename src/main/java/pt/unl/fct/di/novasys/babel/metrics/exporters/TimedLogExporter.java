package pt.unl.fct.di.novasys.babel.metrics.exporters;

import pt.unl.fct.di.novasys.babel.metrics.MultiRegistryEpochSample;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;
import pt.unl.fct.di.novasys.babel.metrics.formatting.Formatter;
import pt.unl.fct.di.novasys.babel.metrics.formatting.PrometheusFormatter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import static java.lang.Thread.sleep;


/** Export metrics to a log file at regular intervals
 * <br>
 * The log is saved in the specified path. If no path is specified, the log is saved in the current working directory under the name exporterName.log
 */
public class TimedLogExporter extends Exporter{

    private static final long S_TO_MS = 1000;


    public TimedLogExporter(String exporterName){
        super(exporterName);
    }

    public TimedLogExporter(String exporterName, String configPath) {
        super(exporterName, configPath);
    }

    public TimedLogExporter(String exporterName, String configPath, ExporterCollectOptions exporterCollectOptions) {
        super(exporterName, configPath, exporterCollectOptions);
    }

    public TimedLogExporter(String exporterName, ExporterCollectOptions exporterCollectOptions) {
        super(exporterName, exporterCollectOptions);
    }

    public TimedLogExporter(String exporterName, Properties exporterConfigs, ExporterCollectOptions exporterCollectOptions) {
        super(exporterName, exporterConfigs, exporterCollectOptions);
    }


    @Override
    public Properties loadDefaults() {
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty("INTERVAL", "60");
        defaultProperties.setProperty("LOG_PATH", "./");
        return defaultProperties;
    }

    @Override
    public void run() {
        long sleep_ms = Long.parseLong(this.getProperty("INTERVAL")) * S_TO_MS;
        Formatter formatter = new PrometheusFormatter();

        FileWriter writer = null;
        try {
            writer = new FileWriter(this.getProperty("LOG_PATH") + this.getExporterName() + ".log", true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            try {
                sleep(sleep_ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Collecting metrics");
            try {
                MultiRegistryEpochSample sample = collectAllMetrics();

                String formattedSample = formatter.format(sample);
                // Write to file

                writer.write(System.currentTimeMillis() + "\n");
                writer.write(formattedSample);
                writer.write("--------------------\n");
                writer.flush();


            } catch (NoSuchProtocolRegistry noSuchProtocolRegistry) {
                noSuchProtocolRegistry.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }
}
