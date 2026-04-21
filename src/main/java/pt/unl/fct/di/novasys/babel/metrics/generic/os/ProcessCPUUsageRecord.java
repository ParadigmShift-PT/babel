package pt.unl.fct.di.novasys.babel.metrics.generic.os;

/**
 * Holds a snapshot of per-process CPU time counters read from {@code /proc/self/stat},
 * used to compute delta-based process CPU utilisation between two consecutive measurements.
 */
public class ProcessCPUUsageRecord {
    private final long active_time;
    private final long elapsed_time;

    /**
     * Constructs a {@code ProcessCPUUsageRecord} with the given active and elapsed CPU time values.
     *
     * @param active_time  cumulative CPU time consumed by the process (utime + stime) in clock ticks
     * @param elapsed_time total elapsed process running time in clock ticks
     */
    public ProcessCPUUsageRecord(long active_time, long elapsed_time) {
        this.active_time = active_time;
        this.elapsed_time = elapsed_time;
    }

    /**
     * Constructs a zero-valued {@code ProcessCPUUsageRecord}, used as the initial baseline for delta calculations.
     */
    public ProcessCPUUsageRecord(){
        this.active_time = 0;
        this.elapsed_time = 0;
    }

    /**
     * Returns the cumulative CPU time consumed by the process in clock ticks.
     *
     * @return active CPU time
     */
    public long getActive_time() {
        return active_time;
    }

    /**
     * Returns the total elapsed process running time in clock ticks.
     *
     * @return elapsed process time
     */
    public long getElapsed_time() {
        return elapsed_time;
    }
}
