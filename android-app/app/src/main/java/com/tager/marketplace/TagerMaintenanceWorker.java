package com.tager.marketplace;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;

public class TagerMaintenanceWorker extends Worker {
    private static final long CAMERA_MAX_AGE_MS = 24L * 60L * 60L * 1000L;
    private static final long CRASH_HEALTH_MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L;

    public TagerMaintenanceWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context context = getApplicationContext();
            // Only touch cache paths created and owned by Tager itself.
            // Chromium/WebView manages its own cache lifecycle and must not be
            // modified behind the renderer while it may be active.
            File cameraDir = new File(context.getCacheDir(), "camera");
            cleanupOlderThan(cameraDir, CAMERA_MAX_AGE_MS);
            cleanupEmptyDirectories(cameraDir);
            TagerCrashRecorder.clearStale(context, CRASH_HEALTH_MAX_AGE_MS);
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
        if (directory == null || !directory.isDirectory()) return;
        File[] children = directory.listFiles();
        if (children != null && children.length == 0) directory.delete();
    }
}
