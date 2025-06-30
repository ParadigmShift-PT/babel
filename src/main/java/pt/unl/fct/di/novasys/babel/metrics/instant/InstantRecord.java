package pt.unl.fct.di.novasys.babel.metrics.instant;

import pt.unl.fct.di.novasys.babel.metrics.MetricSample;
import pt.unl.fct.di.novasys.babel.metrics.Record;
import pt.unl.fct.di.novasys.babel.metrics.exporters.CollectOptions;

public class InstantRecord extends Record {
    InstantExporter exporter;


    private InstantRecord(Builder builder) {
        super(builder);
        this.exporter = builder.exporter;
    }

    public void record(String... labelValues) {
        super.record(labelValues);
        MetricSample ms = super.collect(new CollectOptions(true));
        exporter.addMetricSample(ms);
    }

    public static Builder builder(String name, InstantExporter exporter,  String... recordParams){
        return new Builder(name, exporter,recordParams);
    }



    public static class Builder extends Record.Builder {

        private InstantExporter exporter;

        public Builder(String name,InstantExporter exporter, String... recordParams){
            super(name, recordParams);
            this.exporter = exporter;
        }

        @Override
        public Builder self() {
            return this;
        }

        public Record build(){
            return new InstantRecord(this);
        }
    }


}
