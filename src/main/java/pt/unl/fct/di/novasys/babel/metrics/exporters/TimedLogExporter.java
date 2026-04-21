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

        /**
         * Sets the formatter used to render each metric snapshot before writing to the log file.
         *
         * @param formatter the {@link NodeSampleFormatter} to use
         * @return this builder
         */
        public Builder setFormatter(NodeSampleFormatter formatter){
            properties.setProperty(FORMATTER,formatter.getFormatterName());
            return this;
        }

        /**
         * Sets the directory path where the log file is written.
         * The file is named {@code <exporterName>.log} within that directory.
         *
         * @param path the target directory path (trailing slash optional)
         * @return this builder
         */
        public Builder setLogPath(String path){
            properties.setProperty(LOG_PATH, path);
            return this;
        }

        /**
         * Sets the export interval in seconds.
         *
         * @param interval seconds between successive log writes
         * @return this builder
         */
        public Builder setInterval(long interval){
            properties.setProperty(INTERVAL, Long.toString(interval));
            return this;
        }


        @Override
        public Builder self() {
            return this;
        }


        /**
         * Builds and returns a configured {@link TimedLogExporter}.
         *
         * @return a new {@code TimedLogExporter}
         */
        @Override
        public TimedLogExporter build() {
            exporterConfigs(properties);
            return new TimedLogExporter(this);
        }
    }

    /**
     * Creates a {@code TimedLogExporter} from the supplied builder, resolving the formatter
     * from the configured property (defaulting to {@link SimpleFormatter}).
     *
     * @param builder the fully configured builder
     */
    public TimedLogExporter(Builder builder) {
        super(builder);
        this.formatter = getFormatterOrDefault(getProperty(FORMATTER), new SimpleFormatter());

    }


    /**
     * Returns the default configuration: interval of 10 seconds, log path {@code ./},
     * and no default formatter (falls back to {@code SimpleFormatter} at construction time).
     *
     * @return properties containing default values for {@code INTERVAL} and {@code LOG_PATH}
     */
    @Override
    public Properties loadDefaults() {
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty(INTERVAL, "10");
        defaultProperties.setProperty(LOG_PATH, "./");
        return defaultProperties;
    }

    /**
     * Runs the export loop: sleeps for the configured interval, then collects all metrics,
     * formats the snapshot, and appends it to the log file. Exits when the exporter is disabled.
     */
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

