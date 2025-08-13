package pt.unl.fct.di.novasys.babel.metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final int DEFAULT_RECORDS_ESTIMATE = 25;

    private boolean timestampParam;

    private String[] recordParams;
    private List<LabelValues> records;

    private final Object lock = new Object();

    /**
     * Number of parameters that are used to record the event.
     */
    private int numRecordParams;



    protected Record(Builder builder) {
       super(builder);
       init(builder.timestampParam, builder.estimatedRecords, builder.recordParams);
    }

    private Record(Record r){
        super(r);
        init(r.timestampParam, 0, r.recordParams);
    }

    public void init(boolean timestampParam, int estimatedRecords, String... recordParams) {
        this.timestampParam = timestampParam;
        this.recordParams = recordParams;

        if(this.timestampParam)
            this.numRecordParams = recordParams.length - 1;
        else
            this.numRecordParams = recordParams.length;

        this.records = new ArrayList<>(estimatedRecords);
    }

    public void record(String... parameters) {
        if(isDisabled()) return;

        if (this.numRecordParams != parameters.length)
            throw new IncorrectLabelNumberException();

        String[] recordParamsCopy = Arrays.copyOf(parameters, this.numRecordParams + (this.timestampParam ? 1 : 0));
        if (this.timestampParam) {
            recordParamsCopy[parameters.length] = String.valueOf(System.currentTimeMillis());
        }

        LabelValues lv = new LabelValues(recordParamsCopy);
        synchronized (lock) {
            this.records.add(lv);
        }
    }

    public static Builder builder(String name, String... recordParams){
        return new Builder(name, recordParams);
    }

    @Override
    protected void resetThisMetric() {
        synchronized (lock) {
            this.records.clear();
        }
    }

    @Override
    protected MetricSample collectMetric() {
        //Snapshot approach allows for concurrent recording without blocking the recording thread while iterating over the records

        List<LabelValues> snapshot;
        synchronized (lock){
            snapshot = new ArrayList<>(this.records);
        }
        List<Sample> samples = new ArrayList<>(snapshot.size());
        for(LabelValues lv : snapshot){
            samples.add(new Sample(0, recordParams, Arrays.copyOf(lv.getLabelValues(), recordParams.length)));
        }

        return sampleBuilder().labelNames(Arrays.copyOf(recordParams,recordParams.length)).build(samples.toArray(new Sample[0]));
    }

    @Override
    protected Record newInstance() {
        return new Record(this);
    }


    public static class Builder extends MetricBuilder<Builder> {
        private String[] recordParams;
        private boolean timestampParam = false;
        private int estimatedRecords = DEFAULT_RECORDS_ESTIMATE;

        public Builder(String name, String... recordParams){
            super(name, Unit.NONE, MetricType.RECORD);
            this.recordParams = Arrays.copyOf(recordParams, recordParams.length);
        }

        /**
         * Adds a timestamp parameter to the record, this parameter is used to record the time at which the event happened.<br>
         * The parameter is set automatically by the Record Metric class, to the current time in milliseconds.
         * @return the builder
         */
        public Builder timestampParam(){
            if (timestampParam) {
               return this;
            }
            String[] updatedRecordParams = new String[recordParams.length + 1];
            System.arraycopy(recordParams, 0, updatedRecordParams, 0, recordParams.length);
            updatedRecordParams[recordParams.length] = "timestamp";
            recordParams = updatedRecordParams;
            timestampParam = true;
            return this;
        }

        /**
         * Sets the number of records that are estimated to be recorded by this metric, in between resets.<br>
         * This is used to optimize the memory allocation for the records.<br>
         * If the number is not set, or is smaller than 0, a default value of 25 is used.
         * @param num the number of records that are estimated to be recorded
         * @return the builder
         */
        public Builder numberRecordsEstimate(int num){
            if(num > 0)
                this.estimatedRecords = num;
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
