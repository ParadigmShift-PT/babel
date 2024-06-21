package pt.unl.fct.di.novasys.babel.metrics.exporters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ExporterCollectOptions {
    private Map<Short, RegistryCollectOptions> perRegistryCollectOptions;
    boolean collectAllMetrics = false;

    short[] protocolsToCollect = new short[0];

    boolean collectOSMetrics = false;


    public ExporterCollectOptions(Map<Short, RegistryCollectOptions> perRegistryCollectOptions) {
        this.perRegistryCollectOptions = perRegistryCollectOptions;
        this.collectAllMetrics = true;
        this.collectOSMetrics = true;
    }


    public ExporterCollectOptions(Map<Short, RegistryCollectOptions> perRegistryCollectOptions, short[] protocolsToCollect, boolean collectOSMetrics) {
        this.perRegistryCollectOptions = perRegistryCollectOptions;
        this.protocolsToCollect = protocolsToCollect;
        this.collectOSMetrics = collectOSMetrics;
    }

    public ExporterCollectOptions(){
        this.perRegistryCollectOptions = new HashMap<>();
    }

    public void addRegistryCollectOptions(short protocolId, RegistryCollectOptions registryCollectOptions){
        this.perRegistryCollectOptions.put(protocolId, registryCollectOptions);
    }

    public RegistryCollectOptions getRegistryCollectOptions(Short protocolId){
        return this.perRegistryCollectOptions.get(protocolId);
    }

    public boolean isCollectAllMetrics() {
        return collectAllMetrics;
    }


    public short[] getProtocolsToCollect() {
        return Arrays.copyOf(protocolsToCollect, protocolsToCollect.length);
    }

    public boolean isCollectOSMetrics() {
        return collectOSMetrics;
    }


}
