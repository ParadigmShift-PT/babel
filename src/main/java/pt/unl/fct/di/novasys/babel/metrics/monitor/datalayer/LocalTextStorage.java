package pt.unl.fct.di.novasys.babel.metrics.monitor.datalayer;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.babel.metrics.formatting.SimpleFormatter;
import pt.unl.fct.di.novasys.babel.metrics.formatting.IdentifiedNodeSampleFormatter;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * A {@link Storage} implementation that persists aggregated {@link NodeSample} data to a local text file,
 * using a configurable {@link IdentifiedNodeSampleFormatter} (default: {@link SimpleFormatter}).
 * Each call to {@link #store} appends a formatted and flushed line to the file.
 */
public class LocalTextStorage implements Storage{

    private final IdentifiedNodeSampleFormatter formatter;
    private final FileWriter writer;

    /**
     * Builder for {@link LocalTextStorage}.
     * Defaults: {@link SimpleFormatter}, path {@code MonitorTextStorage.txt}, append mode enabled.
     */
    public static class Builder {
        private IdentifiedNodeSampleFormatter formatter = new SimpleFormatter();
        private String path = "MonitorTextStorage.txt";
        private boolean append = true;

        /**
         * Sets the formatter used to convert samples to text.
         *
         * @param formatter the formatter to use
         * @return this builder
         */
        public Builder setFormatter(IdentifiedNodeSampleFormatter formatter) {
            this.formatter = formatter;
            return this;
        }

        /**
         * Sets the file path for the output file, creating parent directories if necessary.
         *
         * @param path the file path; must not be null, empty, or end with {@code /}
         * @return this builder
         * @throws IllegalArgumentException if the path is invalid
         * @throws RuntimeException         if required parent directories cannot be created
         */
        public Builder setPath(String path) {
            if(path == null || path.isEmpty()) {
                throw new IllegalArgumentException("Path cannot be null or empty");
            }
            if(path.endsWith("/")) {
                throw new IllegalArgumentException("Path cannot be a directory, must be a file!");
            }

            if(path.contains("/")){
                String dir = path.substring(0, path.lastIndexOf("/"));
                File directory = new File(dir);
                if(!directory.exists()){
                    if(!directory.mkdirs()){
                        throw new RuntimeException("Could not create directory: " + dir);
                    }
                }
            }

            this.path = path;
            return this;
        }

        /**
         * Controls whether samples are appended to an existing file ({@code true}) or the file is
         * overwritten on each run ({@code false}).
         *
         * @param append {@code true} to append, {@code false} to overwrite
         * @return this builder
         */
        public Builder setAppend(boolean append) {
            this.append = append;
            return this;
        }

        /**
         * Builds and returns a new {@link LocalTextStorage} with the configured settings.
         *
         * @return a new {@link LocalTextStorage}
         */
        public LocalTextStorage build() {
            return new LocalTextStorage(formatter, path, append);
        }
    }

    /**
     * Constructor for the LocalTextStorage<br>
     * @param formatter Formatter to format the samples
     * @param path Path to the file where the samples will be stored
     * @param append If true, the samples will be appended to the file, if false, the file will be overwritten
     */
    private LocalTextStorage(IdentifiedNodeSampleFormatter formatter, String path, boolean append) {
        this.formatter = formatter;
        try{
            this.writer = new FileWriter(path, append);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }





    /**
     * Formats and appends a single host's sample to the output file.
     *
     * @param host       the node identifier string
     * @param nodeSample the metrics snapshot to persist
     * @throws RuntimeException if writing to the file fails
     */
    @Override
    public void store(String host, NodeSample nodeSample) {
        try {
            writer.write(this.formatter.format(host, nodeSample));
            writer.write(System.lineSeparator()); // Add a new line after each sample
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Formats and appends a map of per-node samples to the output file in a single write.
     *
     * @param samples a map from node identifier to its collected {@link NodeSample}
     * @throws RuntimeException if writing to the file fails
     */
    @Override
    public void store(Map<String, NodeSample> samples) {
        try {
            writer.write(this.formatter.format(samples));
            writer.write(System.lineSeparator()); // Add a new line after each sample
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
