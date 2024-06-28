package pt.unl.fct.di.novasys.babel.metrics.generic.os;

public class ProcessCPUUsageRecord {
    private final long active_time;
    private final long elapsed_time;

    public ProcessCPUUsageRecord(long active_time, long elapsed_time) {
        this.active_time = active_time;
        this.elapsed_time = elapsed_time;
    }

    public ProcessCPUUsageRecord(){
        this.active_time = 0;
        this.elapsed_time = 0;
    }

    public long getActive_time() {
        return active_time;
    }

    public long getElapsed_time() {
        return elapsed_time;
    }
}
