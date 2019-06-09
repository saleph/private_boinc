package edu.berkeley.boinc;

import android.app.Activity;
import android.content.*;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Memorycostamcostam extends Fragment {

    private final Activity context;
    private AtomicBoolean isCanvasLocked = new AtomicBoolean(false);
    private TextView memTotal;
    private TextView memUsed;
    private TextView memAvailable;
    private TextView memFree;
    private TextView cached;
    private TextView threshold;
    private TextView memUsedPercentage;
    private TextView memAvailablePercentage;
    private TextView memFreePercentage;
    private TextView cachedPercentage;
    private TextView thresholdPercentage;

    private DecimalFormat memoryTakenFormat = new DecimalFormat("##,###,##0");
    private DecimalFormat memoryTakenPercentageFormat = new DecimalFormat("##0.0");

    private ViewGraphic plotViewGraphic;
    private ReaderService readerService;
    private Handler memoryTextViewsRefreshHandler = new Handler();
    private Handler plotRefreshHandler = new Handler();
    private Runnable drawRunnableGraphic = new Runnable() {
        private Thread drawThread;

        @Override
        public void run() {
            plotRefreshHandler.postDelayed(drawRunnableGraphic, C.defaultIntervalRead);
            drawThread = new Thread() {
                @Override
                public void run() {
                    if (isCanvasLocked.getAndSet(true)) {
                        return;
                    }
                    Canvas canvas = plotViewGraphic.lockCanvas();
                    if (canvas == null) {
                        isCanvasLocked.set(false);
                        return;
                    }
                    plotViewGraphic.onDrawCustomised(canvas, drawThread);
                    try {
                        plotViewGraphic.unlockCanvasAndPost(canvas);
                    } catch (IllegalStateException e) {
                        Log.w("Activity main: ", e.getMessage());
                    }
                    isCanvasLocked.set(false);
                }
            };
            drawThread.start();
        }
    };
    private Runnable drawRunnable = new Runnable() {
        @Override
        public void run() {
            memoryTextViewsRefreshHandler.postDelayed(this, C.defaultIntervalUpdate);
            if (readerService == null) {
                return;
            }
            setTextLabelMemory(memUsed, memUsedPercentage, readerService.getMemUsed());
            setTextLabelMemory(memAvailable, memAvailablePercentage, readerService.getMemAvailable());
            setTextLabelMemory(memFree, memFreePercentage, readerService.getMemFree());
            setTextLabelMemory(cached, cachedPercentage, readerService.getCached());
            setTextLabelMemory(threshold, thresholdPercentage, readerService.getThreshold());
        }
    };
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            readerService = ((ReaderService.ReaderServiceBinder) service).getService();
            plotViewGraphic.setService(readerService);
            memTotal.setText(String.format("%s%s", memoryTakenFormat.format(readerService.getMemTotal()), C.kB));
            memoryTextViewsRefreshHandler.removeCallbacks(drawRunnable);
            plotRefreshHandler.removeCallbacks(drawRunnableGraphic);
            memoryTextViewsRefreshHandler.post(drawRunnable);
            plotRefreshHandler.post(drawRunnableGraphic);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            readerService = null;
        }
    };

    public Memorycostamcostam(Activity context) {
        super();
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View layout = inflater.inflate(R.layout.memory_statistics_fragment, container, false);
        plotViewGraphic = layout.findViewById(R.id.ANGraphic);
        memTotal = layout.findViewById(R.id.TVMemTotal);
        memUsed = layout.findViewById(R.id.TVMemUsed);
        memUsedPercentage = layout.findViewById(R.id.TVMemUsedP);
        memAvailable = layout.findViewById(R.id.TVMemAvailable);
        memAvailablePercentage = layout.findViewById(R.id.TVMemAvailableP);
        memFree = layout.findViewById(R.id.TVMemFree);
        memFreePercentage = layout.findViewById(R.id.TVMemFreeP);
        cached = layout.findViewById(R.id.TVCached);
        cachedPercentage = layout.findViewById(R.id.TVCachedP);
        threshold = layout.findViewById(R.id.TVThreshold);
        thresholdPercentage = layout.findViewById(R.id.TVThresholdP);
        plotViewGraphic.setOpaque(false);
        return layout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        context.bindService(new Intent(context, ReaderService.class), mServiceConnection, 0);
    }

    @Override
    public void onStop() {
        super.onStop();
        context.unbindService(mServiceConnection);
    }

    private void setTextLabelMemory(TextView absoluteMemoryValue, TextView percentageMemoryValue, Collection<Long> valuesCol) {
        List<Long> values = new ArrayList<>(valuesCol);
        if (values.isEmpty()) {
            return;
        }
        final long lastReadValue = values.get(values.size() - 1);
        String absoluteMemory = String.format("%s%s", memoryTakenFormat.format(lastReadValue), C.kB);
        absoluteMemoryValue.setText(absoluteMemory);

        final float percentage = lastReadValue * 100 / (float) readerService.getMemTotal();
        String percentageMemory = String.format("%s%s", memoryTakenPercentageFormat.format(percentage), C.percent);
        percentageMemoryValue.setText(percentageMemory);
    }
}