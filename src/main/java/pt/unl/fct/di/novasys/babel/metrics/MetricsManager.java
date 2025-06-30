package pt.unl.fct.di.novasys.babel.metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.DuplicatedProtocolMetric;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoProcfsException;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.OSMetricsConfigException;
import pt.unl.fct.di.novasys.babel.metrics.exporters.*;
import pt.unl.fct.di.novasys.babel.metrics.generic.os.OSMetrics;
import pt.unl.fct.di.novasys.babel.metrics.instant.InstantExporter;
import pt.unl.fct.di.novasys.babel.metrics.monitor.Aggregation;
import pt.unl.fct.di.novasys.babel.metrics.monitor.Monitor;
import pt.unl.fct.di.novasys.babel.metrics.monitor.SimpleMonitor;
import pt.unl.fct.di.novasys.babel.metrics.monitor.datalayer.MonitorStorage;
import pt.unl.fct.di.novasys.babel.metrics.monitor.datalayer.Storage;
import pt.unl.fct.di.novasys.babel.metrics.utils.JSONParser;
import pt.unl.fct.di.novasys.network.data.Host;


import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsManager {

    private static final String CURRENT_WORKING_DIR = System.getProperty("user.dir");

    /**
     * Used to identify that the metrics are global (aggregate of all protocols)
     */
    public static final String GLOBAL_HOST_IDENTIFIER = "GLOBAL";

    public static final short OS_METRIC_PROTOCOL_ID = -1;
    public static final String OS_PROTO_NAME  = "OS";

    private static final Logger logger = LogManager.getLogger(MetricsManager.class);
    private final ConcurrentHashMap<Short, ProtocolMetrics> protocolMetricsMap = new ConcurrentHashMap<>();

    OSMetrics osMetrics;
    /**
     * A set of exporters which are to be assigned to their thread
     */
    List<ThreadedExporter> threadedExporters = new ArrayList<>();

    List<InstantExporter> instantExporters = new ArrayList<>();

    /**
     * Set of both Threaded and Protocol Exporters
     */
    List<Exporter> exporters = new ArrayList<>();

    private boolean disabled;

    private SimpleMonitor simpleMonitor;


    /**
     * For each protocol,there is a main exporter, which is able to reset and advance the epoch of the protocol metrics
     */
    Map<Short, Exporter> protocolExporters = new HashMap<>();

    private static MetricsManager system;

    public static synchronized MetricsManager getInstance() {
        if (system == null)
            system = new MetricsManager();
        return system;
    }


    private boolean started;

    private MetricsManager() {
        started = false;
        this.disabled = false;
    }

    /**
     * Registers the exporters to be used by the MetricsManager <br>
     * @param exporters the exporters to be used
     **/
    public void registerExporters(ThreadedExporter...exporters){
        if(started){
            throw new IllegalStateException("Can't register threaded exporters after starting the MetricsManager");
        }

        if(exporters.length == 0){
            throw new IllegalArgumentException("No exporters provided");
        }

        this.threadedExporters.addAll(Arrays.asList(exporters));
        this.exporters.addAll(Arrays.asList(exporters));
    }

    /**
     * Registers an InstantExporter to be used by the MetricsManager
     */
    public void registerExporters(InstantExporter exporter){
        if(started){
            throw new IllegalStateException("Can't register threaded exporters after starting the MetricsManager");
        }

        this.instantExporters.add(exporter);
    }

    /**
     * Registers the Protocol Exporters which are to be used<br>
     * While these are the user's responsibility to start, they are registered here, so they can be stopped by the MetricsManager if disable() is called
     * @param exporters the exporters to be used
     */
    public void registerExporters(ProtocolExporter ...exporters){
        if(exporters.length == 0){
            throw new IllegalArgumentException("No exporters provided");
        }

        this.exporters.addAll(Arrays.asList(exporters));
    }


    public void registerExporters(String configPath){
        registerExportersUsingConfig(configPath);
    }






    /**
     * Registers the exporters to be used by the MetricsManager using the <br>
     *
     * */
    @SuppressWarnings("unchecked")
    private void registerExportersUsingConfig(String configPath){
            List<Object> parsedJson = (List<Object>) JSONParser.parseJsonFile(configPath);
            //System.out.println(parsedJson);
            for(Object exporter : parsedJson){
                    String exporterClass = (String) ((Map<String, Object>) exporter).get("type");
                    String exporterName = (String) ((Map<String, Object>) exporter).get("name");
                    String exporterConfigPath = (String) ((Map<String, Object>) exporter).get("exporterConfigs");
                    String exporterCollectOptionsPath = (String) ((Map<String, Object>) exporter).get("exporterCollectOptions");

                    ThreadedExporter ex = null;
                    GenericProtocol exporterProtocol = null;
                    switch (exporterClass){
//                        case "TimedLogExporter":
//                            ex = new TimedLogExporter.Builder(exporterName)
//                                    .exporterConfigPath(exporterConfigPath)
//                                    .exporterCollectOptionsPath(exporterCollectOptionsPath)
//                                    .build();
//                            break;
                        //TODO: CHECK WORKING AND REMOVE THIS CASE AS DID W/ TIMELOGEXPORTER
                        case "PrometheusHTTPExporter":
                            ex = new PrometheusHTTPExporter.Builder(exporterName).
                                    exporterConfigPath(exporterConfigPath)
                                    .exporterCollectOptionsPath(exporterCollectOptionsPath)
                                    .build();
                            break;
                        default:
                            try {
                                Class<?> exporterClassType;
                                try{
                                    exporterClassType = Class.forName(exporterClass);
                                } catch (ClassNotFoundException e) {
                                    throw new RuntimeException("Exporter class not found!! " + e);
                                }
                                //Check if class is a superclass of Threaded exporter, if yes, it must implement a Builder class
                                if (ThreadedExporter.class.isAssignableFrom(exporterClassType)) {
                                        Class<?> exporterBuilder;
                                    try {
                                        exporterBuilder = Class.forName(exporterClass + "$Builder");
                                    } catch (ClassNotFoundException e) {
                                        throw new RuntimeException("Exporter class must have a Builder class!!" + e);
                                    }
                                    try {
                                        Object builderInstance = exporterBuilder.getDeclaredConstructor(String.class).newInstance(exporterName);
                                        exporterBuilder.getMethod("exporterConfigPath", String.class).invoke(builderInstance, exporterConfigPath);
                                        exporterBuilder.getMethod("exporterCollectOptionsPath", String.class).invoke(builderInstance, exporterCollectOptionsPath);
                                        ex = (ThreadedExporter) exporterBuilder.getMethod("build").invoke(builderInstance);
                                    }catch (NoSuchMethodException | InstantiationException | IllegalAccessException e){
                                        throw new RuntimeException("Builder method not found, wrong arguments or wrong return type!!" + e);
                                    }catch (InvocationTargetException e){
                                        throw new RuntimeException("Builder threw an exception!! " + e.getTargetException().getMessage());
                                    }
                                } else {
                                    if (GenericProtocol.class.isAssignableFrom(exporterClassType)) {
                                        ProtocolExporter protocolExporter = new ProtocolExporter
                                                .Builder(exporterName)
                                                .exporterConfigPath(exporterConfigPath)
                                                .exporterCollectOptionsPath(exporterCollectOptionsPath)
                                                .build();
                                        try{
                                            Object protocolInstance = exporterClassType.getDeclaredConstructor(ProtocolExporter.class).newInstance(protocolExporter);
                                            exporterProtocol = (GenericProtocol) protocolInstance;
                                        }catch (NoSuchMethodException e){
                                            throw new RuntimeException("Exporter protocol must have a constructor that receives a ProtocolExporter object!!");
                                        }
                                    }
                                }
                            }
                            catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }

                    }

                    if(ex != null){
                        threadedExporters.add(ex);
                    }else{
                        if(exporterProtocol != null){
                            try {
                                Babel.getInstance().registerProtocol(exporterProtocol);
                            } catch (ProtocolAlreadyExistsException e) {
                                throw new RuntimeException(e);
                            }
                            Properties exporterProperties = new Properties();
                            if (exporterConfigPath != null) {
                                try {
                                    Reader reader = new FileReader(exporterConfigPath);
                                    exporterProperties.load(reader);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            try {
                                exporterProtocol.init(exporterProperties);
                            } catch (Exception e) {
                                throw new RuntimeException("Error initializing protocol " + exporterProtocol.getProtoName() + " " + e);
                        }
                    }


            }
            }

    }


    public synchronized void registerOSMetrics(OSMetrics.MetricType ...metricTypes) throws NoProcfsException, OSMetricsConfigException, DuplicatedProtocolMetric {
        if (this.osMetrics == null) {
            this.osMetrics = new OSMetrics();
        }

        for(OSMetrics.MetricType mt : metricTypes){
            registerMetric(osMetrics.getOSMetric(mt, osMetrics), OS_METRIC_PROTOCOL_ID, OS_PROTO_NAME);
        }

    }

    public synchronized void registerOSMetricCategory(OSMetrics.MetricCategory ...metricCategories) throws NoProcfsException, OSMetricsConfigException, DuplicatedProtocolMetric {
        if (this.osMetrics == null) {
            this.osMetrics = new OSMetrics();
        }
        Set<OSMetrics.MetricType> mts = osMetrics.getMetricsFromCategories(metricCategories);

        registerOSMetrics(mts.toArray(new OSMetrics.MetricType[0]));
    }

    /**
     * This method will start all (threaded) exporters registered with the MetricsManager in their own threads<br>
     * After this method is called, no more exporters can be registered<br>
     */
    public synchronized void start() {
        //Start exporter threads
        if(started)
            return;

        for(ThreadedExporter ex : threadedExporters){
//            for(short protoId : ex.getExporterCollectOptions().getProtocolsToCollect()){
//                this.protocolExporters.put(protoId, ex);
//            }
            new Thread(ex, ex.getExporterName()).start();
        }
        for(InstantExporter ex : instantExporters){
            new Thread(ex, ex.getExporterName()).start();
        }

        started = true;
    }

    /**
     * Disables all Exporters and metrics<br>
     * All calls to metrics will be either ignored or return dummy references<br>
     * Eventually stops all exporters<br>
     * Any collections happening by still running exporters will succeed, but the data will be meaningless
     * */
    public synchronized void disable(){
        this.disabled = true;

        for(Exporter ex : exporters){
            ex.disable();
        }
        for(ProtocolMetrics mr : protocolMetricsMap.values()){
            mr.disable();
        }

    }


    public synchronized void registerMetric(Metric m, short protocolID, String protoName) throws DuplicatedProtocolMetric {

        //If metrics have been disabled, we don't register the metric and set it to disabled
        if(this.disabled){
            m.disable();
            return;
        }

        if(protocolMetricsMap.containsKey(protocolID)){
            protocolMetricsMap.get(protocolID).register(m);
        }else{
            ProtocolMetrics pm = new ProtocolMetrics(protocolID, protoName);
            pm.register(m);
            protocolMetricsMap.put(protocolID, pm);
        }
    }


    /**
     * Collects metrics for all registered protocols, including OS metrics if enabled<br>
     * @return {@link NodeSample} containing the metrics for all registered protocols
     */
    public NodeSample collectMetricsAllProtocols(ExporterCollectOptions exporterCollectOptions)  {
        NodeSample nodeSample = new NodeSample();
        for (Short protocolId : protocolMetricsMap.keySet()) {
            if(protocolId == OS_METRIC_PROTOCOL_ID && !exporterCollectOptions.isCollectOSMetrics()){
                continue;
            }
            nodeSample.addProtocolSample(protocolId, collectMetricForProtocol(protocolId, exporterCollectOptions));
        }
        return nodeSample;
    }


    public NodeSample collectMetricsProtocols(boolean collectOSMetrics, ExporterCollectOptions exporterCollectOptions, short ...protocolIds) throws NoSuchProtocolRegistry{
        NodeSample nodeSample = new NodeSample();

        if(collectOSMetrics && osMetrics != null){
            ProtocolSample osSample = collectMetricForProtocol(OS_METRIC_PROTOCOL_ID, exporterCollectOptions);
            nodeSample.addProtocolSample(OS_METRIC_PROTOCOL_ID, osSample);
        }

        for (short protoId : protocolIds) {
            ProtocolSample protocolSample = collectMetricForProtocol(protoId, exporterCollectOptions);
            nodeSample.addProtocolSample(protoId, protocolSample);
        }

        return nodeSample;
    }





    private ProtocolSample collectMetricForProtocol(short protocolId, ExporterCollectOptions exporterCollectOptions) throws NoSuchProtocolRegistry {

        ProtocolCollectOptions protocolCollectOptions = exporterCollectOptions.getProtocolCollectOptions(protocolId);

        if(protocolCollectOptions == null){
            protocolCollectOptions = new ProtocolCollectOptions();
        }

        ProtocolMetrics protocolMetrics = protocolMetricsMap.get(protocolId);
        if(protocolMetrics == null){
            throw new NoSuchProtocolRegistry(protocolId);
        }


        return protocolMetrics.collect(protocolCollectOptions);
    }


    public String getProtoNameById(short protocolID){
        if(protocolID == OS_METRIC_PROTOCOL_ID){
            return OS_PROTO_NAME;
        }

        return Babel.getInstance().getProtoNameById(protocolID);
    }

    public void startMonitorExporter(Host self, Host monitor, long interval, ExporterCollectOptions eco) {
        MonitorExporter exporter = new MonitorExporter(self, monitor, interval, eco);
        try {
            Babel.getInstance().registerProtocol(exporter);
            exporter.init(new Properties());
        } catch (ProtocolAlreadyExistsException | HandlerRegistrationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Starts the monitor, which will collect metrics from the protocols and store them in the monitor storage<br>
     * The monitor storage is a docker stack, comprised of a time-series database (InfluxDB) and a visualization tool (Grafana)
     * @param myself the host that is starting the monitor
     * @param props the properties for the monitor storage, must contain the following keys:
     *<ul>
     *              <li>HOST: the host where the monitor storage is running</li>
     *              <li> PORT: the port where the monitor storage is running</li>
     *</ul>
     */
    public Monitor startMonitor(Host myself, Properties props){
        Storage monitorStorage = new MonitorStorage(props);
        return startMonitor(myself, monitorStorage);
    }

    public Monitor startMonitor(Host myself, Storage storage) {
        //Storage defaults to LocalTextStorage
        if(this.simpleMonitor != null){
            throw new IllegalStateException("Monitor already started");
        }
        this.simpleMonitor = new SimpleMonitor(myself, storage);
        try {
            Babel.getInstance().registerProtocol(this.simpleMonitor);
            this.simpleMonitor.init(new Properties());
        } catch (HandlerRegistrationException | ProtocolAlreadyExistsException | IOException e) {
            throw new RuntimeException(e);
        }
        return this.simpleMonitor;
    }

    /**
     * Adds an aggregation request to the monitor<br>
     * An AggregationRequest contains the Aggregation to be performed, and the metrics on which it is to be performed
     * @param aggregation the aggregation request
     */
    public void addAggregation(Aggregation aggregation){
        simpleMonitor.addAggregation(aggregation);
    }
}
