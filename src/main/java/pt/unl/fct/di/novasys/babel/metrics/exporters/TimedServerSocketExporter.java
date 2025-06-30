package pt.unl.fct.di.novasys.babel.metrics.exporters;

import pt.unl.fct.di.novasys.babel.metrics.formatting.JSONFormatter;
import pt.unl.fct.di.novasys.babel.metrics.formatting.NodeSampleFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;


/**
 * Exports metrics to a peer connected via a regular socket<br>
 * On connection the peer sends a message to the exporter indicating how often (times per minute) it wants to receive metrics<br>
 * The exporter then sends metrics at the specified rate<br>
 * If the connection is lost, it will be available for a new connection
 */
public class TimedServerSocketExporter extends ThreadedExporter{

    public static final String PORT = "PORT";
    public static final String TERMINATOR = "TERMINATOR";
    public static final String FORMATTER = "FORMATTER";

    NodeSampleFormatter formatter;

    /**
      * Time between sending metrics in milliseconds, calculated from the times per minute received from the peer
     */
    int sleepTime;

    public static class Builder extends ExporterBuilder<Builder>{
        Properties properties = new Properties();
        public Builder(String exporterName) {
            super(exporterName);
        }

        @Override
        public Builder self() {
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



        @Override
        public TimedServerSocketExporter build() {
            exporterConfigs(properties);
            return new TimedServerSocketExporter(this);
        }
    }


    public TimedServerSocketExporter(Builder exporterBuilder) {
        super(exporterBuilder);
        this.formatter = getFormatterOrDefault(getProperty(FORMATTER), new JSONFormatter());
    }

    private OutputStreamWriter waitForConnect(){
        try(ServerSocket serverSocket = new ServerSocket(Integer.parseInt(getProperty(PORT)))){
            Socket s = serverSocket.accept();
            serverSocket.close();
            BufferedReader in = new BufferedReader(new java.io.InputStreamReader(s.getInputStream()));
            String line = in.readLine();
            int timesPerMinute = Integer.parseInt(line);
            sleepTime = 60000 / timesPerMinute;
            return new OutputStreamWriter(s.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
            OutputStreamWriter out;
        try{
            out = waitForConnect();
            System.err.println("Connected to socket - 1st time");
            while(true){
                Thread.sleep(sleepTime);

                //If the exporter is disabled, stop the thread
                if(isDisabled()) return;

                try{
                    out.write(this.formatter.format(collectAllMetrics()));
                    out.write(this.getProperty(TERMINATOR));
                    out.flush();
                }catch (IOException e){
                    System.err.println("Error connecting to socket, will try again");
                    out = waitForConnect();
                    System.err.println("Connected to socket");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Properties loadDefaults() {
        Properties defaults = new Properties();
        defaults.setProperty(PORT, "9101");
        defaults.setProperty(TERMINATOR, "\n");
        defaults.setProperty(FORMATTER, JSONFormatter.NAME);

        return defaults;
    }
}
