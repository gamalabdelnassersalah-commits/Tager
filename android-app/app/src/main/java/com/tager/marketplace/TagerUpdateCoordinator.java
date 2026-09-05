package com.tager.marketplace;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import java.lang.ref.WeakReference;

final class TagerUpdateCoordinator {
    private static final int UPDATE_REQUEST_CODE = 6202;
    private static final String PREFS = "tager_update_state";
    private static final String KEY_LAST_CHECK_AT = "last_check_at";
    private static final long CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L;

    private final AppUpdateManager updateManager;
    private final InstallStateUpdatedListener installListener;
    private final SharedPreferences preferences;
    private WeakReference<Activity> activityRef = new WeakReference<>(null);
    private boolean updateFlowStarted;

    TagerUpdateCoordinator(TagerApplication application) {
        updateManager = AppUpdateManagerFactory.create(application);
        preferences = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        installListener = state -> {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                Activity activity = activityRef.get();
                if (activity != null && !activity.isFinishing()) {
                    Toast.makeText(
                            activity,
                            "تم تنزيل تحديث تاجر — سيتم تثبيته الآن",
                            Toast.LENGTH_LONG).show();
                }
                updateManager.completeUpdate();
            }
        };
        updateManager.registerListener(installListener);
    }

    void onActivityResumed(Activity activity) {
        if (!(activity instanceof TagerActivity)) return;
        activityRef = new WeakReference<>(activity);
        if (updateFlowStarted) {
            resumeUpdateIfNeeded(activity);
            return;
        }
        long now = System.currentTimeMillis();
        long lastCheck = preferences.getLong(KEY_LAST_CHECK_AT, 0L);
        if (now - lastCheck < CHECK_INTERVAL_MS) return;
        preferences.edit().putLong(KEY_LAST_CHECK_AT, now).apply();
        checkForUpdate(activity);
    }

    void onActivityPaused(Activity activity) {
        Activity current = activityRef.get();
        if (current == activity) activityRef.clear();
    }

    void shutdown() {
        updateManager.unregisterListener(installListener);
        activityRef.clear();
    }

    private void checkForUpdate(Activity activity) {
        updateManager.getAppUpdateInfo()
                .addOnSuccessListener(info -> {
                    if (activity.isFinishing()) return;
                    if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        startFlexibleUpdate(activity, info);
                    } else if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        updateManager.completeUpdate();
                    }
                });
    }

    private void resumeUpdateIfNeeded(Activity activity) {
        updateManager.getAppUpdateInfo()
                .addOnSuccessListener(info -> {
                    if (activity.isFinishing()) return;
                    if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                            && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        startFlexibleUpdate(activity, info);
                    } else if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        updateManager.completeUpdate();
                    } else {
                        updateFlowStarted = false;
                    }
                });
    }

    private void startFlexibleUpdate(Activity activity, AppUpdateInfo info) {
        if (updateFlowStarted
                && info.updateAvailability() != UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            return;
        }
        try {
            updateFlowStarted = true;
            updateManager.startUpdateFlowForResult(
                    info,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    UPDATE_REQUEST_CODE);
        } catch (IntentSender.SendIntentException | RuntimeException error) {
            updateFlowStarted = false;
        }
    }
}
