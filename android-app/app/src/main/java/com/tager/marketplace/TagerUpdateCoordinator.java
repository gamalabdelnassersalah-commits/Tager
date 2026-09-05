package com.tager.marketplace;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;

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
    static final int UPDATE_REQUEST_CODE = 6202;
    private static final String PREFS = "tager_update_state";
    private static final String KEY_LAST_CHECK_AT = "last_check_at";
    private static final String KEY_UPDATE_LATER_AT = "update_later_at";
    private static final String KEY_INSTALL_LATER_AT = "install_later_at";
    private static final long CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L;
    private static final long UPDATE_PROMPT_COOLDOWN_MS = 24L * 60L * 60L * 1000L;
    private static final long INSTALL_PROMPT_COOLDOWN_MS = 6L * 60L * 60L * 1000L;

    private final AppUpdateManager updateManager;
    private final InstallStateUpdatedListener installListener;
    private final SharedPreferences preferences;
    private WeakReference<Activity> activityRef = new WeakReference<>(null);
    private boolean updateFlowStarted;
    private boolean updatePromptVisible;
    private boolean installPromptVisible;
    private boolean updateCheckInProgress;

    TagerUpdateCoordinator(TagerApplication application) {
        updateManager = AppUpdateManagerFactory.create(application);
        preferences = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        installListener = state -> {
            if (state.installStatus() != InstallStatus.DOWNLOADED) return;
            updateFlowStarted = false;
            Activity activity = activityRef.get();
            if (isUsable(activity)) showInstallReadyPrompt(activity, true);
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
        if (now - lastCheck < CHECK_INTERVAL_MS) {
            checkDownloadedUpdate(activity);
            return;
        }
        checkForUpdate(activity);
    }

    void onActivityPaused(Activity activity) {
        Activity current = activityRef.get();
        if (current == activity) activityRef.clear();
    }

    boolean onActivityResult(int requestCode, int resultCode) {
        if (requestCode != UPDATE_REQUEST_CODE) return false;
        updateFlowStarted = false;
        long now = System.currentTimeMillis();
        SharedPreferences.Editor editor = preferences.edit();
        if (resultCode == Activity.RESULT_OK) {
            editor.remove(KEY_UPDATE_LATER_AT).putLong(KEY_LAST_CHECK_AT, now);
        } else {
            // User cancellation or an interrupted Play flow gets a prompt
            // cooldown, without being treated as a successful update check.
            editor.putLong(KEY_UPDATE_LATER_AT, now);
        }
        editor.apply();
        return true;
    }

    void shutdown() {
        updateManager.unregisterListener(installListener);
        activityRef.clear();
    }

    private void checkForUpdate(Activity activity) {
        if (updateCheckInProgress) return;
        updateCheckInProgress = true;
        updateManager.getAppUpdateInfo()
                .addOnSuccessListener(info -> {
                    updateCheckInProgress = false;
                    preferences.edit().putLong(KEY_LAST_CHECK_AT, System.currentTimeMillis()).apply();
                    if (!isUsable(activity)) return;
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        showInstallReadyPrompt(activity, false);
                        return;
                    }
                    if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        showUpdateAvailablePrompt(activity, info);
                    }
                })
                .addOnFailureListener(error -> updateCheckInProgress = false);
    }

    private void checkDownloadedUpdate(Activity activity) {
        updateManager.getAppUpdateInfo()
                .addOnSuccessListener(info -> {
                    if (!isUsable(activity)) return;
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        showInstallReadyPrompt(activity, false);
                    }
                });
    }

    private void resumeUpdateIfNeeded(Activity activity) {
        updateManager.getAppUpdateInfo()
                .addOnSuccessListener(info -> {
                    if (!isUsable(activity)) return;
                    if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                            && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        startFlexibleUpdate(activity, info);
                    } else if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        updateFlowStarted = false;
                        showInstallReadyPrompt(activity, false);
                    } else {
                        updateFlowStarted = false;
                    }
                })
                .addOnFailureListener(error -> updateFlowStarted = false);
    }

    private void showUpdateAvailablePrompt(Activity activity, AppUpdateInfo info) {
        if (updatePromptVisible || updateFlowStarted || !isUsable(activity)) return;
        long deferredAt = preferences.getLong(KEY_UPDATE_LATER_AT, 0L);
        if (System.currentTimeMillis() - deferredAt < UPDATE_PROMPT_COOLDOWN_MS) return;

        updatePromptVisible = true;
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("إصدار جديد من تاجر متاح")
                .setMessage("يمكن تنزيل التحديث في الخلفية مع الاستمرار في استخدام التطبيق.")
                .setPositiveButton("تحديث الآن", (ignored, which) -> startFlexibleUpdate(activity, info))
                .setNegativeButton("لاحقًا", (ignored, which) -> preferences.edit()
                        .putLong(KEY_UPDATE_LATER_AT, System.currentTimeMillis())
                        .apply())
                .create();
        dialog.setOnDismissListener(ignored -> updatePromptVisible = false);
        try {
            dialog.show();
        } catch (RuntimeException error) {
            updatePromptVisible = false;
        }
    }

    private void showInstallReadyPrompt(Activity activity, boolean ignoreCooldown) {
        if (installPromptVisible || !isUsable(activity)) return;
        long deferredAt = preferences.getLong(KEY_INSTALL_LATER_AT, 0L);
        if (!ignoreCooldown
                && System.currentTimeMillis() - deferredAt < INSTALL_PROMPT_COOLDOWN_MS) {
            return;
        }

        installPromptVisible = true;
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("التحديث جاهز للتثبيت")
                .setMessage("أعد تشغيل تاجر الآن لتثبيت الإصدار الجديد، أو أكمل عملك وثبته لاحقًا.")
                .setPositiveButton("إعادة تشغيل وتثبيت", (ignored, which) -> {
                    preferences.edit().remove(KEY_INSTALL_LATER_AT).apply();
                    updateManager.completeUpdate();
                })
                .setNegativeButton("لاحقًا", (ignored, which) -> preferences.edit()
                        .putLong(KEY_INSTALL_LATER_AT, System.currentTimeMillis())
                        .apply())
                .create();
        dialog.setOnDismissListener(ignored -> installPromptVisible = false);
        try {
            dialog.show();
        } catch (RuntimeException error) {
            installPromptVisible = false;
        }
    }

    private void startFlexibleUpdate(Activity activity, AppUpdateInfo info) {
        if (!isUsable(activity)) return;
        if (updateFlowStarted
                && info.updateAvailability() != UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            return;
        }
        try {
            updateFlowStarted = true;
            preferences.edit().remove(KEY_UPDATE_LATER_AT).apply();
            updateManager.startUpdateFlowForResult(
                    info,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    UPDATE_REQUEST_CODE);
        } catch (IntentSender.SendIntentException | RuntimeException error) {
            updateFlowStarted = false;
        }
    }

    private boolean isUsable(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }
}
