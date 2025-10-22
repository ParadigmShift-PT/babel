package pt.unl.fct.di.novasys.babel.metrics.monitor.datalayer;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.babel.metrics.formatting.SimpleFormatter;
import pt.unl.fct.di.novasys.babel.metrics.formatting.IdentifiedNodeSampleFormatter;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.Map;


public class LocalTextStorage implements Storage{

    private final IdentifiedNodeSampleFormatter formatter;
    private final FileWriter writer;

    public static class Builder {
        private IdentifiedNodeSampleFormatter formatter = new SimpleFormatter();
        private String path = "MonitorTextStorage.txt";
        private boolean append = true;


        public Builder setFormatter(IdentifiedNodeSampleFormatter formatter) {
            this.formatter = formatter;
            return this;
        }

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

        public Builder setAppend(boolean append) {
            this.append = append;
            return this;
        }


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
