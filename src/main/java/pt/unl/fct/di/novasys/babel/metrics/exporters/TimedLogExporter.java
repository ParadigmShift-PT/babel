package pt.unl.fct.di.novasys.babel.metrics.exporters;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;
import pt.unl.fct.di.novasys.babel.metrics.formatting.NodeSampleFormatter;
import pt.unl.fct.di.novasys.babel.metrics.formatting.SimpleFormatter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import static java.lang.Thread.sleep;


/** Export metrics to a log file at regular intervals
 * <br>
 * The log is saved in the specified path. If no path is specified, the log is saved in the current working directory under the name exporterName.log
 */
public class TimedLogExporter extends ThreadedExporter{

    public static final String FORMATTER = "FORMATTER";
    public static final String INTERVAL = "INTERVAL";
    public static final String LOG_PATH = "LOG_PATH";

    private static final long S_TO_MS = 1000;

    private final NodeSampleFormatter formatter;


    public static class Builder extends ExporterBuilder<Builder> {
        private NodeSampleFormatter formatter;
        Properties properties = new Properties();

        /**
         * Builder for TimedLogExporter
         * <br>
         * Default values:
         * <ul>
         *     <li>INTERVAL: 10</li>
         *     <li>LOG_PATH: ./</li>
         *     <li>FORMATTER: SimpleFormatter</li>
         * </ul>
         *
         */
        public Builder(String exporterName) {
            super(exporterName);
        }

        public Builder setFormatter(NodeSampleFormatter formatter){
            properties.setProperty(FORMATTER,formatter.getFormatterName());
            return this;
        }

        public Builder setLogPath(String path){
            properties.setProperty(LOG_PATH, path);
            return this;
        }

        public Builder setInterval(long interval){
            properties.setProperty(INTERVAL, Long.toString(interval));
            return this;
        }


        @Override
        public Builder self() {
            return this;
        }


        @Override
        public TimedLogExporter build() {
            exporterConfigs(properties);
            return new TimedLogExporter(this);
        }
    }

    public TimedLogExporter(Builder builder) {
        super(builder);
        this.formatter = getFormatterOrDefault(getProperty(FORMATTER), new SimpleFormatter());

    }


    @Override
    public Properties loadDefaults() {
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty(INTERVAL, "10");
        defaultProperties.setProperty(LOG_PATH, "./");
        return defaultProperties;
    }

    @Override
    public void run() {
        long sleep_ms = Long.parseLong(this.getProperty(INTERVAL)) * S_TO_MS;

        FileWriter writer;
        try {
            writer = new FileWriter(this.getProperty(LOG_PATH) + this.getExporterName() + ".log", true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            try {
                sleep(sleep_ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //If the exporter is disabled, stop the thread
            if(isDisabled()) return;
            
            try {
                NodeSample sample = collectAllMetrics();
                String formattedSample = formatter.format(sample);
                // Write to file
                writer.write(formattedSample);
                writer.write("\n");
                writer.flush();

            } catch (NoSuchProtocolRegistry noSuchProtocolRegistry) {
                noSuchProtocolRegistry.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }
}

