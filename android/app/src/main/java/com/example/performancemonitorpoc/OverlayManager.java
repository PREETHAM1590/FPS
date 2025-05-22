package com.example.performancemonitorpoc;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.common.annotations.VisibleForTesting;

public class OverlayManager extends ReactContextBaseJavaModule {
    private static final String TAG = "OverlayManager";
    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private Promise mOverlayPermissionPromise;
    private TextView overlayView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams overlayParams;
    private boolean isOverlayVisible = false;

    public OverlayManager(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(mActivityEventListener);
    }

    @Override
    public String getName() {
        return "OverlayManager";
    }

    @ReactMethod
    public void requestOverlayPermission(Promise promise) {
        mOverlayPermissionPromise = promise;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(getReactApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getReactApplicationContext().getPackageName()));
                Activity currentActivity = getCurrentActivity();
                if (currentActivity != null) {
                    currentActivity.startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
                } else {
                    promise.reject("E_ACTIVITY_DOES_NOT_EXIST", "Activity doesn't exist");
                }
            } else {
                promise.resolve(true);
            }
        } else {
            // On Android versions below M, if permission is in manifest, it's granted at install time.
            promise.resolve(true);
        }
    }

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
                if (mOverlayPermissionPromise != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(getReactApplicationContext())) {
                            mOverlayPermissionPromise.resolve(true);
                        } else {
                            mOverlayPermissionPromise.resolve(false); // Or reject with a specific error
                        }
                    } else {
                         mOverlayPermissionPromise.resolve(true); // Should be granted if manifest has it
                    }
                    mOverlayPermissionPromise = null;
                }
            }
        }
    };

    @ReactMethod
    public void toggleOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(getReactApplicationContext())) {
            Toast.makeText(getReactApplicationContext(), "Overlay permission not granted", Toast.LENGTH_LONG).show();
            return;
        }

        ReactApplicationContext context = getReactApplicationContext();
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
             Toast.makeText(context, "No current activity to display overlay", Toast.LENGTH_SHORT).show();
            return;
        }


        currentActivity.runOnUiThread(() -> {
            if (windowManager == null) {
                windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
            }

            if (overlayView == null) {
                overlayView = new TextView(context);
                overlayView.setText("Overlay Active");
                overlayView.setBackgroundColor(Color.argb(128, 255, 0, 0)); // Semi-transparent red
                overlayView.setTextColor(Color.WHITE);
                overlayView.setPadding(20, 20, 20, 20);
                overlayView.setGravity(Gravity.CENTER);

                int layoutFlag;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                } else {
                    layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
                }

                overlayParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        layoutFlag,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        android.graphics.PixelFormat.TRANSLUCENT);
                overlayParams.gravity = Gravity.TOP | Gravity.START;
                overlayParams.x = 100;
                overlayParams.y = 100;
            }

            if (!isOverlayVisible) {
                try {
                    windowManager.addView(overlayView, overlayParams);
                    isOverlayVisible = true;
                    Toast.makeText(context, "Overlay Shown", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                     Toast.makeText(context, "Failed to show overlay: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                try {
                    if (overlayView.isAttachedToWindow()) { // Check if view is still attached
                         windowManager.removeView(overlayView);
                    }
                    isOverlayVisible = false;
                    Toast.makeText(context, "Overlay Hidden", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(context, "Failed to hide overlay: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
