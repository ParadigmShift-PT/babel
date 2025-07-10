package pt.unl.fct.di.novasys.babel.metrics.exporters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.metrics.MetricsManager;
import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;
import pt.unl.fct.di.novasys.babel.metrics.formatting.JSONFormatter;
import pt.unl.fct.di.novasys.babel.metrics.formatting.NodeSampleFormatter;
import pt.unl.fct.di.novasys.babel.metrics.formatting.PrometheusFormatter;
import pt.unl.fct.di.novasys.babel.metrics.formatting.SimpleFormatter;
import pt.unl.fct.di.novasys.babel.metrics.utils.JSONParser;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Exporter {

    private static final Logger logger = LogManager.getLogger(Exporter.class);

    private static final String CONFIG_DEFAULT_PATH = System.getProperty("user.dir") + "configs/exporters/";

    private final static String CONFIG_FILE_FORMAT = ".conf";

    private final String exporterName;

    private Properties exporterConfigs;

    private String configPath;

    private ExporterCollectOptions exporterCollectOptions;

    private AtomicBoolean disabled = new AtomicBoolean(false);



    public abstract static class ExporterBuilder<T extends ExporterBuilder<T>> {
        private final String exporterName;
        private String exporterConfigPath = "";
        private Properties exporterConfigs = new Properties();
        private ExporterCollectOptions exporterCollectOptions = ExporterCollectOptions.builder().build();
        private String exporterCollectOptionsPath  = "";
//        private EpochUpdate defaultEpochUpdate = EpochUpdate.tickEpoch();

        public ExporterBuilder(String exporterName) {
            this.exporterName = exporterName;
        }

        /**
         * Method MUST be overridden by subclasses to return the builder itself
         * @return this
         */
        public abstract T self();

        public T exporterConfigs(Properties exporterConfigs){
            this.exporterConfigs = exporterConfigs;
            return self();
        }

        public T exporterConfigPath(String configPath){
            this.exporterConfigPath = configPath;
            return self();
        }

        public T exporterCollectOptions(ExporterCollectOptions exporterCollectOptions){
            this.exporterCollectOptions = exporterCollectOptions;
            return self();
        }

        public T exporterCollectOptionsPath(String exporterCollectOptionsPath){
            this.exporterCollectOptionsPath = exporterCollectOptionsPath;
            return self();
        }

        public abstract Exporter build();
    }

    public Exporter(ExporterBuilder exporterBuilder){
        this.exporterName = exporterBuilder.exporterName;
        this.configPath = exporterBuilder.exporterConfigPath.endsWith("/") ? exporterBuilder.exporterConfigPath : exporterBuilder.exporterConfigPath + "/";
        //In case the path for the exporter configs is specified, load from file, else, load from supplied properties
        if(!exporterBuilder.exporterConfigPath.isEmpty())
            this.exporterConfigs = this.loadConfigFromFile(exporterBuilder.exporterConfigPath);
        else
            this.exporterConfigs = this.loadConfigFromProperties(exporterBuilder.exporterConfigs);


        if(!exporterBuilder.exporterCollectOptionsPath.isEmpty()){
            this.exporterCollectOptions = this.jsonLoadCollectOptions(exporterBuilder.exporterCollectOptionsPath);
        }else{
            this.exporterCollectOptions = exporterBuilder.exporterCollectOptions;
        }
    }

    public String getExporterName() {
        return exporterName;
    }



    /**
     * Sets a flag for the exporter to stop<br>
     * Does not mean that the exporter will stop immediately<br>
     * The exporter MUST check this flag periodically
     */
    public void disable(){
        this.disabled.set(true);
    }

    public boolean isDisabled(){
        return disabled.get();
    }

    public boolean isEnabled(){
        return !disabled.get();
    }


    /**
     * Returns all configurations related to the calling exporter contained in exporterName.conf
     * <p></p>
     * This includes the collect options related to the calling exporter
     * <br>
     * If no config file is found, the exporter will just use default values
     * <br>
     * If a config that is defined in the defaults is absent in the config files, default is used
     * @return Properties object containing all config values
     */
    public Properties loadConfigFromFile(String exporterConfigPath){
        Properties properties = this.loadDefaults();
        File config_file = new File(exporterConfigPath);
        if(config_file.isFile()){
            try {
                properties.load(Files.newInputStream(config_file.toPath()));
            } catch (Exception e) {
                logger.error("Error loading exporter config file:{}", config_file.getAbsolutePath());
            }
        }

        return properties;
    }

    /**
     * Returns all configurations passed in the exporterConfigs parameter merged with the default values
     * @param exporterConfigs Properties object containing all configurations passed in the constructor
     * @return  Properties object containing all config values
     */
    private Properties loadConfigFromProperties(Properties exporterConfigs) {
        Properties properties = this.loadDefaults();
        properties.putAll(exporterConfigs);
        return properties;
    }

    public String getProperty(String key){
        return exporterConfigs.getProperty(key);
    }


    /**
     * Loads default values for the exporter<br>
     * To be implemented by each exporter
     * @return Properties object containing all default values
     */
    public abstract Properties loadDefaults();


//    /**
//     * Loads all collect options related to this exporter
//     * These are loaded from files with the following format protocolId.exporterName.conf
//     * @return ExporterCollectOptions object containing all collect options
//     */
//    public ExporterCollectOptions propertiesLoadCollectOptions(){
//        ExporterCollectOptions exporterCollectOptions1 = ExporterCollectOptions.builder().build();
//        //TODO: verify if regex correctly matches all files
//        FilenameFilter filenameFilter = (dir,name) -> name.matches("[0-9]+\\." + exporterName + CONFIG_FILE_FORMAT);
//        File[] configFiles = new File(configPath).listFiles(filenameFilter);
//        if (configFiles != null) {
//            for (File file : configFiles) {
//                if(file.isFile()) {
//                    short protocolId = Short.parseShort(file.getName().split("\\.")[0]);
//                    Properties properties = new Properties();
//                    try {
//                        properties.load(Files.newInputStream(file.toPath()));
//                        RegistryCollectOptions registryCollectOptions = new RegistryCollectOptions();
//                        exporterCollectOptions1.addRegistryCollectOptions(protocolId, registryCollectOptions);
//                    } catch (Exception e) {
//                        System.out.println("Error loading collect options config file:" + file.getAbsolutePath());
//                    }
//                }
//            }
//        }
//        return exporterCollectOptions1;
//    }

    /**
     * Loads all collect options related to this exporter from a json file
     * @return ExporterCollectOptions object containing all collect options
     */
    @SuppressWarnings("unchecked")
    public ExporterCollectOptions jsonLoadCollectOptions(String path) {
        ExporterCollectOptions exporterCollectOptions = ExporterCollectOptions.builder().build();
        //read from collectOptionsExporterA.json

        if(!new File(path).isFile()){
            return exporterCollectOptions;
        }

        ExporterCollectOptions.Builder exporterCollectOptionsBuilder = ExporterCollectOptions.builder();

       try {
        Map<String, Object> parsedJson = (Map<String, Object>) JSONParser.parseJsonFile(path);

        Boolean collectOSMetrics = (Boolean) parsedJson.get("collectOSMetrics");
        if (collectOSMetrics != null) {
            exporterCollectOptionsBuilder = exporterCollectOptionsBuilder.collectOSMetrics(collectOSMetrics);
            Boolean collectAllProtocols = (Boolean) parsedJson.get("collectAllProtocols");
            logger.debug("Collect all protocols: {}", collectAllProtocols);
            if (collectAllProtocols != null && collectAllProtocols) {
                exporterCollectOptionsBuilder = exporterCollectOptionsBuilder.collectAllMetrics(true);
            } else {
                exporterCollectOptionsBuilder = exporterCollectOptionsBuilder.collectAllMetrics(false);
                List<Integer> protocolsToCollect = (List<Integer>) parsedJson.get("protocolsToCollect");
                if (protocolsToCollect != null) {
                    short[] protocolsToCollectArray = new short[protocolsToCollect.size()];
                    for (int i = 0; i < protocolsToCollect.size(); i++) {
                        protocolsToCollectArray[i] = protocolsToCollect.get(i).shortValue();
                    }
                    exporterCollectOptionsBuilder = exporterCollectOptionsBuilder.protocolsToCollect(protocolsToCollectArray);
                    List<Object> perProtocolCollectOptions = (List<Object>) parsedJson.get("exporterCollectOptions");
                    if(perProtocolCollectOptions != null){
                        Map<Short, ProtocolCollectOptions> protocolCollectOptionsMap = parsePerProtocolCollectOptions(perProtocolCollectOptions);
                        exporterCollectOptionsBuilder = exporterCollectOptionsBuilder.perProtocolCollectOptions(protocolCollectOptionsMap);
                    }
                } else {
                    throw new RuntimeException("Error parsing JSON file - protocolsToCollect not found");
                }

            }
        }


        }catch(ClassCastException e){
            throw new RuntimeException("Error parsing JSON file: " + e.getMessage());
        }



//        JSONParser parser = new JSONParser();
//        try {
//            Reader reader = new FileReader(configPath + "collectOptionsExporter_example.json");
//            JSONObject jsonObject = (JSONObject) parser.parse(reader);
//            String configType = (String) jsonObject.get("configType");
//            if(configType.equals("perProtocolCollectOptions")){
//                JSONArray perProtocolCollectOptions = (JSONArray) jsonObject.get("configs");
//                for (Object o : perProtocolCollectOptions) {
//                    JSONObject protocolCollectOptions = (JSONObject) o;
//                    short protocolId = ((Long) protocolCollectOptions.get("protocolId")).shortValue();
//                    RegistryCollectOptions registryCollectOptions = new RegistryCollectOptions();
//                    JSONArray perMetricCollectOptions = (JSONArray) protocolCollectOptions.get("metrics");
//                    for (Object o1 : perMetricCollectOptions) {
//                        JSONObject metricCollectOptions = (JSONObject) o1;
//                        String metricName = (String) metricCollectOptions.get("name");
//                        JSONObject options = (JSONObject) metricCollectOptions.get("option");
//                        boolean resetOnCollect = (boolean) options.get("resetOnCollect");
//                        CollectOptions.ReduceType reduceType = CollectOptions.ReduceType.valueOf((String) options.get("reduceType"));
//                        CollectOptions collectOptions = new CollectOptions(resetOnCollect, reduceType);
//                        registryCollectOptions.addCollectOptions(metricName, collectOptions);
//                    }
//                    exporterCollectOptions.addRegistryCollectOptions(protocolId, registryCollectOptions);
//                }
//            }
//
//
//        } catch (IOException | ParseException e) {
//            throw new RuntimeException(e);
//        }

        return exporterCollectOptionsBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private Map<Short, ProtocolCollectOptions> parsePerProtocolCollectOptions(List<Object> perProtocolCollectOptions) {
        Map<Short, ProtocolCollectOptions> allProtocolCollectOptionsMap = new HashMap<>();
        for(Object proto : perProtocolCollectOptions){
            Map<String, Object> protocol = (Map<String, Object>) proto;
            short protocolId = (short)((int) protocol.get("protocolId"));

            ProtocolCollectOptions protocolCollectOptions = new ProtocolCollectOptions();
            List<Object> metrics = (List<Object>) protocol.get("protocolCollectOptions");
            for(Object metric : metrics){
                CollectOptions collectOptions = new CollectOptions();

                Map<String, Object> metricMap = (Map<String, Object>) metric;
                String metricName = (String) metricMap.get("name");
                Map<String, Object> options = (Map<String, Object>) metricMap.get("collectOptions");
                Boolean resetOnCollect = (Boolean) options.get("resetOnCollect");

                if(resetOnCollect != null){
                    collectOptions = new CollectOptions(resetOnCollect);
                }


                protocolCollectOptions.addCollectOptions(metricName, collectOptions);
            }
            logger.debug(protocolCollectOptions);
            allProtocolCollectOptionsMap.put(protocolId, protocolCollectOptions);
        }
        return allProtocolCollectOptionsMap;
    }


    public ExporterCollectOptions getExporterCollectOptions(){
        return this.exporterCollectOptions;
    }


    /**
     * Collects all metrics for all protocols <br>
     * @return {@link NodeSample} object containing metrics for all protocols
     */
    public NodeSample collectAllMetrics(){
        return MetricsManager.getInstance().collectMetricsAllProtocols(exporterCollectOptions);
    }

    /**
     * Collects metrics for the protocols specified. <br>
     * @param collectOsMetrics whether to collect OS metrics
     * @param protocolIds array of protocol ids to collect metrics for
     * @return {@link NodeSample} object containing metrics for all specified protocols
     * @throws NoSuchProtocolRegistry if a protocol is not found in the metrics manager
     */
    public NodeSample collectMetrics(boolean collectOsMetrics, short... protocolIds) throws NoSuchProtocolRegistry {
        return MetricsManager.getInstance().collectMetricsProtocols(collectOsMetrics, exporterCollectOptions, protocolIds);
    }


    /**
     * Collects metrics for the protocols specified in the supplied exporterCollectOptions. <br>
     * @return {@link NodeSample} object containing metrics for all specified protocols
     * @throws NoSuchProtocolRegistry if a protocol to be collected is not found in the metrics manager
     */
    public NodeSample collectMetrics() throws NoSuchProtocolRegistry {
        if(exporterCollectOptions.collectMetricsAllProtocols)
            return collectAllMetrics();

        return collectMetrics(exporterCollectOptions.isCollectOSMetrics(), exporterCollectOptions.getProtocolsToCollect());
    }

    /**
     * Instantiates and returns the given NodeSampleFormatter, if none is found, returns the given default formatter
     * @param formatterNameOrClass name or full class of the formatter to instantiate
     * @param defaultFormatter default formatter to return if the given formatter is not found
     * @return NodeSampleFormatter object
     */
    public static NodeSampleFormatter getFormatterOrDefault(String formatterNameOrClass, NodeSampleFormatter defaultFormatter){
        try {
            switch (formatterNameOrClass) {
                case "SimpleFormatter":
                    return new SimpleFormatter();
                case "PrometheusFormatter":
                    return new PrometheusFormatter();
                case "JSONFormatter":
                    return new JSONFormatter();
                default:
                    return Class.forName(formatterNameOrClass).asSubclass(NodeSampleFormatter.class).getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            return defaultFormatter;
        }
    }
}