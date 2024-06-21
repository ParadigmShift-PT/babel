package pt.unl.fct.di.novasys.babel.metrics.generic.os;

public class CPUUsageRecord {
    private final long total_time;
    private final long idle_time;

    public CPUUsageRecord(long total_time, long idle_time) {
        this.total_time = total_time;
        this.idle_time = idle_time;
    }

    public CPUUsageRecord(){
        this.total_time = 0;
        this.idle_time = 0;
    }

    public long getTotal_time() {
        return total_time;
    }

    public long getIdle_time() {
        return idle_time;
    }
}
