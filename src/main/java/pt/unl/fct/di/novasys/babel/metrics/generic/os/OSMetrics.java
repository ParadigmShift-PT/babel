package pt.unl.fct.di.novasys.babel.metrics.generic.os;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.metrics.Metric;
import pt.unl.fct.di.novasys.babel.metrics.MetricSample;
import pt.unl.fct.di.novasys.babel.metrics.OSMetric;
import pt.unl.fct.di.novasys.babel.metrics.Sample;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.NoProcfsException;
import pt.unl.fct.di.novasys.babel.metrics.exceptions.OSMetricsConfigException;
import pt.unl.fct.di.novasys.babel.metrics.exporters.CollectOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * This class provides a set of metrics related to the operating system
 * <p>
 * It relies almost exclusively on the <b>/proc</b> filesystem, thus it is only available on Linux
 */
public class OSMetrics {

    private static final Logger logger = LogManager.getLogger(OSMetrics.class);

    private static final String[] MEMORY_LABEL_NAMES = new String[]{"memory"};

    Map<MetricType, String> units;
    Map<MetricType, String> names;



    private Map<MetricType, String> getMetricUnits() {
        Map<MetricType, String> units = new HashMap<>();

        units.put(MetricType.SYSTEM_CPU_USAGE, Metric.Unit.PERCENTAGE);
        units.put(MetricType.SYSTEM_LOAD_AVERAGE, Metric.Unit.NONE);
        units.put(MetricType.SYSTEM_MEMORY_USAGE_PERCENTAGE, Metric.Unit.PERCENTAGE);
        units.put(MetricType.SYSTEM_MEMORY_USAGE, Metric.Unit.KBYTES);

        units.put(MetricType.PROCESS_CPU_USAGE, Metric.Unit.PERCENTAGE);
        units.put(MetricType.PROCESS_MEMORY_USAGE_PERCENTAGE, Metric.Unit.PERCENTAGE);
        units.put(MetricType.PROCESS_MEMORY_USAGE, Metric.Unit.KBYTES);

        units.put(MetricType.SYSTEM_DISK_WRITE_BYTES, Metric.Unit.BYTES);
        units.put(MetricType.SYSTEM_DISK_READ_BYTES, Metric.Unit.BYTES);
        units.put(MetricType.PROCESS_DISK_WRITE_BYTES, Metric.Unit.BYTES);
        units.put(MetricType.PROCESS_DISK_WRITE_NUM, Metric.Unit.NONE);
        units.put(MetricType.PROCESS_DISK_READ_BYTES, Metric.Unit.BYTES);
        units.put(MetricType.PROCESS_DISK_READ_NUM, Metric.Unit.NONE);

        units.put(MetricType.SYSTEM_NETWORK_WRITE_BYTES, Metric.Unit.BYTES);
        units.put(MetricType.SYSTEM_NETWORK_WRITE_PACKETS, Metric.Unit.NONE);
        units.put(MetricType.SYSTEM_NETWORK_READ_BYTES, Metric.Unit.BYTES);
        units.put(MetricType.SYSTEM_NETWORK_READ_PACKETS, Metric.Unit.NONE);

        return units;
    }

    private Map<MetricType, String> getMetricNames() {
        Map<MetricType, String> names = new HashMap<>();


        names.put(MetricType.SYSTEM_CPU_USAGE, "system_cpu_usage");
        names.put(MetricType.SYSTEM_LOAD_AVERAGE, "system_load_average");
        names.put(MetricType.SYSTEM_MEMORY_USAGE_PERCENTAGE, "system_memory_usage_percentage");
        names.put(MetricType.SYSTEM_MEMORY_USAGE, "system_memory_usage");
        names.put(MetricType.PROCESS_CPU_USAGE, "process_cpu_usage");
        names.put(MetricType.PROCESS_MEMORY_USAGE_PERCENTAGE, "process_memory_usage_percentage");
        names.put(MetricType.PROCESS_MEMORY_USAGE, "process_memory_usage");
        names.put(MetricType.SYSTEM_DISK_WRITE_BYTES, "system_disk_write_bytes");
        names.put(MetricType.SYSTEM_DISK_READ_BYTES, "system_disk_read_bytes");
        names.put(MetricType.PROCESS_DISK_WRITE_BYTES, "process_disk_write_bytes");
        names.put(MetricType.PROCESS_DISK_WRITE_NUM, "process_disk_write_num");
        names.put(MetricType.PROCESS_DISK_READ_BYTES, "process_disk_read_bytes");
        names.put(MetricType.PROCESS_DISK_READ_NUM, "process_disk_read_num");
        names.put(MetricType.SYSTEM_NETWORK_WRITE_BYTES, "system_network_write_bytes");
        names.put(MetricType.SYSTEM_NETWORK_WRITE_PACKETS, "system_network_write_packets");
        names.put(MetricType.SYSTEM_NETWORK_READ_BYTES, "system_network_read_bytes");
        names.put(MetricType.SYSTEM_NETWORK_READ_PACKETS, "system_network_read_packets");

        return names;
    }


    public OSMetric getOSMetric(MetricType mt, OSMetrics osm) {
        String unit = units.get(mt);
        String name = names.get(mt);

        String[] networkLabelNames = new String[]{"interface"};
        String[] absoluteMemoryLabelNames = new String[]{"total_memory"};

        switch (mt) {
            case SYSTEM_CPU_USAGE:
                return new OSMetric(name, unit, Metric.MetricType.GAUGE, osm, mt) {
                    @Override
                    protected MetricSample collectMetric() {
                        return sampleBuilder().build(new Sample(getOsMetrics().systemCPUUsage()));
                    }
                };
            case SYSTEM_LOAD_AVERAGE:
                return new OSMetric(name, unit, Metric.MetricType.GAUGE, osm, mt) {
                    @Override
                    protected MetricSample collectMetric() {
                        return sampleBuilder().build(new Sample(getOsMetrics().getSystemLoadAverage_Minute()));
                    }
                };

            case SYSTEM_MEMORY_USAGE_PERCENTAGE:
                return new OSMetric(name, unit, Metric.MetricType.GAUGE, osm, mt) {
                    @Override
                    protected MetricSample collectMetric() {
                        return sampleBuilder().build(new Sample(getOsMetrics().getSystemUsedMemoryPercentage()));
                    }
                };

            case SYSTEM_MEMORY_USAGE:
                return new OSMetric(name, unit, Metric.MetricType.GAUGE, osm, mt) {
                    @Override
                    protected MetricSample collectMetric() {
                        Sample[] samples = getMemorySample(getSystemMemoryAbsolute());
                        return sampleBuilder().labelNames(MEMORY_LABEL_NAMES).build(samples);
                    }
                };

            case SYSTEM_DISK_READ_BYTES:
                return new OSMetric(name, unit, Metric.MetricType.COUNTER, osm, mt) {
                    @Override
                    protected MetricSample collectMetric() {
                        return sampleBuilder().build(new Sample(getOsMetrics().getSystemDiskReadBytes()));
                    }
                };


            case SYSTEM_DISK_WRITE_BYTES:
                return new OSMetric(name, unit, Metric.MetricType.COUNTER, osm, mt) {
                    @Override
                    protected MetricSample collectMetric() {
                        return sampleBuilder().build(new Sample(getOsMetrics().getSystemDiskWriteBytes()));
                    }
                };

            case SYSTEM_NETWORK_WRITE_BYTES:
                return new OSMetric(name, unit, Metric.MetricType.COUNTER, osm, mt) {

                    final Map<String, Long> values = getOsMetrics().getSystemNetworkWriteBytes();

                    @Override
                    protected MetricSample collectMetric() {
                        Sample[] samples = new Sample[values.size()];
                        int i = 0;
                        for (Map.Entry<String, Long> entry : values.entrySet()) {
                            samples[i++] = new Sample(entry.getValue().doubleValue(), networkLabelNames, new String[]{entry.getKey()});
                        }


                        return sampleBuilder().labelNames(networkLabelNames).build(samples);
                    }
                };
            case SYSTEM_NETWORK_WRITE_PACKETS:
                return new OSMetric(name, unit, Metric.MetricType.COUNTER, osm, mt) {
                    final Map<String, Long> values = getOsMetrics().getSystemNetworkWritePackets();

                    @Override
                    protected MetricSample collectMetric() {
                        Sample[] samples = new Sample[values.size()];
                        int i = 0;
                        for (Map.Entry<String, Long> entry : values.entrySet()) {
                            samples[i++] = new Sample(entry.getValue().doubleValue(), networkLabelNames, new String[]{entry.getKey()});
                        }

                        return sampleBuilder().labelNames(networkLabelNames).build(samples);
                    }
                };
            case SYSTEM_NETWORK_READ_BYTES:
                return new OSMetric(name, unit, Metric.MetricType.COUNTER, osm, mt) {
                    final Map<String, Long> values = getOsMetrics().getSystemNetworkReadBytes();

                    @Override
                    protected MetricSample collectMetric() {
                        Sample[] samples = new Sample[values.size()];
                        int i = 0;
                        for (Map.Entry<String, Long> entry : values.entrySet()) {
                            samples[i++] = new Sample(entry.getValue().doubleValue(), networkLabelNames, new String[]{entry.getKey()});
                        }


                        return sampleBuilder().labelNames(networkLabelNames).build(samples);
                    }
                };
            case SYSTEM_NETWORK_READ_PACKETS:
                return new OSMetric(name, unit, Metric.MetricType.COUNTER, osm, mt) {
                    final Map<String, Long> values = getOsMetrics().getSystemNetworkReadPackets();

                    @Override
                    protected MetricSample collectMetric() {
                        Sample[] samples = new Sample[values.size()];
                        int i = 0;
                        for (Map.Entry<String, Long> entry : values.entrySet()) {
                            samples[i++] = new Sample(entry.getValue().doubleValue(), networkLabelNames, new String[]{entry.getKey()});
                        }

                        return sampleBuilder().labelNames(networkLabelNames).build(samples);
                    }
                };
            case PROCESS_CPU_USAGE:
                return new OSMetric(name, unit, Metric.MetricType.GAUGE, osm, mt) {
                    @Override
                    protected MetricSample collectMetric() {
                        return sampleBuilder().build(new Sample(getOsMetrics().processCPUUsage()));
                    }
                };
            case PROCESS_MEMORY_USAGE_PERCENTAGE:
                return new OSMetric(name, unit, Metric.MetricType.GAUGE, osm, mt) {

                    @Override
                    protected MetricSample collectMetric() {
                        return sampleBuilder().build(new Sample(getOsMetrics().getProcessMemoryUsagePercentage()));
                    }
                };

            case PROCESS_MEMORY_USAGE:
                return new OSMetric(name, unit, Metric.MetricType.GAUGE, osm, mt) {

                    @Override
                    protected MetricSample collectMetric() {
                        Sample[] samples = getMemorySample(getProcessMemoryUsage());
                        return sampleBuilder().labelNames(MEMORY_LABEL_NAMES).build(samples);
                    }
                };
            case PROCESS_DISK_READ_BYTES:
                return new OSMetric(name, unit, Metric.MetricType.COUNTER, osm, mt) {
                    @Override
                    protected MetricSample collectMetric() {
                        return sampleBuilder().build(new Sample(getOsMetrics().getProcessDiskReadBytes()));
                    }
                };
            case PROCESS_DISK_READ_NUM:
                return new OSMetric(name, unit, Metric.MetricType.COUNTER, osm, mt) {
                    @Override
                    protected MetricSample collectMetric() {
                        return sampleBuilder().build(new Sample(getOsMetrics().getProcessDiskReadNum()));
                    }
                };
            case PROCESS_DISK_WRITE_BYTES:
                return new OSMetric(name, unit, Metric.MetricType.COUNTER, osm, mt) {
                    @Override
                    protected MetricSample collectMetric() {
                        return sampleBuilder().build(new Sample(getOsMetrics().getProcessDiskWriteBytes()));
                    }
                };
            case PROCESS_DISK_WRITE_NUM:
                return new OSMetric(name, unit, Metric.MetricType.COUNTER, osm, mt) {
                    @Override
                    protected MetricSample collectMetric() {
                        return sampleBuilder().build(new Sample(getOsMetrics().getProcessDiskWriteNum()));
                    }
                };

            default:
                return null;

        }
    }

    /**
     * All the available OS metrics
     * <ul>
     *     <li>SYSTEM_CPU_USAGE: System-wide CPU usage</li>
     *     <li>SYSTEM_LOAD_AVERAGE: System load average</li>
     *     <li>SYSTEM_MEMORY_USAGE_PERCENTAGE: System memory usage as a percentage of total memory</li>
     *     <li>SYSTEM_MEMORY_USAGE: System memory usage in KBytes</li>
     *     <li>PROCESS_CPU_USAGE: Process CPU usage</li>
     *     <li>PROCESS_MEMORY_USAGE: Process memory usage in KBytes</li>
     *     <li>PROCESS_MEMORY_USAGE_PERCENTAGE: Process memory usage as a percentage of total memory available to process</li>
     *     <li>SYSTEM_DISK_WRITE_BYTES: System disk write bytes</li>
     *     <li>SYSTEM_DISK_READ_BYTES: System disk read bytes</li>
     *     <li>PROCESS_DISK_WRITE_BYTES: Process disk write bytes</li>
     *     <li>PROCESS_DISK_WRITE_NUM: Process disk write number</li>
     *     <li>PROCESS_DISK_READ_BYTES: Process disk read bytes</li>
     *     <li>PROCESS_DISK_READ_NUM: Process disk read number</li>
     *     <li>SYSTEM_NETWORK_WRITE_BYTES: System network write bytes</li>
     *     <li>SYSTEM_NETWORK_WRITE_PACKETS: System network write packets</li>
     *     <li>SYSTEM_NETWORK_READ_BYTES: System network read bytes</li>
     *     <li>SYSTEM_NETWORK_READ_PACKETS: System network read packets</li>
     * </ul>
     */
    public enum MetricType {
        SYSTEM_CPU_USAGE,
        SYSTEM_LOAD_AVERAGE,
        SYSTEM_MEMORY_USAGE_PERCENTAGE,
        SYSTEM_MEMORY_USAGE,
        PROCESS_CPU_USAGE,
        PROCESS_MEMORY_USAGE_PERCENTAGE,
        PROCESS_MEMORY_USAGE,
        SYSTEM_DISK_WRITE_BYTES,
        SYSTEM_DISK_READ_BYTES,
        PROCESS_DISK_WRITE_BYTES,
        PROCESS_DISK_WRITE_NUM,
        PROCESS_DISK_READ_BYTES,
        PROCESS_DISK_READ_NUM,
        SYSTEM_NETWORK_WRITE_BYTES,
        SYSTEM_NETWORK_WRITE_PACKETS,
        SYSTEM_NETWORK_READ_BYTES,
        SYSTEM_NETWORK_READ_PACKETS
    }

    /**
     * All the available metric categories
     * <ul>
     *     <li>SYSTEM: System metrics, namely system-wide CPU and memory usage, system load average, disk and network I/O</li>
     *     <li>PROCESS: Process metrics, namely process CPU and memory usage, disk I/O</li>
     *     <li>DISK: Disk metrics, namely disk read and write bytes and number of reads and writes</li>
     *     <li>CPU: CPU metrics, namely system-wide and process CPU usage</li>
     *     <li>MEMORY: Memory metrics, namely system-wide and process memory usage</li>
     *     <li>NETWORK: Network metrics, namely network read and write bytes and packets</li>
     * </ul>
     */
    public enum MetricCategory {
        SYSTEM,
        PROCESS,
        DISK,
        CPU,
        MEMORY,
        NETWORK
    }

    public static final int CLK_TCK = 100;
    public static final int BLKSIZE = 512;
    private static final String PROC = "/proc/";
    private static final String PROCSTAT = "/proc/stat";
    private static final String PROCMEMINFO = "/proc/meminfo";
    private static final String PROCUPTIME = "/proc/uptime";
    private static final String PROCLOADAVG = "/proc/loadavg";

    public int n_cpus;

    private final String pid;

    private ProcessCPUUsageRecord last_process_cpu_usage = new ProcessCPUUsageRecord();
    private CPUUsageRecord last_system_cpu_usage = new CPUUsageRecord();


    public OSMetrics() throws NoProcfsException, OSMetricsConfigException {
        this.n_cpus = getNumCPUs();

        //check if proc exists
        File f = new File(PROC);
        if (!f.exists() || !f.isDirectory()) {
            throw new NoProcfsException("No /proc directory found");
        }

        try {
            this.pid = new File("/proc/self").getCanonicalFile().getName();
        } catch (IOException e) {
            throw new NoProcfsException("No /proc/self directory found");
        }


        this.units = getMetricUnits();
        this.names = getMetricNames();

        if (this.units.size() != MetricType.values().length || this.names.size() != MetricType.values().length) {
            throw new OSMetricsConfigException("MetricType enum and units/names map are not in sync");
        }
    }


    public Set<MetricType> getMetricsFromCategories(MetricCategory[] categoriesToCollect) {
        Set<MetricType> metricsToCollect = new HashSet<>();
        for (MetricCategory category : categoriesToCollect) {
            switch (category) {
                case SYSTEM:
                    List<MetricType> system_collection = new ArrayList<>();
                    system_collection.add(MetricType.SYSTEM_CPU_USAGE);
                    system_collection.add(MetricType.SYSTEM_LOAD_AVERAGE);
                    system_collection.add(MetricType.SYSTEM_MEMORY_USAGE_PERCENTAGE);
                    system_collection.add(MetricType.SYSTEM_MEMORY_USAGE);
                    system_collection.add(MetricType.SYSTEM_DISK_WRITE_BYTES);
                    system_collection.add(MetricType.SYSTEM_DISK_READ_BYTES);

                    system_collection.add(MetricType.SYSTEM_NETWORK_WRITE_BYTES);
                    system_collection.add(MetricType.SYSTEM_NETWORK_WRITE_PACKETS);
                    system_collection.add(MetricType.SYSTEM_NETWORK_READ_BYTES);
                    system_collection.add(MetricType.SYSTEM_NETWORK_READ_PACKETS);
                    metricsToCollect.addAll(system_collection);
                    break;
                case PROCESS:
                    List<MetricType> process_collection = new ArrayList<>();
                    process_collection.add(MetricType.PROCESS_CPU_USAGE);
                    process_collection.add(MetricType.PROCESS_MEMORY_USAGE_PERCENTAGE);
                    process_collection.add(MetricType.PROCESS_MEMORY_USAGE);
                    process_collection.add(MetricType.PROCESS_DISK_WRITE_BYTES);
                    process_collection.add(MetricType.PROCESS_DISK_WRITE_NUM);
                    process_collection.add(MetricType.PROCESS_DISK_READ_BYTES);
                    process_collection.add(MetricType.PROCESS_DISK_READ_NUM);
                    metricsToCollect.addAll(process_collection);
                    break;
                case DISK:
                    List<MetricType> disk_collection = new ArrayList<>();
                    disk_collection.add(MetricType.SYSTEM_DISK_WRITE_BYTES);
                    disk_collection.add(MetricType.SYSTEM_DISK_READ_BYTES);
                    disk_collection.add(MetricType.PROCESS_DISK_WRITE_BYTES);
                    disk_collection.add(MetricType.PROCESS_DISK_WRITE_NUM);
                    disk_collection.add(MetricType.PROCESS_DISK_READ_BYTES);
                    disk_collection.add(MetricType.PROCESS_DISK_READ_NUM);
                    metricsToCollect.addAll(disk_collection);
                    break;
                case CPU:
                    List<MetricType> cpu_collection = new ArrayList<>();
                    cpu_collection.add(MetricType.SYSTEM_CPU_USAGE);
                    cpu_collection.add(MetricType.PROCESS_CPU_USAGE);
                    metricsToCollect.addAll(cpu_collection);
                    break;
                case MEMORY:
                    List<MetricType> memory_collection = new ArrayList<>();
                    memory_collection.add(MetricType.SYSTEM_MEMORY_USAGE_PERCENTAGE);
                    memory_collection.add(MetricType.PROCESS_MEMORY_USAGE_PERCENTAGE);
                    memory_collection.add(MetricType.SYSTEM_MEMORY_USAGE);
                    memory_collection.add(MetricType.PROCESS_MEMORY_USAGE);

                    metricsToCollect.addAll(memory_collection);
                    break;
                case NETWORK:
                    List<MetricType> network_collection = new ArrayList<>();
                    network_collection.add(MetricType.SYSTEM_NETWORK_WRITE_BYTES);
                    network_collection.add(MetricType.SYSTEM_NETWORK_WRITE_PACKETS);
                    network_collection.add(MetricType.SYSTEM_NETWORK_READ_BYTES);
                    network_collection.add(MetricType.SYSTEM_NETWORK_READ_PACKETS);
                    metricsToCollect.addAll(network_collection);
                    break;
            }
        }
        return metricsToCollect;
    }


    /**
     * Gets the number of CPUs in the system
     * This is calculated by counting the number of lines in /proc/stat that start with cpu
     * The first line is not counted because it is the total CPU usage
     *
     * @return number of CPUs in the system, or -1 if a failure occurred
     */
    private int getNumCPUs() {
        BufferedReader reader;
        try {
            reader = new BufferedReader((new FileReader(PROCSTAT)));
            String line;
            int n_cpus = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("cpu")) {
                    n_cpus++;
                }
            }
            reader.close();

            //Remove the first cpu line
            return n_cpus - 1;

        } catch (IOException e) {
            logger.error(e.getMessage());
            return -1;
        }
    }

    public static class MemInfo {
        public long memTotal;
        public long memAvailable;

        public MemInfo(long memTotal, long memAvailable) {
            this.memTotal = memTotal;
            this.memAvailable = memAvailable;
        }
    }


    /**
     * Gets the system memory usage as a percentage
     * This is calculated by subtracting the MemAvailable from MemTotal and dividing by MemTotal
     *
     * @return system memory usage as a percentage, or -1 if a failure occurred
     */
    public int getSystemUsedMemoryPercentage() {
        MemInfo memInfo = getSystemMemoryAbsolute();
        if (memInfo.memTotal == -1 || memInfo.memAvailable == -1) {
            return -1;
        }
        return (int) ((memInfo.memTotal - memInfo.memAvailable) * 100 / memInfo.memTotal);
    }

    public MemInfo getSystemMemoryAbsolute() {
        BufferedReader reader;
        try {
            reader = new BufferedReader((new FileReader(PROCMEMINFO)));
            String mem_total_str;
            mem_total_str = reader.readLine().replace("MemTotal:", "").replace("kB", "").trim();
            long mem_total = Long.parseLong(mem_total_str);
            reader.readLine();
            long mem_available = Long.parseLong(reader.readLine().replace("MemAvailable:", "").replace("kB", "").trim());
            reader.close();

            return new MemInfo(mem_total, mem_available);

        } catch (IOException e) {
            logger.error(e.getMessage());
            return new MemInfo(-1, -1);
        }
    }


    /**
     * Return the system uptime in clock_ticks
     *
     * @return system uptime in clock_ticks, or -1 if a failure occurred
     */
    private long getSystemUptime() {
        BufferedReader reader;
        try {
            reader = new BufferedReader((new FileReader(PROCUPTIME)));
            String uptime = reader.readLine().split(" ")[0];
            reader.close();
            return (long) (Double.parseDouble(uptime) * CLK_TCK);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return -1;
        }
    }


    /**
     * Given the process PID return the process running time
     * <p>
     * Calculated by subtracting process starttime from system uptime in clock_ticks
     *
     * @param pid process PID
     * @return process running time in clock_ticks, or -1 if a failure occurred
     */
    private long getProcessRunningTime(String pid) {
        //Here we assume there are 100 CLK_TCK in a second
        BufferedReader reader;
        try {
            long uptime_clock_ticks = this.getSystemUptime();
            if (uptime_clock_ticks == -1) {
                return -1;
            }
            reader = new BufferedReader((new FileReader(PROC + pid + "/stat")));
            String[] stat = reader.readLine().split(" ");
            long starttime = Long.parseLong(stat[21]);

            reader.close();
            return uptime_clock_ticks - starttime;

        } catch (Exception e) {
            logger.error(e.getMessage());
            return -1;
        }
    }


    /**
     * Return the memory usage as a percentage of the JVM memory
     *
     * @return used memory percentage
     */
    public int getProcessMemoryUsagePercentage() {
        MemInfo processMemoryInfo = getProcessMemoryUsage();
        return (int) ((processMemoryInfo.memTotal - processMemoryInfo.memAvailable) * 100 / processMemoryInfo.memTotal);
    }


    /**
     * Return the MemInfo containing the info about total memory available to the JVM, and the current free memory<br>
     * Information is in KBytes
     *
     * @return MemInfo  containing the info about current JVM
     */
    public MemInfo getProcessMemoryUsage() {
        return new MemInfo(Runtime.getRuntime().totalMemory() / 1000, Runtime.getRuntime().freeMemory() / 1000);
    }


    /**
     * Return the current CPU usage by the process as a percentage,
     * where cpu_usage = utime + stime / process_running_time
     * <p>
     * If previous measurement, cpu_usage = delta(last_utime + last_stime) / delta(process_running_time),
     * which gives us the CPU utilization in the last time interval
     * <p>
     * In a multi-core system, the CPU usage can be above 100% if the process is using more than one core
     * To obtain a percentage bounded by 100%, use the processCPUUsageBounded() method
     *
     * @return current CPU usage by the process as a percentage, or -1 if a failure occurred
     */
    public int processCPUUsage() {
        long process_running_in_clk_ticks = this.getProcessRunningTime(pid);
        if (process_running_in_clk_ticks == -1) {
            return -1;
        }

        BufferedReader reader;

        try {
            reader = new BufferedReader((new FileReader(PROC + pid + "/stat")));
            String[] stat = reader.readLine().split(" ");
            long utime = Long.parseLong(stat[13]);
            long stime = Long.parseLong(stat[14]);
            reader.close();


            ProcessCPUUsageRecord current_process_cpu_usage = new ProcessCPUUsageRecord(utime + stime, process_running_in_clk_ticks);

            long delta_active_time = current_process_cpu_usage.getActive_time() - last_process_cpu_usage.getActive_time();
            long delta_elapsed_time = current_process_cpu_usage.getElapsed_time() - last_process_cpu_usage.getElapsed_time();


            int cpu_usage = (int) (delta_active_time * 100 / (delta_elapsed_time));
            last_process_cpu_usage = current_process_cpu_usage;
            return cpu_usage;


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the current CPU usage by the process as a percentage, bounded by 100%
     * <p>
     * For more information, see processCPUUsage()
     *
     * @return current CPU usage by the process as a percentage, bounded by 100%, or -1 if a failure occurred
     */
    public int processCPUUsageBounded() {
        int cpu_usage = processCPUUsage();
        if (cpu_usage == -1) {
            return -1;
        }
        return cpu_usage / n_cpus;
    }

    /**
     * Gets the system CPU usage as a percentage
     * Calculated by subtracting the idle time from the total time and dividing by the total time
     * If a previous measurement was made, the current CPU usage is calculated by subtracting the previous CPU usage from the current CPU usage,
     * so that we know the CPU utilization in the last time interval
     *
     * @return system CPU usage as a percentage
     */
    //https://github.com/htop-dev/htop/blob/15652e7b8102e86b3405254405d8ee5d2a239004/linux/LinuxProcessList.c#L1261
    public int systemCPUUsage() {
        BufferedReader reader;

        try {
            reader = new BufferedReader((new FileReader(PROCSTAT)));
            String[] stat = reader.readLine().split(" ");
            reader.close();

            CPUUsageRecord current_system_cpu_usage = getCpuUsageRecord(stat);

            long total_delta = current_system_cpu_usage.getTotal_time() - last_system_cpu_usage.getTotal_time();
            long idle_delta = current_system_cpu_usage.getIdle_time() - last_system_cpu_usage.getIdle_time();

            int cpu_usage = (int) ((total_delta - idle_delta) * 100 / total_delta);

            last_system_cpu_usage = current_system_cpu_usage;
            return cpu_usage;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static CPUUsageRecord getCpuUsageRecord(String[] stat) {
        long user_time = Long.parseLong(stat[2]);
        long nice_time = Long.parseLong(stat[3]);
        long system_time = Long.parseLong(stat[4]);
        long idle_time = Long.parseLong(stat[5]);
        long iowait_time = Long.parseLong(stat[6]);
        long irq_time = Long.parseLong(stat[7]);
        long softirq_time = Long.parseLong(stat[8]);
        long steal_time = Long.parseLong(stat[9]);
        long guest_time = Long.parseLong(stat[10]);
        long guest_nice_time = Long.parseLong(stat[11]);

        user_time -= guest_time;
        nice_time -= guest_nice_time;
        long idlealltime = idle_time + iowait_time;
        long systemalltime = system_time + irq_time + softirq_time;
        long virtalltime = guest_time + guest_nice_time;
        long totaltime = user_time + nice_time + systemalltime + idlealltime + steal_time + virtalltime;

        return new CPUUsageRecord(totaltime, idlealltime);
    }

    /**
     * Gets the system load average for the last minute
     * The load average denotes the average number of processes that are in a runnable state
     * If the load average is equal to the number of processors, then the system is said to be 100% busy
     *
     * @return system load average for the last minute
     */
    public double getSystemLoadAverage_Minute() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(PROCLOADAVG));
            String[] loadavg = reader.readLine().split(" ");
            reader.close();
            return Double.parseDouble(loadavg[0]);
        } catch (IOException e) {
            logger.error(e.getMessage());
            return -1;
        }
    }


    /**
     * Gets the system load average for the last minute, as a percentage
     * This is calculated by dividing the load average by the number of processors
     * The load average denotes the average number of processes that are in a runnable state
     * A system is overloaded if the value is greater than 100
     *
     * @return system load average for the last minute, as a percentage
     */
    public int getSystemLoadAverage_Minute_Percentage() {
        double load_avg = getSystemLoadAverage_Minute();
        if (load_avg == -1) {
            return -1;
        }
        return (int) (load_avg / n_cpus * 100);
    }


    /**
     * Returns the number of bytes written to disk by the system <br>
     * This is done by multiplying the number of sectors written by the block size (512 bytes)
     * Currently only supports nvme0n1 and sda disks
     *
     * @return number of bytes written to disk by the system, or -1 if a failure occurred
     */
    public long getSystemDiskWriteBytes() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(PROC + "diskstats"));
            String line;
            long write_sectors = 0;
            while ((line = reader.readLine()) != null) {
                if (line.matches("nvme0n\\d\\s") || line.matches("sda\\s")) {
                    String[] split = line.split(" ");
                    write_sectors = Long.parseLong(split[9]);
                }
            }
            reader.close();
            return write_sectors * BLKSIZE;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return -1;
        }
    }

    public long getSystemDiskReadBytes() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(PROC + "diskstats"));
            String line;
            long read_sectors = 0;
            while ((line = reader.readLine()) != null) {
                if (line.matches("nvme0n\\d\\s") || line.matches("sda\\s")) {
                    String[] split = line.split(" ");
                    read_sectors = Long.parseLong(split[5]);
                }
            }
            reader.close();
            return read_sectors * BLKSIZE;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return -1;
        }
    }

    /**
     * Returns the number of bytes written to network by the system, per interface
     *
     * @return map with number of bytes read from network by the system, per interface, or null if a failure occurred
     */
    public Map<String, Long> getSystemNetworkWriteBytes() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(PROC + "net/dev"));
            String line;
            Map<String, Long> write_bytes = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                //Exclude lines that are not interfaces or local interface (lo)
                if (line.matches("(.*?):(.*?)") && !line.trim().startsWith("lo:")) {
                    String[] split = line.split(":");
                    String iface = split[0].trim();
                    String[] stats = split[1].trim().split("(\\s)+");
                    long bytes = Long.parseLong(stats[0]);
                    if (bytes > 0)
                        write_bytes.put(iface, bytes);
                }
            }

            return write_bytes;

        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * Returns the number of packets written to network by the system, per interface
     *
     * @return map with number of packets read from network by the system, per interface, or null if a failure occurred
     */
    public Map<String, Long> getSystemNetworkWritePackets() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(PROC + "net/dev"));
            String line;
            Map<String, Long> write_packets = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                //Exclude lines that are not interfaces or local interface (lo)
                if (line.matches("(.*?):(.*?)") && !line.trim().startsWith("lo:")) {
                    String[] split = line.split(":");
                    String iface = split[0].trim();
                    String[] stats = split[1].trim().split("(\\s)+");
                    long packets = Long.parseLong(stats[1]);
                    if (packets > 0)
                        write_packets.put(iface, packets);
                }
            }

            return write_packets;

        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }


    public Map<String, Long> getSystemNetworkReadBytes() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(PROC + "net/dev"));
            String line;
            Map<String, Long> read_bytes = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                //Exclude lines that are not interfaces or local interface (lo)
                if (line.matches("(.*?):(.*?)") && !line.trim().startsWith("lo:")) {
                    String[] split = line.split(":");
                    String iface = split[0].trim();
                    String[] stats = split[1].trim().split("(\\s)+");
                    long bytes = Long.parseLong(stats[8]);
                    if (bytes > 0)
                        read_bytes.put(iface, bytes);
                }
            }

            return read_bytes;

        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public Map<String, Long> getSystemNetworkReadPackets() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(PROC + "net/dev"));
            String line;
            Map<String, Long> read_packets = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                //Exclude lines that are not interfaces or local interface (lo)
                if (line.matches("(.*?):(.*?)") && !line.trim().startsWith("lo:")) {
                    String[] split = line.split(":");
                    String iface = split[0].trim();
                    String[] stats = split[1].trim().split("(\\s)+");
                    long packets = Long.parseLong(stats[9]);
                    if (packets > 0)
                        read_packets.put(iface, packets);
                }
            }

            return read_packets;

        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }


    public long getProcessDiskWriteBytes() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(PROC + pid + "/io"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("write_bytes:")) {
                    return Long.parseLong(line.split(" ")[1]);
                }
            }
            reader.close();
            return -1;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return -1;
        }
    }

    public long getProcessDiskWriteNum() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(PROC + pid + "/io"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("syscw:")) {
                    return Long.parseLong(line.split(" ")[1]);
                }
            }
            reader.close();
            return -1;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return -1;
        }
    }

    public long getProcessDiskReadBytes() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(PROC + pid + "/io"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("read_bytes:")) {
                    return Long.parseLong(line.split(" ")[1]);
                }
            }
            reader.close();
            return -1;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return -1;
        }
    }

    public long getProcessDiskReadNum() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(PROC + pid + "/io"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("syscr:")) {
                    return Long.parseLong(line.split(" ")[1]);
                }
            }
            reader.close();
            return -1;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return -1;
        }
    }

    public Sample[] getMemorySample(MemInfo memInfo) {
        Sample[] samples = new Sample[2];
        samples[0] = new Sample(memInfo.memTotal, MEMORY_LABEL_NAMES, new String[]{"total"});
        samples[1] = new Sample(memInfo.memAvailable, MEMORY_LABEL_NAMES, new String[]{"available"});
        return samples;
    }
}


//    protected MetricSample collect() {
//        List<String> labels = new ArrayList<>();
//        List<Double> samples = new ArrayList<>();
//
//        for (MetricType metricType : metricsToCollect) {
//        switch (metricType) {
//            case SYSTEM_CPU_USAGE:
//                return new MetricSample("percentage", "system_cpu_usage", new String[]{"system_cpu_usage"}, new double[]{systemCPUUsage()}, 1);
//            case SYSTEM_LOAD_AVERAGE:
//                return new MetricSample("load_average", "system_load_average", new String[]{"system_load_average"}, new double[]{getSystemLoadAverage_Minute()}, 1);
//            case SYSTEM_MEMORY_USAGE:
//                return new MetricSample("percentage", "system_memory_usage", new String[]{"system_memory_usage"}, new double[]{getSystemUsedMemory()}, 1);
//            case PROCESS_CPU_USAGE:
//                return new MetricSample("percentage", "process_cpu_usage", new String[]{"process_cpu_usage"}, new double[]{processCPUUsage()}, 1);
//            case PROCESS_MEMORY_USAGE:
//                return new MetricSample("percentage", "process_memory_usage", new String[]{"process_memory_usage"}, new double[]{getMemoryUsage()}, 1);
//            case SYSTEM_DISK_WRITE_BYTES:
//                return new MetricSample("bytes", "system_disk_write_bytes", new String[]{"system_disk_write_bytes"}, new double[]{getSystemDiskWriteBytes()}, 1);
//            case SYSTEM_DISK_WRITE_NUM:
//                return new MetricSample("number", "system_disk_write_num", new String[]{"system_disk_write_num"}, new double[]{getSystemDiskWriteBytes()}, 1);
//            case SYSTEM_DISK_READ_BYTES:
//                return new MetricSample("bytes", "system_disk_read_bytes", new String[]{"system_disk_read_bytes"}, new double[]{getSystemDiskReadBytes()}, 1);
//            case SYSTEM_DISK_READ_NUM:
//                return new MetricSample("number", "system_disk_read_num", new String[]{"system_disk_read_num"}, new double[]{getSystemDiskReadNum()}, 1);
//            case PROCESS_DISK_WRITE_BYTES:
//                return new MetricSample("bytes", "process_disk_write_bytes", new String[]{"process_disk_write_bytes"}, new double[]{getProcessDiskWriteBytes()}, 1);
//            case PROCESS_DISK_WRITE_NUM:
//                return new MetricSample("number", "process_disk_write_num", new String[]{"process_disk_write_num"}, new double[]{getProcessDiskWriteNum()}, 1);
//            case PROCESS_DISK_READ_BYTES:
//                return new MetricSample("bytes", "process_disk_read_bytes", new String[]{"process_disk_read_bytes"}, new double[]{getProcessDiskReadBytes()}, 1);
/// /            case PROCESS_DISK_READ_NUM:
/// /                return new Metric
//
//
//
//        }
//        }
//    }


//    public static void main(String[] args) throws InterruptedException, NoProcfsException {
//
//        Metric c = new Counter("test", "test");
//
//        Counter d = MetricFactory.ServerClient.Server.n_requests();
//
//        OSMetrics osm = new OSMetrics();
//        System.out.println("PID: " + osm.pid);
//        System.out.println("Number of CPUs: " + osm.n_cpus);
//        System.out.println("System uptime: " + osm.getSystemUptime());
//        System.out.println("Process running time: " + osm.getProcessRunningTime(osm.pid));
//       for (int i = 0; i < 1000000000; i++) {
//            System.out.println("System CPU usage: " + osm.systemCPUUsage());
//            System.out.println("Spotify CPU usage: " + osm.processCPUUsage());
//            System.out.println("Memory usage: " + osm.getMemoryUsage());
//            System.out.println("System load average: " + osm.getSystemLoadAverage_Minute());
//            System.out.println("System load average percentage: " + osm.getSystemLoadAverage_Minute_Percentage());
//            System.out.println("System memory usage: " + osm.getSystemUsedMemory());
//            System.out.println("------------------");
//            System.out.println("System network write bytes: " + osm.getSystemNetworkWriteBytes());
//            System.out.println("System network write packets: " + osm.getSystemNetworkWritePackets());
//            System.out.println("System network read bytes: " + osm.getSystemNetworkReadBytes());
//            System.out.println("System network read packets: " + osm.getSystemNetworkReadPackets());
//           // System.out.println("System network write packets: " + osm.getSystemNetworkWritePackets());
//            sleep(2000);
//        }
//
//    }


//public static void main(String[] args) throws FileNotFoundException {
//    BufferedReader reader;
//    try {
//        reader = new BufferedReader(new FileReader(PROC + "net/dev"));
//        String line;
//        Map<String, Long> write_bytes = new HashMap<>();
//        while ((line = reader.readLine()) != null) {
//            //Exclude lines that are not interfaces
//            //System.out.println(line);
//            if (line.matches("(.*?):(.*?)") || line.trim().startsWith("lo:")) {
//                System.out.println(line);
//            }
//        }
//    } catch (IOException e) {
//        throw new RuntimeException(e);
//    }
//}
//}