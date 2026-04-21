package pt.unl.fct.di.novasys.babel.metrics.exporters;

import pt.unl.fct.di.novasys.babel.metrics.formatting.NodeSampleFormatter;
import pt.unl.fct.di.novasys.babel.metrics.formatting.JSONFormatter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Properties;

/**
 * Export metrics to a TCP server socket at regular intervals<br>
 * If the connection is lost, it will try to reconnect <br>
 * The properties HOST and PORT must be set
 * <br>
 * The metrics are sent in the specified format, with the specified terminator at the end
 */
public class TimedSocketExporter extends ThreadedExporter{

    public static final String HOST = "HOST";
    public static final String PORT = "PORT";
    public static final String TERMINATOR = "TERMINATOR";
    public static final String FORMATTER = "FORMATTER";
    public static final String INTERVAL = "INTERVAL";


    public static final long S_TO_MS = 1000;

    NodeSampleFormatter formatter;

    Socket socket = null;

    /**
     * Builder for {@link TimedSocketExporter}.
     */
    public static class Builder extends ExporterBuilder<Builder>{
        Properties properties = new Properties();

        /**
         * Creates a builder for a {@link TimedSocketExporter} with the given name.
         *
         * @param exporterName logical name for the exporter
         */
        public Builder(String exporterName) {
            super(exporterName);
        }

        /**
         * Returns this builder (required by the covariant builder pattern).
         *
         * @return this builder
         */
        @Override
        public Builder self() {
            return this;
        }

        /**
         * Sets the hostname or IP address of the remote TCP server to connect to.
         *
         * @param host the remote host
         * @return this builder
         */
        public Builder setHost(String host){
            properties.setProperty(HOST, host);
            return this;
        }

        /**
         * Sets the TCP port of the remote server.
         *
         * @param port the remote port number
         * @return this builder
         */
        public Builder setPort(int port){
            properties.setProperty(PORT, Integer.toString(port));
            return this;
        }

        /**
         * Sets the string appended after each metric payload to delimit messages on the stream.
         *
         * @param terminator the message terminator (e.g. {@code "\n"})
         * @return this builder
         */
        public Builder setTerminator(String terminator){
            properties.setProperty(TERMINATOR, terminator);
            return this;
        }

        /**
         * Sets the formatter used to serialise each metric snapshot before sending.
         *
         * @param formatter the {@link NodeSampleFormatter} to use
         * @return this builder
         */
        public Builder setFormatter(NodeSampleFormatter formatter){
            properties.setProperty(FORMATTER, formatter.getFormatterName());
            return this;
        }

        /**
         * Sets the export interval in seconds (fractional values are allowed).
         *
         * @param interval seconds between successive metric sends
         * @return this builder
         */
        public Builder setInterval(double interval){
            properties.setProperty(INTERVAL, Double.toString(interval));
            return this;
        }

        /**
         * Builds and returns a configured {@link TimedSocketExporter}.
         *
         * @return a new {@code TimedSocketExporter}
         */
        @Override
        public TimedSocketExporter build() {
            exporterConfigs(properties);
            return new TimedSocketExporter(this);
        }
    }


    private TimedSocketExporter(Builder exporterBuilder) {
        super(exporterBuilder);
        this.formatter = getFormatterOrDefault(getProperty(FORMATTER), new JSONFormatter());
    }

    /**
     * Checks if any of the properties are empty, if so returns false
     * @param properties Properties to check
     * @return True if all properties are not empty, false otherwise
     */
    private boolean checkProperties(String... properties){
        for(String property : properties){
            if(this.getProperty(property).isEmpty()){
                return false;
            }
        }
        return true;
    }

    /**
     * Returns default configuration: interval 10 s, empty host and port (must be provided),
     * newline terminator, and JSON formatter.
     *
     * @return properties containing default values for all configuration keys
     */
    @Override
    public Properties loadDefaults() {
        Properties defaults = new Properties();
        defaults.setProperty(INTERVAL, "10");
        defaults.setProperty(HOST, "");
            defaults.setProperty(PORT, "");
            defaults.setProperty(TERMINATOR, "\n");
            defaults.setProperty(FORMATTER, "JSONFormatter");
        return defaults;
    }


    private void connect(String ip, int port){
        try {
            socket = new Socket(ip, port);
        } catch (IOException e) {
            System.err.println("Error connecting to socket, will try again");
        }
    }


    /**
     * Connects to the configured host and port, then enters a loop that collects all metrics,
     * formats the snapshot, and sends it over the socket at the configured interval.
     * Automatically reconnects on I/O failure. Exits when the exporter is disabled.
     *
     * @throws IllegalArgumentException if {@code HOST} or {@code PORT} are not configured
     */
    @Override
    public void run() {

        if(!this.checkProperties(HOST, PORT)){
            throw new IllegalArgumentException("HOST and PORT must be set");
        }

        String ip = this.getProperty(HOST);
        int port = Integer.parseInt(this.getProperty(PORT));

        connect(ip, port);

        OutputStreamWriter osw;


        long sleep_ms = Math.round(Double.parseDouble(this.getProperty(INTERVAL)) * S_TO_MS);
        while(true){

            //If the exporter is disabled, stop the thread
            if(isDisabled()) return;

            try{
                osw = new OutputStreamWriter(socket.getOutputStream());
                osw.write(this.formatter.format(collectAllMetrics()));
                osw.write(this.getProperty(TERMINATOR));
                osw.flush();
            } catch (IOException | NullPointerException e) {
                connect(ip, port);

            }
            try {
                Thread.sleep(sleep_ms);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


    }
}
