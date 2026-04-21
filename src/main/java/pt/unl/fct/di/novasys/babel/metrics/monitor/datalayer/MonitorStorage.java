package pt.unl.fct.di.novasys.babel.metrics.monitor.datalayer;

import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.babel.metrics.formatting.JSONFormatter;
import pt.unl.fct.di.novasys.babel.metrics.utils.ClientSocketCommons;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A {@link Storage} implementation that forwards aggregated samples to a remote host over a TCP socket,
 * serialising each entry as a JSON string (newline-terminated) via a background writer thread.
 * The writer thread is started lazily on the first {@link #store} call and reconnects automatically on errors.
 */
public class MonitorStorage implements Storage{

    LinkedBlockingQueue<Map<String, NodeSample>> queue;
    ClientSocketCommons.HostPort hostPort;
    Thread thread;
    boolean threadRunning = false;

    /**
     * Constructor for the MonitorStorage
     * @param props Properties containing at least HOST and PORT
     */
    public MonitorStorage(Properties props) {
        this.queue = new LinkedBlockingQueue<>();
        this.thread = new Thread(this::sendSamplesSocket);
        try {
            this.hostPort = ClientSocketCommons.parseHostPort(props);
        } catch (ClientSocketCommons.IncorrectHostPortException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendSamplesSocket(){
        JSONFormatter jsonFormatter = new JSONFormatter();
        Socket socket;

        socket = ClientSocketCommons.connect(this.hostPort.host,this.hostPort.port);

        OutputStreamWriter osw;
        try {
            osw = new OutputStreamWriter(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while(true){
            try {
                osw.write(jsonFormatter.format(queue.take()));
                osw.write("\n");
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

    /**
     * Enqueues a single host's sample for transmission, wrapping it in a singleton map.
     *
     * @param host       the node identifier string
     * @param nodeSample the metrics snapshot to send
     */
    @Override
    public void store(String host, NodeSample nodeSample) {
        Map<String, NodeSample> samples = Map.of(host, nodeSample);
        this.store(samples);
    }

    /**
     * Enqueues a map of per-node samples for transmission, starting the background writer thread
     * if it has not yet been started.
     *
     * @param samples a map from node identifier to its collected {@link NodeSample}
     */
    @Override
    public void store(Map<String, NodeSample> samples) {
        if(!threadRunning){
            thread.start();
            threadRunning = true;
        }
        queue.add(samples);
    }

}
