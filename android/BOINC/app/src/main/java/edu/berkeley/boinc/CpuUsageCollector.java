package edu.berkeley.boinc;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.buffer.CircularFifoBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.collections.BufferUtils.synchronizedBuffer;

/**
 * Cpu usage collector. Statistics are based on values stored in /proc/stat system file.
 */
@SuppressWarnings("unchecked")
public class CpuUsageCollector {
    private static final int INITIAL_DELAY = 0;
    private static final int SAMPLES_BUFFER_SIZE_DEFAULT = 1000;
    private static final String PROC_STAT_FILE = "/proc/stat";
    private final long periodInMs;
    private final int samplesBufferSize;
    private long totalCpuCycles;
    private long previousTotalCpuCycles;
    private long cpuCyclesSpentWorking;
    private long previousCpuCyclesSpentWorking;

    private Buffer cpuUsagePercentageSamples;
    private ScheduledFuture<?> collectorHandle;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private List<CpuUsageSampleAddedListener> listeners = new ArrayList<>();

    /**
     * Construct collector.
     *
     * @param periodInMs read period in miliseconds
     */
    CpuUsageCollector(long periodInMs) {
        this(periodInMs, SAMPLES_BUFFER_SIZE_DEFAULT);
    }

    /**
     * Construct collector.
     *
     * @param periodInMs        read period in miliseconds
     * @param samplesBufferSize size of the circular buffer for samples
     */
    CpuUsageCollector(long periodInMs, int samplesBufferSize) {
        this.periodInMs = periodInMs;
        this.samplesBufferSize = samplesBufferSize;
    }

    /**
     * Add callback for new samples.
     *
     * @param listener callback function to add
     */
    public void registerCpuUsageSampleAddedListener(CpuUsageSampleAddedListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Remove callback for new samples.
     *
     * @param listener callback function to remove
     */
    public void unregisterCpuUsageSampleAddedListener(CpuUsageSampleAddedListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Start collecting samples. Previously collected samples will be discarded.
     */
    public void start() {
        cpuUsagePercentageSamples = synchronizedBuffer(new CircularFifoBuffer(samplesBufferSize));
        final Runnable collector = new Runnable() {
            public void run() {
                collectCpuUsage();
            }
        };
        collectorHandle = scheduler.scheduleAtFixedRate(collector, INITIAL_DELAY, periodInMs,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Stop collecting samples.
     */
    public void stop() {
        if (collectorHandle != null && !collectorHandle.isDone()) {
            collectorHandle.cancel(true);
        }
    }

    /**
     * @return buffer with samples
     */
    public Collection<Float> getcpuUsagePercentageSamples() {
        return cpuUsagePercentageSamples;
    }

    private void collectCpuUsage() {
        parseProcStatFile();
        if (previousTotalCpuCycles != 0) {
            addNewCpuUsage();
            notifyCpuUsageSampleAddedListeners();
        }
        previousCpuCyclesSpentWorking = cpuCyclesSpentWorking;
        previousTotalCpuCycles = totalCpuCycles;
    }

    private void notifyCpuUsageSampleAddedListeners() {
        for (CpuUsageSampleAddedListener listener : listeners) {
            listener.onCpuUsageSampleAdded();
        }
    }

    private void parseProcStatFile() {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(PROC_STAT_FILE))) {
            String lineWithStatisticsForAllCores = reader.readLine();
            parseCpuUsage(lineWithStatisticsForAllCores);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseCpuUsage(String lineWithStatisticsForAllCores) {
        // cpu  3700061 1387288 4050658 15473074 107427 489280 257459 0 0 0
        String multipleWhitespacesRegex = "[ ]+";
        int nineEntriesToMatch = 9;
        String[] position = lineWithStatisticsForAllCores.split(multipleWhitespacesRegex,
                nineEntriesToMatch);
        long user = Long.parseLong(position[1]);
        long nice = Long.parseLong(position[2]);
        long system = Long.parseLong(position[3]);
        cpuCyclesSpentWorking = user + nice + system;

        long idle = Long.parseLong(position[4]);
        long ioWait = Long.parseLong(position[5]);
        long irq = Long.parseLong(position[6]);
        long softIrq = Long.parseLong(position[7]);
        totalCpuCycles = cpuCyclesSpentWorking + idle + ioWait + irq + softIrq;
    }

    private void addNewCpuUsage() {
        long totalCpuCyclesDifference = totalCpuCycles - previousTotalCpuCycles;
        long cpuCyclesSpentWorkingDifference =
                cpuCyclesSpentWorking - previousCpuCyclesSpentWorking;
        float cpuUsageFactor = cpuCyclesSpentWorkingDifference / (float) totalCpuCyclesDifference;
        float percentage = restrictPercentage(cpuUsageFactor * 100);
        cpuUsagePercentageSamples.add(percentage);
    }

    private float restrictPercentage(float percentage) {
        if (percentage > 100)
            return 100;
        else if (percentage < 0)
            return 0;
        else
            return percentage;
    }
}
