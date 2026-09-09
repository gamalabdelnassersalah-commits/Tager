package com.tager.marketplace;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class TagerApplication extends Application {
    public static final String CHANNEL_ORDERS = "tager_orders";
    public static final String CHANNEL_MESSAGES = "tager_messages";
    public static final String CHANNEL_DOWNLOADS = "tager_downloads";
    private static final String PERIODIC_MAINTENANCE = "tager_periodic_maintenance";
    private static final String GOOGLE_PLAY_PACKAGE = "com.android.vending";
    private static final long MAINTENANCE_STARTUP_DELAY_MS = 5000L;

    private TagerUpdateCoordinator updateCoordinator;
    private Boolean installedFromGooglePlay;
    private final Handler startupHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        installCrashRecorder();
        createNotificationChannels();
        scheduleMaintenanceDeferred();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) { }
            @Override public void onActivityStarted(@NonNull Activity activity) { }
            @Override public void onActivityResumed(@NonNull Activity activity) {
                if (!(activity instanceof TagerActivity)) return;
                ensureUpdateCoordinator();
                if (updateCoordinator != null) updateCoordinator.onActivityResumed(activity);
            }
            @Override public void onActivityPaused(@NonNull Activity activity) {
                if (updateCoordinator != null) updateCoordinator.onActivityPaused(activity);
            }
            @Override public void onActivityStopped(@NonNull Activity activity) { }
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
            @Override public void onActivityDestroyed(@NonNull Activity activity) { }
        });
    }

    private void ensureUpdateCoordinator() {
        if (updateCoordinator != null) return;
        if (installedFromGooglePlay == null) {
            installedFromGooglePlay = isInstalledFromGooglePlay();
        }
        if (Boolean.TRUE.equals(installedFromGooglePlay)) {
            try {
                updateCoordinator = new TagerUpdateCoordinator(this);
            } catch (RuntimeException ignored) {
                updateCoordinator = null;
            }
        }
    }

    private boolean isInstalledFromGooglePlay() {
        try {
            String installer;
            PackageManager packageManager = getPackageManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                installer = packageManager
                        .getInstallSourceInfo(getPackageName())
                        .getInstallingPackageName();
            } else {
                installer = packageManager.getInstallerPackageName(getPackageName());
            }
            return GOOGLE_PLAY_PACKAGE.equals(installer);
        } catch (RuntimeException | PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private void installCrashRecorder() {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            TagerCrashRecorder.record(this, thread, error);
            if (previous != null) {
                previous.uncaughtException(thread, error);
            } else {
                System.exit(10);
            }
        });
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel orders = new NotificationChannel(
                CHANNEL_ORDERS,
                "الطلبات وحالة الطلب",
                NotificationManager.IMPORTANCE_HIGH);
        orders.setDescription("إشعارات الطلبات، الموافقات، وعروض الأسعار من تاجر");

        NotificationChannel messages = new NotificationChannel(
                CHANNEL_MESSAGES,
                "الرسائل والتنبيهات",
                NotificationManager.IMPORTANCE_DEFAULT);
        messages.setDescription("رسائل الموردين والعملاء والتنبيهات المهمة");

        NotificationChannel downloads = new NotificationChannel(
                CHANNEL_DOWNLOADS,
                "التنزيلات",
                NotificationManager.IMPORTANCE_LOW);
        downloads.setDescription("حالة تنزيل ملفات ومستندات تاجر");

        manager.createNotificationChannel(orders);
        manager.createNotificationChannel(messages);
        manager.createNotificationChannel(downloads);
    }

    private void scheduleMaintenanceDeferred() {
        startupHandler.postDelayed(this::scheduleMaintenance, MAINTENANCE_STARTUP_DELAY_MS);
    }

    private void scheduleMaintenance() {
        try {
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    TagerMaintenanceWorker.class,
                    24,
                    TimeUnit.HOURS)
                    .build();
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    PERIODIC_MAINTENANCE,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request);
        } catch (RuntimeException ignored) {
            // Maintenance is best-effort and must never delay or crash app startup.
        }
    }
}
