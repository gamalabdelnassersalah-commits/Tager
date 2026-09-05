package com.tager.marketplace;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

final class TagerNotificationCenter {
    private TagerNotificationCenter() { }

    static boolean canNotify(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    static void showOrderNotification(Context context, int notificationId, String title, String message, String page) {
        show(
                context,
                TagerApplication.CHANNEL_ORDERS,
                notificationId,
                title,
                message,
                page,
                NotificationCompat.PRIORITY_HIGH);
    }

    static void showMessageNotification(Context context, int notificationId, String title, String message, String page) {
        show(
                context,
                TagerApplication.CHANNEL_MESSAGES,
                notificationId,
                title,
                message,
                page,
                NotificationCompat.PRIORITY_DEFAULT);
    }

    private static void show(
            Context context,
            String channelId,
            int notificationId,
            String title,
            String message,
            String page,
            int priority) {
        if (!canNotify(context)) return;

        String safeTitle = title == null || title.trim().isEmpty() ? "Tager | تاجر" : title.trim();
        String safeMessage = message == null ? "" : message.trim();
        String safePage = sanitizePage(page);

        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse("tager://" + safePage), context, TagerActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification_tager)
                .setContentTitle(safeTitle)
                .setContentText(safeMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(safeMessage))
                .setPriority(priority)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Permission can be revoked between canNotify() and notify().
            // Failing closed keeps notification delivery safe without crashing Tager.
        }
    }

    private static String sanitizePage(String page) {
        if (page == null || !page.matches("[A-Za-z0-9_-]{1,64}")) return "home";
        return page;
    }
}
