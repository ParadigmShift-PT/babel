package pt.unl.fct.di.novasys.babel.metrics.exporters;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;
import pt.unl.fct.di.novasys.babel.metrics.formatting.PrometheusFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.Properties;

/**
 * Threaded exporter that exposes collected metrics over HTTP in Prometheus text format (0.0.4).
 * It starts an embedded {@link HttpServer} on the configured port and serves a {@code /metrics}
 * endpoint that returns a fresh snapshot on every scrape request.
 */
public class PrometheusHTTPExporter extends ThreadedExporter{

    private static final Logger logger = LogManager.getLogger(PrometheusHTTPExporter.class);

    public final static String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";

    public final static String PORT = "PORT";

    /**
     * Builder for {@link PrometheusHTTPExporter}.
     */
    public static class Builder extends ExporterBuilder<Builder> {
        Properties properties = new Properties();

        /**
         * Creates a builder for a {@link PrometheusHTTPExporter} with the given name.
         *
         * @param exporterName logical name for the exporter
         */
        public  Builder(String exporterName) {
            super(exporterName);
        }

        @Override
        public Builder self() {
            return this;
        }

        /**
         * Sets the TCP port on which the HTTP metrics endpoint will listen.
         *
         * @param port the port number to bind to
         * @return this builder
         */
        public Builder setPort(int port){
            properties.setProperty(PORT, Integer.toString(port));
            return this;
        }

        /**
         * Builds and returns a configured {@link PrometheusHTTPExporter}.
         *
         * @return a new {@code PrometheusHTTPExporter}
         */
        @Override
        public PrometheusHTTPExporter build() {
            exporterConfigs(properties);
            return new PrometheusHTTPExporter(this);
        }
    }

    private PrometheusHTTPExporter(Builder builder) {
        super(builder);
    }

    @Override
    public Properties loadDefaults() {
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty(PORT, "9101");
        return defaultProperties;
    }


    /**
     * Starts the embedded HTTP server on the configured port, serves scrape requests until
     * the exporter is disabled, then stops the server cleanly.
     */
    public void run() {
        try {
            int port = Integer.parseInt(getProperty(PORT));
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/metrics", new PrometheusMetricsHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            logger.debug("Prometheus HTTP Exporter started on port {}", port);

            //Main thread stays alive until the exporter is disabled, to work as a daemon thread
            while(isEnabled()){
                Thread.sleep(1000);
            }
            server.stop(0);
        } catch (IOException e) {
            logger.error("Error starting Prometheus HTTP Exporter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * HTTP handler that collects a fresh metric snapshot and writes it in Prometheus text format
     * as the response body of each {@code /metrics} request.
     */
    class PrometheusMetricsHandler implements HttpHandler {
        /**
         * Handles an incoming HTTP request by collecting metrics, formatting them with
         * {@link PrometheusFormatter}, and writing the result to the response.
         * Returns HTTP 500 if the requested protocol registry is not found.
         *
         * @param t the HTTP exchange representing the incoming request and outgoing response
         * @throws IOException if an I/O error occurs while writing the response
         */
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                NodeSample mres = collectMetrics();
                PrometheusFormatter formatter = new PrometheusFormatter();
                String collect = formatter.format(mres);

                t.getResponseHeaders().set("Content-Type", CONTENT_TYPE_004);
                t.sendResponseHeaders(HttpURLConnection.HTTP_OK, collect.length());

                OutputStream os = t.getResponseBody();
                os.write(collect.getBytes());
                os.close();
            } catch (NoSuchProtocolRegistry e) {
                t.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, 0);
            }
                t.close();
        }
    }


}

