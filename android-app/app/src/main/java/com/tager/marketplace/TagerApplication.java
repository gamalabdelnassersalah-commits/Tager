package com.tager.marketplace;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class TagerApplication extends Application {
    public static final String CHANNEL_ORDERS = "tager_orders";
    public static final String CHANNEL_MESSAGES = "tager_messages";
    public static final String CHANNEL_DOWNLOADS = "tager_downloads";
    private static final String PERIODIC_MAINTENANCE = "tager_periodic_maintenance";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        scheduleMaintenance();
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

    private void scheduleMaintenance() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                TagerMaintenanceWorker.class,
                24,
                TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                PERIODIC_MAINTENANCE,
                ExistingPeriodicWorkPolicy.UPDATE,
                request);
    }
}
