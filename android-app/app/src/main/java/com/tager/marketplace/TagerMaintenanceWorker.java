package com.tager.marketplace;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;

public class TagerMaintenanceWorker extends Worker {
    private static final long CAMERA_MAX_AGE_MS = 24L * 60L * 60L * 1000L;
    private static final long WEBVIEW_TEMP_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L;

    public TagerMaintenanceWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context context = getApplicationContext();
            cleanupOlderThan(new File(context.getCacheDir(), "camera"), CAMERA_MAX_AGE_MS);
            cleanupOlderThan(new File(context.getCacheDir(), "WebView"), WEBVIEW_TEMP_MAX_AGE_MS);
            cleanupEmptyDirectories(context.getCacheDir());
            return Result.success();
        } catch (RuntimeException error) {
            return Result.retry();
        }
    }

    private void cleanupOlderThan(File directory, long maxAgeMs) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) return;
        File[] files = directory.listFiles();
        if (files == null) return;
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        for (File file : files) {
            if (file == null) continue;
            if (file.isDirectory()) {
                cleanupOlderThan(file, maxAgeMs);
                deleteIfEmpty(file);
            } else if (file.lastModified() > 0L && file.lastModified() < cutoff) {
                file.delete();
            }
        }
    }

    private void cleanupEmptyDirectories(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) return;
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file != null && file.isDirectory()) {
                cleanupEmptyDirectories(file);
                deleteIfEmpty(file);
            }
        }
    }

    private void deleteIfEmpty(File directory) {
        File[] children = directory.listFiles();
        if (children != null && children.length == 0) directory.delete();
    }
}
