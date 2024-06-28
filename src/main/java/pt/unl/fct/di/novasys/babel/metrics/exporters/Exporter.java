package pt.unl.fct.di.novasys.babel.metrics.exporters;

import java.io.*;
import java.nio.file.Files;
import java.util.Properties;
//import org.json.simple.*;
//import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.ParseException;
import pt.unl.fct.di.novasys.babel.metrics.MetricsManager;
import pt.unl.fct.di.novasys.babel.metrics.MultiRegistryEpochSample;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;

public abstract class Exporter implements Runnable {


    private final String exporterName;
    private static final String CONFIG_DEFAULT_PATH = System.getProperty("user.dir") + "configs/exporters/";

    private final static String CONFIG_FILE_FORMAT = ".conf";

    private Properties exporterConfigs;

    private String configPath = CONFIG_DEFAULT_PATH;

    private ExporterCollectOptions exporterCollectOptions;



    public Exporter(String exporterName) {
        this.exporterName = exporterName;
        this.exporterConfigs = this.loadConfigFromFile();
        this.exporterCollectOptions = this.jsonLoadCollectOptions();
    }

    public Exporter(String exporterName, Properties properties) {
        this.exporterName = exporterName;
        this.exporterConfigs = this.loadConfigFromProperties(properties);
        this.exporterCollectOptions = this.jsonLoadCollectOptions();
    }


    public Exporter(String exporterName, String configPath) {
        this.configPath = configPath.endsWith("/") ? configPath : configPath + "/";
        this.exporterName = exporterName;
        this.exporterConfigs = this.loadConfigFromFile();
        this.exporterCollectOptions = this.jsonLoadCollectOptions();
    }

    public Exporter(String exporterName, String configPath, ExporterCollectOptions exporterCollectOptions) {
        this.configPath = configPath.endsWith("/") ? configPath : configPath + "/";
        this.exporterName = exporterName;
        this.exporterConfigs = this.loadConfigFromFile();
        this.exporterCollectOptions = exporterCollectOptions;
    }

    public Exporter(String exporterName, ExporterCollectOptions exporterCollectOptions) {
        this.exporterName = exporterName;
        this.exporterConfigs = this.loadConfigFromFile();
        this.exporterCollectOptions = exporterCollectOptions;
    }

    public Exporter(String exporterName, Properties exporterConfigs, ExporterCollectOptions exporterCollectOptions) {
        this.exporterName = exporterName;
        this.exporterConfigs = this.loadConfigFromProperties(exporterConfigs);
        this.exporterCollectOptions = exporterCollectOptions;
    }




    public String getExporterName() {
        return exporterName;
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
    public Properties loadConfigFromFile(){
        Properties properties = this.loadDefaults();
        File config_file = new File(configPath + exporterName + CONFIG_FILE_FORMAT);
        if(config_file.isFile()){
            try {
                properties.load(Files.newInputStream(config_file.toPath()));
            } catch (Exception e) {
                System.out.println("Error loading exporter config file:" + config_file.getAbsolutePath());
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


    /**
     * Loads all collect options related to this exporter
     * These are loaded from files with the following format protocolId.exporterName.conf
     * @return ExporterCollectOptions object containing all collect options
     */
    public ExporterCollectOptions propertiesLoadCollectOptions(){
        ExporterCollectOptions exporterCollectOptions1 = new ExporterCollectOptions();
        //TODO: verify if regex correctly matches all files
        FilenameFilter filenameFilter = (dir,name) -> name.matches("[0-9]+\\." + exporterName + CONFIG_FILE_FORMAT);
        File[] configFiles = new File(configPath).listFiles(filenameFilter);
        if (configFiles != null) {
            for (File file : configFiles) {
                if(file.isFile()) {
                    short protocolId = Short.parseShort(file.getName().split("\\.")[0]);
                    Properties properties = new Properties();
                    try {
                        properties.load(Files.newInputStream(file.toPath()));
                        RegistryCollectOptions registryCollectOptions = new RegistryCollectOptions();
                        exporterCollectOptions1.addRegistryCollectOptions(protocolId, registryCollectOptions);
                    } catch (Exception e) {
                        System.out.println("Error loading collect options config file:" + file.getAbsolutePath());
                    }
                }
            }
        }
        return exporterCollectOptions1;
    }

    /**
     * TODO: clear this mess
     * TODO: remove JSON parser dependency
     * Loads all collect options related to this exporter from a json file
     * @return ExporterCollectOptions object containing all collect options
     */
    public ExporterCollectOptions jsonLoadCollectOptions() {
        ExporterCollectOptions exporterCollectOptions = new ExporterCollectOptions();
        //read from collectOptionsExporterA.json

        //Check if file exists
        if(!new File(configPath + "collectOptionsExporter_example.json").isFile()){
            return new ExporterCollectOptions();
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

        return exporterCollectOptions;
    }


    public ExporterCollectOptions getExporterCollectOptions(){
        return this.exporterCollectOptions;
    }




    public MultiRegistryEpochSample collectAllMetrics() {
        return MetricsManager.getInstance().collectMetricsAllProtocols(exporterCollectOptions);
    }


    private MultiRegistryEpochSample collectMetrics(boolean collectOsMetrics, short... protocolIds) throws NoSuchProtocolRegistry {
        return MetricsManager.getInstance().collectMetricsProtocols(this, collectOsMetrics, exporterCollectOptions, protocolIds);
    }

    public MultiRegistryEpochSample collectMetrics() throws NoSuchProtocolRegistry {
        return collectMetrics(exporterCollectOptions.isCollectOSMetrics(), exporterCollectOptions.getProtocolsToCollect());
    }

}