package pt.unl.fct.di.novasys.babel.metrics;

import pt.unl.fct.di.novasys.babel.metrics.exceptions.IncorrectLabelNumberException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This metric is used to record that a certain event happened.<br>
 * An example of this is the ids of the messages that are sent and received by a protocol, to do so we could set the recordParams as "messageId" and "sent/received".<br>
 * This metric is not compatible with the PrometheusExporter (will be ignored by it).
 */
public class Record extends Metric<Record> {
    private final boolean timestampParam;

    private final String[] recordParams;
    private final List<LabelValues> records;

    /**
     * Number of parameters that are used to record the event.
     */
    private final int numRecordParams;


    protected Record(Builder builder) {
       super(builder);
       this.timestampParam = builder.timestampParam;
       this.recordParams = builder.recordParams;

        if(this.timestampParam)
            this.numRecordParams = recordParams.length - 1;
        else
            this.numRecordParams = recordParams.length;

        this.records = new ArrayList<>();
    }

    private Record(Record r){
        super(r);
        this.timestampParam = r.timestampParam;
        this.recordParams = r.recordParams;

        if(this.timestampParam)
            this.numRecordParams = recordParams.length - 1;
        else
            this.numRecordParams = recordParams.length;

        this.records = new ArrayList<>();
    }

    public void record(String... parameters) {
        if(isDisabled()) return;

        if (this.numRecordParams != parameters.length)
            throw new IncorrectLabelNumberException();

        String[] lvalues;
        if (this.timestampParam) {
            lvalues = new String[parameters.length + 1];
            System.arraycopy(parameters, 0, lvalues, 0, parameters.length);
            lvalues[parameters.length] = String.valueOf(System.currentTimeMillis());
        } else {
            lvalues = parameters;
        }

        LabelValues lv = new LabelValues(lvalues);
        this.records.add(lv);
    }

    public static Builder builder(String name, String... recordParams){
        return new Builder(name, recordParams);
    }

    @Override
    protected void resetThisMetric() {
        this.records.clear();
    }

    @Override
    protected MetricSample collectMetric() {
        Sample[] samples = new Sample[records.size()];
        int i = 0;  
        for(LabelValues lv : records){
            samples[i] = new Sample(0, recordParams, Arrays.copyOf(lv.getLabelValues(), recordParams.length));
            i++;
        }

        return sampleBuilder().labelNames(Arrays.copyOf(recordParams,recordParams.length)).build(samples);
    }

    @Override
    protected Record newInstance() {
        return new Record(this);
    }


    public static class Builder extends MetricBuilder<Builder> {
        private String[] recordParams;
        private boolean timestampParam = false;

        public Builder(String name, String... recordParams){
            super(name, Unit.NONE, MetricType.RECORD);
            this.recordParams = recordParams;
        }

        /**
         * Adds a timestamp parameter to the record, this parameter is used to record the time at which the event happened.<br>
         * The parameter is set automatically by the Record Metric class, to the current time in milliseconds.
         * @return the builder
         */
        public Builder timestampParam(){
            String[] updatedRecordParams = new String[recordParams.length + 1];
            System.arraycopy(recordParams, 0, updatedRecordParams, 0, recordParams.length);
            updatedRecordParams[recordParams.length] = "timestamp";
            recordParams = updatedRecordParams;
            timestampParam = true;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public Record build(){
            return new Record(this);
        }
    }
}
