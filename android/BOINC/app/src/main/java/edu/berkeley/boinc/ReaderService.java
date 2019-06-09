package edu.berkeley.boinc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.util.Collection;

public class ReaderService extends Service {

    private MemoryUsageCollector memoryUsageCollector;

    private BroadcastReceiver receiverClose = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            sendBroadcast(new Intent(C.actionFinishActivity));
            stopSelf();
        }
    };

    class ReaderServiceBinder extends Binder {
        ReaderService getService() {
            return ReaderService.this;
        }
    }

    @Override
    public void onCreate() {
        this.memoryUsageCollector = new MemoryUsageCollector(this, C.defaultIntervalRead, C.sampleBufferSize);
        memoryUsageCollector.registerMemoryUsageSampleAddedListener(new MemoryUsageSampleAddedListener() {
            @Override
            public void onMemoryUsageSampleAdded() {
                handleMemorySample();
            }
        });
        memoryUsageCollector.start();

        registerReceiver(receiverClose, new IntentFilter(C.actionClose));
        startMyOwnForeground();
    }


    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                                                       .setSmallIcon(R.drawable.boinc)
                                                       .setContentTitle("App is collecting device usage statistics")
                                                       .setPriority(NotificationManager.IMPORTANCE_MIN)
                                                       .setCategory(Notification.CATEGORY_SERVICE)
                                                       .build();
        startForeground(2, notification);
    }

    private void handleMemorySample() {
        //log(memoryUsageCollector.getMemFree().toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("on destroy");
        memoryUsageCollector.stop();
        stopForeground(true);
        unregisterReceiver(receiverClose);
    }

    private void log(String msg) {
        Log.i("sample", msg);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ReaderServiceBinder();
    }

    Collection<Long> getMemoryAM() {
        return memoryUsageCollector.getMemoryAM();
    }

    Long getMemTotal() {
        return memoryUsageCollector.getMemTotal() ;
    }

    Collection<Long> getMemUsed() {
        return memoryUsageCollector.getMemUsed();
    }

    Collection<Long> getMemAvailable() {
        return memoryUsageCollector.getMemAvailable();
    }

    Collection<Long> getMemFree() {
        return memoryUsageCollector.getMemFree();
    }

    Collection<Long> getCached() {
        return memoryUsageCollector.getCached();
    }

    Collection<Long> getThreshold() {
        return memoryUsageCollector.getThreshold();
    }

    int getIntervalRead() {
        return memoryUsageCollector.getIntervalRead();
    }

    int getIntervalUpdate() {
        return memoryUsageCollector. getIntervalUpdate();
    }

    double getIntervalWidthInSeconds() {
        return memoryUsageCollector.getIntervalWidthInSeconds();
    }
}
