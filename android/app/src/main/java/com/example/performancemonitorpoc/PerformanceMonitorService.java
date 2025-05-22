package com.example.performancemonitorpoc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class PerformanceMonitorService extends Service {
    private static final String TAG = "PerfMonitorService";
    private static final String NOTIFICATION_CHANNEL_ID = "PerformanceMonitorChannel";
    private static final int NOTIFICATION_ID = 1;

    private Handler handler;
    private Runnable dataRunnable;
    private boolean isMonitoring = false;
    private UsageStatsManager usageStatsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        handler = new Handler(Looper.getMainLooper());
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        createNotificationChannel();

        dataRunnable = new Runnable() {
            @Override
            public void run() {
                if (isMonitoring) {
                    logBatteryTemperature();
                    logForegroundApp();
                    handler.postDelayed(this, 5000); // Log every 5 seconds
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        if (!isMonitoring) {
            isMonitoring = true;
            startForeground(NOTIFICATION_ID, createNotification());
            Toast.makeText(this, "Performance Monitoring Started", Toast.LENGTH_SHORT).show();
            handler.post(dataRunnable);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        isMonitoring = false;
        handler.removeCallbacks(dataRunnable);
        Toast.makeText(this, "Performance Monitoring Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Performance Monitor Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Performance Monitor Active")
                .setContentText("Monitoring performance data...")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with actual app icon
                .setOngoing(true)
                .build();
    }

    private void logBatteryTemperature() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, intentFilter);

        if (batteryStatus != null) {
            int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            float tempCelsius = temperature / 10.0f;
            Log.d(TAG, "Battery Temperature: " + tempCelsius + "°C");
        } else {
            Log.d(TAG, "Could not get battery status");
        }
    }

    private void logForegroundApp() {
        if (usageStatsManager == null) {
            Log.d(TAG, "UsageStatsManager not available");
            return;
        }

        long time = System.currentTimeMillis();
        // Query events in the last 10 seconds. Adjust time window as needed.
        UsageEvents usageEvents = usageStatsManager.queryEvents(time - 1000 * 10, time);
        UsageEvents.Event event = new UsageEvents.Event();
        String foregroundApp = "No foreground app detected";

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) { // ACTIVITY_RESUMED for more frequent updates
                foregroundApp = event.getPackageName();
            }
        }
        Log.d(TAG, "Current Foreground App: " + foregroundApp);
    }
}
