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

    public static class Builder extends ExporterBuilder<Builder>{
        Properties properties = new Properties();

        public Builder(String exporterName) {
            super(exporterName);
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder setHost(String host){
            properties.setProperty(HOST, host);
            return this;
        }

        public Builder setPort(int port){
            properties.setProperty(PORT, Integer.toString(port));
            return this;
        }

        public Builder setTerminator(String terminator){
            properties.setProperty(TERMINATOR, terminator);
            return this;
        }

        public Builder setFormatter(NodeSampleFormatter formatter){
            properties.setProperty(FORMATTER, formatter.getFormatterName());
            return this;
        }

        public Builder setInterval(double interval){
            properties.setProperty(INTERVAL, Double.toString(interval));
            return this;
        }


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
