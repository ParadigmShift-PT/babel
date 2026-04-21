package pt.unl.fct.di.novasys.babel.metrics.utils;

import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

/**
 * Shared utilities for socket-based metric exporters: parsing host/port from properties
 * and establishing TCP connections with basic error handling.
 */
public  class ClientSocketCommons {
    public static final String HOST = "HOST";
    public static final String PORT = "PORT";

    /**
     * Thrown when the {@code HOST} or {@code PORT} property is missing, empty, or not a valid port number.
     */
    public static class IncorrectHostPortException extends Exception {
        /**
         * Constructs an {@code IncorrectHostPortException} with the given detail message.
         *
         * @param message the detail message
         */
        public IncorrectHostPortException(String message) {
            super(message);
        }
    }

    /**
     * Simple holder for a resolved hostname and port number.
     */
    public static class HostPort {
        public String host;
        public int port;

        /**
         * Constructs a {@code HostPort} with the given host string and port number.
         *
         * @param host the hostname or IP address
         * @param port the port number
         */
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


    /**
     * Opens a TCP socket connection to the given host and port, printing an error message to stderr
     * if the initial connection attempt fails (does not retry).
     *
     * @param ip   the hostname or IP address to connect to
     * @param port the port number
     * @return the connected {@link Socket}, or {@code null} if the connection failed
     */
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
