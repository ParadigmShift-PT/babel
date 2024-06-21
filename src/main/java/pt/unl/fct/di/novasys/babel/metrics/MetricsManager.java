package pt.unl.fct.di.novasys.babel.metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.DuplicatedProtocolMetric;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoProcfsException;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.OSMetricsConfigException;
import pt.unl.fct.di.novasys.babel.metrics.exporters.Exporter;
import pt.unl.fct.di.novasys.babel.metrics.exporters.ExporterCollectOptions;
import pt.unl.fct.di.novasys.babel.metrics.exporters.RegistryCollectOptions;
import pt.unl.fct.di.novasys.babel.metrics.generic.os.OSMetrics;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsManager {

    private static final String CURRENT_WORKING_DIR = System.getProperty("user.dir");

    private static final short OS_METRIC_PROTOCOL_ID = -1;
    private static final Logger logger = LogManager.getLogger(MetricsManager.class);


    private final ConcurrentHashMap<Short, MetricRegistry> registries = new ConcurrentHashMap<>();

    OSMetrics osMetrics;
    /**
     * A set of exporters, responsible for exporting samples of metrics
     */
    List<Exporter> exporters;



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

    private final Queue<MetricSchedule> toRegister;

    private boolean started;

    private MetricsManager() {
        //scheduler = Executors.newSingleThreadScheduledExecutor();
        toRegister = new LinkedList<>();
        started = false;
    }

    public void registerExporters(Exporter...exporters){
        if(exporters.length == 0){
            registerExportersUsingConfig();
        }
        //The main exporter for each protocol is the one that is able to reset and advance the epoch of the protocol metrics
        for (Exporter exp : exporters){
            if(!exp.getExporterCollectOptions().isCollectAllMetrics()){
                short[] protocolIDs = exp.getExporterCollectOptions().getProtocolsToCollect();
                for(short protoId : protocolIDs){
                    if(!this.protocolExporters.containsKey(protoId)){
                        this.protocolExporters.put(protoId, exp);
                    }
                }
            }
        }


       if (this.exporters == null) {
           this.exporters = new ArrayList<>(exporters.length);
       }

       this.exporters.addAll(Arrays.asList(exporters));

    }



    //TODO: Register exporters using config instead of method calls
    private void registerExportersUsingConfig(){
        JSONParser parser = new JSONParser();
        try {
            Reader reader = new FileReader(CURRENT_WORKING_DIR + "collectOptionsExporter_example.json");
            JSONObject jsonObject = (JSONObject) parser.parse(reader);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }


    public synchronized void registerOSMetrics(OSMetrics.MetricType ...metricTypes) throws NoProcfsException, OSMetricsConfigException, DuplicatedProtocolMetric {
        if (this.osMetrics == null) {
            this.osMetrics = new OSMetrics();
        }

        for(OSMetrics.MetricType mt : metricTypes){
            registerMetric(osMetrics.getOSMetric(mt, osMetrics), OS_METRIC_PROTOCOL_ID);
        }

    }

    public synchronized void registerOSMetricCategory(OSMetrics.MetricCategory ...metricCategories) throws NoProcfsException, OSMetricsConfigException, DuplicatedProtocolMetric {
        if (this.osMetrics == null) {
            this.osMetrics = new OSMetrics();
        }
        Set<OSMetrics.MetricType> mts = osMetrics.getMetricsFromCategories(metricCategories);

        registerOSMetrics(mts.toArray(new OSMetrics.MetricType[0]));
    }


    public synchronized void start() {
        //Start exporter threads and set main exporters for each protocol
        for(Exporter ex : exporters){
            for(short protoId : ex.getExporterCollectOptions().getProtocolsToCollect()){
                this.protocolExporters.put(protoId, ex);
            }
            
            new Thread(ex, ex.getExporterName()).start();
        }

        started = true;
    }

    //private void scheduleMetric(Metric m) {
    //    scheduler.scheduleAtFixedRate(() -> logMetric(m), m.getPeriod(), m.getPeriod(), TimeUnit.MILLISECONDS);
    //}

//    public synchronized void registerMetric(Metric m) {
//      /*  if (m.isLogPeriodically()) {
//            if (started) scheduleMetric(m);
//            else toRegister.add(m);
//        }
//        if (m.isLogOnChange()) {
//            m.setOnChangeHandler(this::logMetric);
//        }*/
//    }

    public synchronized void registerMetric(Metric m, short protocolId) throws DuplicatedProtocolMetric {
        //toRegister.add(new MetricSchedule(protocolId,m));
        if(registries.containsKey(protocolId)){
            registries.get(protocolId).register(m);
        }else{
            MetricRegistry mr = new MetricRegistry(protocolId);
            mr.register(m);
            registries.put(protocolId, mr);
        }
    }



    private boolean isMainExporterForProtocol(short protocolId, Exporter exporter){
        return protocolExporters.containsKey(protocolId) && protocolExporters.get(protocolId).equals(exporter);
    }

    /**
     * Ends current metric epoch, exporting metrics for all registered protocols <br>
     * Used by and exporter that is only collecting all metrics, and won't be able to reset them or advance the epoch
     * @return MultiRegistryEpochSample containing the metrics for all registered protocols
     */
    public MultiRegistryEpochSample collectMetricsAllProtocols(ExporterCollectOptions exporterCollectOptions)  {
        //When an exporter collects metrics for all protocols, it can't reset the metrics or advance the epoch, so we don't need to pass the exporter
//        if (!this.started)
//            throw new MetricsManagerNotStartedException();

        MultiRegistryEpochSample mres = new MultiRegistryEpochSample();
        for (Short protocolId : registries.keySet()) {
            mres.addRegistrySample(protocolId, collectMetricForProtocol(protocolId, exporterCollectOptions, false));
        }
        return mres;
    }

    public MultiRegistryEpochSample collectMetricsProtocols(Exporter exp, boolean collectOSMetrics, ExporterCollectOptions exporterCollectOptions, short ...protocolIds) throws NoSuchProtocolRegistry{
//        if (!this.started)
//           throw new MetricsManagerNotStartedException();

        MultiRegistryEpochSample mres = new MultiRegistryEpochSample();


        if(collectOSMetrics && osMetrics != null){
            //OS metrics can be collected by any exporter, thus can be ticked, by any exporter
            EpochSample osSample = collectMetricForProtocol(OS_METRIC_PROTOCOL_ID, exporterCollectOptions, true);
            mres.addRegistrySample(OS_METRIC_PROTOCOL_ID, osSample);
        }

        for (short protoId : protocolIds) {
            boolean isMainExporter = isMainExporterForProtocol(protoId, exp);
            EpochSample epochSample = collectMetricForProtocol(protoId, exporterCollectOptions, isMainExporter);
            mres.addRegistrySample(protoId, epochSample);
        }
        return mres;
    }



    private EpochSample collectMetricForProtocol(short protocolId, ExporterCollectOptions exporterCollectOptions, boolean isMainExporter) throws NoSuchProtocolRegistry {

        if(!registries.containsKey(protocolId)){
            throw new NoSuchProtocolRegistry(protocolId);
        }

        RegistryCollectOptions registryCollectOptions = exporterCollectOptions.getRegistryCollectOptions(protocolId);

        if(registryCollectOptions == null){
            registryCollectOptions = new RegistryCollectOptions();
        }

        //If it is the main exporter for the protocol, it can reset the metrics and advance the epoch
        return registries.get(protocolId).collect(registryCollectOptions, isMainExporter);
    }

//    public EpochSample endProtocolEpoch(short protocolId) throws NoSuchProtocolRegistry, MetricsManagerNotStartedException {
//        if (!this.started)
//            throw new MetricsManagerNotStartedException();
//
//        if (!registries.containsKey(protocolId))
//            throw new NoSuchProtocolRegistry(protocolId);
//
//        return registries.get(protocolId).collect();
//    }

//    public void logMetric(Metric m) {
//        synchronized (m) {
//            logger.info("[" + Babel.getInstance().getMillisSinceStart() + "] " + m.getName() + " " + m.computeValue());
//            if (m.isResetOnLog()) m.reset();
//        }
//    }

}
