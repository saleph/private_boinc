package edu.berkeley.boinc;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import android.os.Process;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class MemoryUsageCollector {
    private static final int INITIAL_DELAY = 0;
    private final int periodInMs;
    private final int samplesBufferSize;
    private final Debug.MemoryInfo[] activityManagerMemoryInfo;

    private boolean firstRead = true;
    private Long memTotal;
    private Buffer memUsed;
    private Buffer memAvailable;
    private Buffer memFree;
    private Buffer cached;
    private Buffer threshold;
    private ActivityManager activityManager;
    private ActivityManager.MemoryInfo activityMemoryInfo;

    private Buffer memoryAM;
    private ScheduledFuture<?> collectorHandle;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private List<MemoryUsageSampleAddedListener> listeners = new ArrayList<>();


    MemoryUsageCollector(Context context, int periodInMs, int samplesBufferSize) {
        this.periodInMs = periodInMs;
        this.samplesBufferSize = samplesBufferSize;
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.activityMemoryInfo = new ActivityManager.MemoryInfo();
        this.activityManagerMemoryInfo = activityManager.getProcessMemoryInfo(new int[]{Process.myPid()});
    }

    /**
     * Start collecting samples. Previously collected samples will be discarded.
     */
    void start() {
        memoryAM = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(samplesBufferSize));
        memUsed = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(samplesBufferSize));
        memAvailable = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(samplesBufferSize));
        memFree = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(samplesBufferSize));
        cached = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(samplesBufferSize));
        threshold = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(samplesBufferSize));
        final Runnable collector = new Runnable() {
            public void run() {
                Log.i("sample", "sample collection");
                try {
                    read();
                } catch (Exception e) {
                    Log.i("sample", Log.getStackTraceString(e));
                }
            }
        };
        collectorHandle = scheduler.scheduleAtFixedRate(collector, INITIAL_DELAY, periodInMs,
                                                        TimeUnit.MILLISECONDS);
    }

    /**
     * Stop collecting samples.
     */
    void stop() {
        if (collectorHandle != null && !collectorHandle.isDone()) {
            collectorHandle.cancel(true);
        }
    }

    private void read() {
        try {
            File file = new File("/proc/meminfo");
            String[] memInfoContent = FileUtils.readFileToString(file, Charsets.UTF_8).split(
                    "[\\n]");
            for (String s : memInfoContent) {
                // Memory values. Percentages are calculated in the ActivityMain class.
                if (firstRead && s.startsWith("MemTotal:")) {
                    memTotal = Long.parseLong(s.split("[ ]+", 3)[1]);
                    firstRead = false;
                } else if (s.startsWith("MemFree:"))
                    memFree.add(Long.valueOf(s.split("[ ]+", 3)[1]));
                else if (s.startsWith("Cached:"))
                    cached.add(Long.valueOf(s.split("[ ]+", 3)[1]));
            }
            // http://stackoverflow.com/questions/3170691/how-to-get-current-memory-usage-in-android
            activityManager.getMemoryInfo(activityMemoryInfo);
            if (activityMemoryInfo == null) {
                memUsed.add(0L);
                memAvailable.add(0L);
                threshold.add(0L);
            } else {
                memUsed.add(memTotal - activityMemoryInfo.availMem / 1024);
                memAvailable.add(activityMemoryInfo.availMem / 1024);
                threshold.add(activityMemoryInfo.threshold / 1024);
            }
            memoryAM.add((long) activityManagerMemoryInfo[0].getTotalPrivateDirty());
        } catch (Exception e) {
            e.printStackTrace();
        }
        notifyMemoryUsageSampleAddedListeners();
    }

    private void notifyMemoryUsageSampleAddedListeners() {
        for (MemoryUsageSampleAddedListener listener : listeners) {
            listener.onMemoryUsageSampleAdded();
        }
    }

    /**
     * Add callback for new samples.
     *
     * @param listener callback function to add
     */
    void registerMemoryUsageSampleAddedListener(MemoryUsageSampleAddedListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Remove callback for new samples.
     *
     * @param listener callback function to remove
     */
    public void unregisterMemoryUsageSampleAddedListener(MemoryUsageSampleAddedListener listener) {
        this.listeners.remove(listener);
    }

    Collection<Long> getMemoryAM() {
        return memoryAM;
    }

    Long getMemTotal() {
        return memTotal;
    }

    Collection<Long> getMemUsed() {
        return memUsed;
    }

    Collection<Long> getMemAvailable() {
        return memAvailable;
    }

    Collection<Long> getMemFree() {
        return memFree;
    }

    Collection<Long> getCached() {
        return cached;
    }

    Collection<Long> getThreshold() {
        return threshold;
    }

    int getIntervalRead() {
        return periodInMs;
    }

    int getIntervalUpdate() {
        return periodInMs;
    }

    double getIntervalWidthInSeconds() {
        return periodInMs /1000.0;
    }
}
