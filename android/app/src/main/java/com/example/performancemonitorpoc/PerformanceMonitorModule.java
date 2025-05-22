package com.example.performancemonitorpoc;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class PerformanceMonitorModule extends ReactContextBaseJavaModule {
    private static final String TAG = "PerfMonitorModule";
    private static final int USAGE_STATS_PERMISSION_REQ_CODE = 5678;
    private Promise mUsageStatsPermissionPromise;

    public PerformanceMonitorModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(mActivityEventListener);
    }

    @Override
    public String getName() {
        return "PerformanceMonitorModule";
    }

    @ReactMethod
    public void startService() {
        ReactApplicationContext context = getReactApplicationContext();
        Intent serviceIntent = new Intent(context, PerformanceMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        Log.d(TAG, "PerformanceMonitorService start requested");
    }

    @ReactMethod
    public void stopService() {
        ReactApplicationContext context = getReactApplicationContext();
        context.stopService(new Intent(context, PerformanceMonitorService.class));
        Log.d(TAG, "PerformanceMonitorService stop requested");
    }

    @ReactMethod
    public void requestUsageStatsPermission(Promise promise) {
        mUsageStatsPermissionPromise = promise;
        ReactApplicationContext context = getReactApplicationContext();
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());

        if (mode == AppOpsManager.MODE_ALLOWED) {
            promise.resolve(true);
            return;
        }

        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {
            try {
                 // This intent usually takes the user to the list of apps that have usage access.
                 // The user then needs to find this app and enable it.
                currentActivity.startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), USAGE_STATS_PERMISSION_REQ_CODE);
            } catch (Exception e) {
                Log.e(TAG, "Error starting ACTION_USAGE_ACCESS_SETTINGS activity", e);
                Toast.makeText(context, "Could not open Usage Access Settings. Please enable manually.", Toast.LENGTH_LONG).show();
                promise.reject("E_ACTIVITY_NOT_FOUND", "Could not open Usage Access Settings", e);
                 mUsageStatsPermissionPromise = null; // Clear promise if activity fails to start
            }
        } else {
            promise.reject("E_ACTIVITY_DOES_NOT_EXIST", "Activity doesn't exist");
            mUsageStatsPermissionPromise = null;
        }
    }

     private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (requestCode == USAGE_STATS_PERMISSION_REQ_CODE) {
                if (mUsageStatsPermissionPromise != null) {
                    ReactApplicationContext context = getReactApplicationContext();
                    AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                    int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                            android.os.Process.myUid(), context.getPackageName());
                    if (mode == AppOpsManager.MODE_ALLOWED) {
                        mUsageStatsPermissionPromise.resolve(true);
                    } else {
                        mUsageStatsPermissionPromise.resolve(false); // User backed out or didn't grant
                    }
                    mUsageStatsPermissionPromise = null;
                }
            }
        }
    };
}
