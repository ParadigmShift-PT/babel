package pt.unl.fct.di.novasys.babel.metrics.exporters;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.metrics.MultiRegistryEpochSample;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;
import pt.unl.fct.di.novasys.babel.metrics.formatting.PrometheusFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.Properties;

public class PrometheusHTTPExporter extends Exporter{

    private static final Logger logger = LogManager.getLogger(PrometheusHTTPExporter.class);

    public final static String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";

    private static final String CONFIG_DEFAULT_PATH = System.getProperty("user.dir") + "configs/exporters/";

    private final static String CONFIG_FILE_FORMAT = ".conf";


    public PrometheusHTTPExporter(String exporterName) {
        super(exporterName);
    }

    @Override
    public Properties loadDefaults() {
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty("PORT", "9090");
        return defaultProperties;
    }


    public void run() {
        try {
            int port = Integer.parseInt(getProperty("PORT"));
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/metrics", new PrometheusMetricsHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            logger.debug("Prometheus HTTP Exporter started on port {}", port);
        } catch (IOException e) {
            logger.error("Error starting Prometheus HTTP Exporter", e);
        }
    }


    class PrometheusMetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                MultiRegistryEpochSample mres = collectAllMetrics();
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

