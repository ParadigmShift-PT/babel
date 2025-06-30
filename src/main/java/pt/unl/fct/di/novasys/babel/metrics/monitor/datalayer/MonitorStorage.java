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

    @Override
    public void store(String host, NodeSample nodeSample) {
        Map<String, NodeSample> samples = Map.of(host, nodeSample);
        this.store(samples);
    }

    @Override
    public void store(Map<String, NodeSample> samples) {
        if(!threadRunning){
            thread.start();
            threadRunning = true;
        }
        queue.add(samples);
    }

}
