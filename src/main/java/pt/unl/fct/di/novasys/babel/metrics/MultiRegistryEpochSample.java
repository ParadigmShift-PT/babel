package pt.unl.fct.di.novasys.babel.metrics;

import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MultiRegistryEpochSample {

    private final Map<Short, EpochSample> samplePerRegistry;

    public MultiRegistryEpochSample() {
        this.samplePerRegistry = new HashMap<>();
    }

    public void addRegistrySample(short registryID, EpochSample epochSample){
        this.samplePerRegistry.put(registryID, epochSample);
    }

    public Set<Short> getRegistryIds(){
        return samplePerRegistry.keySet();
    }

    public EpochSample getRegistrySample(short registryId) throws NoSuchProtocolRegistry {
        if(!samplePerRegistry.containsKey(registryId)){
            throw new NoSuchProtocolRegistry(registryId);
        }
        return samplePerRegistry.get(registryId);

    }


}
