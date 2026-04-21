package pt.unl.fct.di.novasys.babel.metrics.generic.os;

/**
 * Holds a snapshot of system-wide CPU time counters read from {@code /proc/stat},
 * used to compute delta-based CPU utilisation between two consecutive measurements.
 */
public class CPUUsageRecord {
    private final long total_time;
    private final long idle_time;

    /**
     * Constructs a {@code CPUUsageRecord} with the given total and idle CPU time values.
     *
     * @param total_time cumulative total CPU time in clock ticks
     * @param idle_time  cumulative idle CPU time in clock ticks
     */
    public CPUUsageRecord(long total_time, long idle_time) {
        this.total_time = total_time;
        this.idle_time = idle_time;
    }

    /**
     * Constructs a zero-valued {@code CPUUsageRecord}, used as the initial baseline for delta calculations.
     */
    public CPUUsageRecord(){
        this.total_time = 0;
        this.idle_time = 0;
    }

    /**
     * Returns the cumulative total CPU time in clock ticks.
     *
     * @return total CPU time
     */
    public long getTotal_time() {
        return total_time;
    }

    /**
     * Returns the cumulative idle CPU time in clock ticks.
     *
     * @return idle CPU time
     */
    public long getIdle_time() {
        return idle_time;
    }
}
