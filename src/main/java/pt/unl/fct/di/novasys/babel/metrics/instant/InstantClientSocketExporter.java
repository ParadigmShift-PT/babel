package pt.unl.fct.di.novasys.babel.metrics.instant;

import pt.unl.fct.di.novasys.babel.metrics.MetricSample;
import pt.unl.fct.di.novasys.babel.metrics.exporters.ThreadedExporter;
import pt.unl.fct.di.novasys.babel.metrics.formatting.JSONFormatter;
import pt.unl.fct.di.novasys.babel.metrics.formatting.MetricSampleFormatter;
import pt.unl.fct.di.novasys.babel.metrics.utils.ClientSocketCommons;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An {@link InstantExporter} that immediately forwards each {@link MetricSample} to a remote
 * host over a TCP socket, serialising it with a configurable {@link MetricSampleFormatter}
 * (default: JSON) and appending a configurable terminator string.
 */
public class InstantClientSocketExporter implements InstantExporter {
    public static final String HOST = "HOST";
    public static final String PORT = "PORT";
    public static final String TERMINATOR = "TERMINATOR";

    BlockingQueue<MetricSample> toExport;
    Properties properties;
    MetricSampleFormatter formatter;
    Socket socket;
    String exporterName;

    /**
     * Builds a new InstantClientSocketExporter<br>
     * Mandatory Properties:
     * <ul>
     *     <li>HOST: Address of host to connect to send metric</li>
     *     <li>PORT: Port of host to connect to send metric</li>
     *     <li>TERMINATOR: string that marks the end of a message (Default: \n)</li>
     * </ul>
     * The formatter is set by default to JSONFormatter, to specify other formatter use setFormatter call
     * @param props Properties containing at least HOST and PORT
     */
    public InstantClientSocketExporter(Properties props, String exporterName){
        this.properties = this.loadDefaults();
        this.properties.putAll(props);
        this.formatter = new JSONFormatter();
        this.exporterName = exporterName;
        this.toExport = new LinkedBlockingQueue<>();

    }

    /**
     * {@inheritDoc}
     */
    public String getExporterName() {
        return exporterName;
    }


    /**
     * Returns the default properties for this exporter, setting the message terminator to {@code \n}.
     *
     * @return a {@link Properties} object containing default values
     */
    protected Properties loadDefaults() {
       Properties defaults = new Properties();
       defaults.setProperty(TERMINATOR, "\n");
       return defaults;
    }

    /**
     * Replaces the formatter used to serialise metric samples before writing to the socket.
     *
     * @param formatter the new formatter to use
     */
    public void setFormatter(MetricSampleFormatter formatter){
        this.formatter = formatter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMetricSample(MetricSample ms) {
        toExport.add(ms);
    }

    /**
     * Connects to the configured remote host and continuously drains the internal queue,
     * writing each serialised sample to the socket followed by the configured terminator.
     * Reconnects automatically on I/O errors.
     */
    public void run() {
        ClientSocketCommons.HostPort hostPort;
        try {
             hostPort = ClientSocketCommons.parseHostPort(this.properties);
        } catch (ClientSocketCommons.IncorrectHostPortException e) {
            throw new RuntimeException(e);
        }

        socket = ClientSocketCommons.connect(hostPort.host,hostPort.port);

        OutputStreamWriter osw = null;
        try {
            osw = new OutputStreamWriter(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }



        while(true){
            try {
                MetricSample s = this.toExport.take();
                osw.write(this.formatter.format(s));
                osw.write(this.properties.getProperty(TERMINATOR));
                osw.flush();
            }catch (IOException | NullPointerException e){
                socket = ClientSocketCommons.connect(hostPort.host, hostPort.port);
                try {
                    osw = new OutputStreamWriter(socket.getOutputStream());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

    }




}
