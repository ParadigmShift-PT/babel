package pt.unl.fct.di.novasys.babel.metrics.instant;

import pt.unl.fct.di.novasys.babel.metrics.MetricSample;
import pt.unl.fct.di.novasys.babel.metrics.Record;
import pt.unl.fct.di.novasys.babel.metrics.exporters.CollectOptions;

/**
 * A {@link Record} that immediately exports each new data point to an {@link InstantExporter}
 * as soon as it is recorded, without waiting for a periodic collection cycle.
 */
public class InstantRecord extends Record {
    InstantExporter exporter;


    private InstantRecord(Builder builder) {
        super(builder);
        this.exporter = builder.exporter;
    }

    /**
     * Records a new data point with the given label values and immediately forwards the resulting
     * {@link MetricSample} to the configured {@link InstantExporter}.
     *
     * @param labelValues label values for the recorded data point
     */
    public void record(String... labelValues) {
        super.record(labelValues);
        MetricSample ms = super.collect(new CollectOptions(true));
        exporter.addMetricSample(ms);
    }

    /**
     * Returns a new {@link Builder} for constructing an {@link InstantRecord}.
     *
     * @param name          the name of the metric
     * @param exporter      the exporter to which samples are forwarded immediately
     * @param recordParams  the parameter names used as label keys for each recorded entry
     * @return a new {@link Builder}
     */
    public static Builder builder(String name, InstantExporter exporter,  String... recordParams){
        return new Builder(name, exporter,recordParams);
    }



    /**
     * Builder for {@link InstantRecord}, wiring in the required {@link InstantExporter}.
     */
    public static class Builder extends Record.Builder {

        private InstantExporter exporter;

        /**
         * Constructs a new {@code Builder} for an {@link InstantRecord}.
         *
         * @param name         the name of the metric
         * @param exporter     the exporter to which samples are forwarded immediately
         * @param recordParams the parameter names used as label keys for each recorded entry
         */
        public Builder(String name,InstantExporter exporter, String... recordParams){
            super(name, recordParams);
            this.exporter = exporter;
        }

        @Override
        public Builder self() {
            return this;
        }

        /**
         * Builds and returns a new {@link InstantRecord} from this builder's configuration.
         *
         * @return a new {@link InstantRecord}
         */
        public Record build(){
            return new InstantRecord(this);
        }
    }


}
