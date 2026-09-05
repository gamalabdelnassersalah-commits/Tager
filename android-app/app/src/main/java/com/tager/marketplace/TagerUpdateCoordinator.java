package com.tager.marketplace;

import android.app.Activity;
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

    private final AppUpdateManager updateManager;
    private WeakReference<Activity> activityRef = new WeakReference<>(null);
    private boolean updateFlowStarted;

    private final InstallStateUpdatedListener installListener = state -> {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            Activity activity = activityRef.get();
            if (activity != null && !activity.isFinishing()) {
                Toast.makeText(activity, "تم تنزيل تحديث تاجر — سيتم تثبيته الآن", Toast.LENGTH_LONG).show();
            }
            updateManager.completeUpdate();
        }
    };

    TagerUpdateCoordinator(TagerApplication application) {
        updateManager = AppUpdateManagerFactory.create(application);
        updateManager.registerListener(installListener);
    }

    void onActivityResumed(Activity activity) {
        if (!(activity instanceof TagerActivity)) return;
        activityRef = new WeakReference<>(activity);
        if (updateFlowStarted) {
            resumeUpdateIfNeeded(activity);
            return;
        }
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
                    }
                });
    }

    private void startFlexibleUpdate(Activity activity, AppUpdateInfo info) {
        if (updateFlowStarted && info.updateAvailability() != UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            return;
        }
        try {
            updateFlowStarted = true;
            updateManager.startUpdateFlowForResult(
                    info,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    UPDATE_REQUEST_CODE);
        } catch (RuntimeException error) {
            updateFlowStarted = false;
        }
    }
}
