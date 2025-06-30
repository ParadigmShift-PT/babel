package pt.unl.fct.di.novasys.babel.metrics.utils;

import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

public  class ClientSocketCommons {
    public static final String HOST = "HOST";
    public static final String PORT = "PORT";

    public static class IncorrectHostPortException extends Exception {
        public IncorrectHostPortException(String message) {
            super(message);
        }
    }

    public static class HostPort {
        public String host;
        public int port;

        public HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    /**
     * Parses the host ("HOST") and port ("PORT") from the given properties argument.
     * @param props properties containing the host and port
     * @return a HostPort object containing the parsed host and port
     * @throws IncorrectHostPortException if the host or port is not found, or if the port is not a number
     */
    public static HostPort parseHostPort(Properties props) throws IncorrectHostPortException {
        String host = props.getProperty(HOST);
        if (host == null) {
            throw new IncorrectHostPortException("Host not found in properties");
        }

        if (host.isEmpty()) {
            throw new IncorrectHostPortException("Host is empty");
        }

        String portStr = props.getProperty(PORT);
        if (portStr == null) {
            throw new IncorrectHostPortException("Port not found in properties");
        }

        int port;

        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new IncorrectHostPortException("Port is not a number");
        }

        if (port < 0 || port > 65535) {
            throw new IncorrectHostPortException("Port is out of range");
        }

        return new HostPort(host, port);
    }


    public static Socket connect(String ip, int port){
        Socket socket = null;
        try {
            socket = new Socket(ip, port);
        } catch (IOException e) {
            System.err.println("Error connecting to socket, will try again");
        }
        return socket;
    }
}
