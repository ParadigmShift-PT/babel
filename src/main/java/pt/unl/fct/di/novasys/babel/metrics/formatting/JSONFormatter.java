package pt.unl.fct.di.novasys.babel.metrics.formatting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.unl.fct.di.novasys.babel.metrics.MetricSample;
import pt.unl.fct.di.novasys.babel.metrics.NodeSample;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoSuchProtocolRegistry;

import java.util.Map;

public class JSONFormatter implements NodeSampleFormatter, MetricSampleFormatter, IdentifiedNodeSampleFormatter {
    public static final String NAME = "JSONFormatter";

    ObjectMapper objectMapper;

    public JSONFormatter() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getFormatterName() {
        return NAME;
    }

    public String format(Object sample) {
        try {
            return objectMapper.writeValueAsString(sample);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String format(MetricSample sample) {
        try {
            return objectMapper.writeValueAsString(sample);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String format(NodeSample sample) throws NoSuchProtocolRegistry {
        try {
            return objectMapper.writeValueAsString(sample);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String format(String node, NodeSample sample) {
        try {
            return objectMapper.writeValueAsString(Map.of(node, sample));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String format(Map<String, NodeSample> samples) {
        try {
            return objectMapper.writeValueAsString(samples);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
