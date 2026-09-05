package com.tager.marketplace;

import android.content.Context;
import android.content.SharedPreferences;

final class TagerCrashRecorder {
    private static final String PREFS = "tager_runtime_health";
    private static final String KEY_LAST_CRASH_AT = "last_crash_at";
    private static final String KEY_LAST_CRASH_TYPE = "last_crash_type";
    private static final String KEY_LAST_CRASH_THREAD = "last_crash_thread";
    private static final String KEY_LAST_CRASH_VERSION = "last_crash_version";
    private static final String KEY_CRASH_COUNT = "crash_count";

    private TagerCrashRecorder() { }

    static void record(Context context, Thread thread, Throwable error) {
        if (context == null || error == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            int count = Math.min(1000, prefs.getInt(KEY_CRASH_COUNT, 0) + 1);
            String type = error.getClass().getName();
            String threadName = thread == null ? "unknown" : sanitize(thread.getName());
            prefs.edit()
                    .putLong(KEY_LAST_CRASH_AT, System.currentTimeMillis())
                    .putString(KEY_LAST_CRASH_TYPE, sanitize(type))
                    .putString(KEY_LAST_CRASH_THREAD, threadName)
                    .putString(KEY_LAST_CRASH_VERSION, BuildConfig.VERSION_NAME)
                    .putInt(KEY_CRASH_COUNT, count)
                    .apply();
        } catch (RuntimeException ignored) {
            // Crash recording must never interfere with Android's normal crash path.
        }
    }

    static Snapshot snapshot(Context context) {
        if (context == null) return Snapshot.empty();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return new Snapshot(
                    Math.max(0L, prefs.getLong(KEY_LAST_CRASH_AT, 0L)),
                    Math.max(0, prefs.getInt(KEY_CRASH_COUNT, 0)),
                    sanitize(prefs.getString(KEY_LAST_CRASH_VERSION, "unknown")));
        } catch (RuntimeException ignored) {
            return Snapshot.empty();
        }
    }

    static void clearStale(Context context, long maxAgeMs) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long crashAt = prefs.getLong(KEY_LAST_CRASH_AT, 0L);
            if (crashAt > 0L && System.currentTimeMillis() - crashAt > maxAgeMs) {
                prefs.edit().clear().apply();
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static String sanitize(String value) {
        if (value == null) return "unknown";
        String safe = value.replaceAll("[^A-Za-z0-9_.$-]", "_");
        return safe.length() > 120 ? safe.substring(0, 120) : safe;
    }

    static final class Snapshot {
        final long lastCrashAt;
        final int crashCount;
        final String version;

        Snapshot(long lastCrashAt, int crashCount, String version) {
            this.lastCrashAt = lastCrashAt;
            this.crashCount = crashCount;
            this.version = version == null ? "unknown" : version;
        }

        boolean hasCrash() {
            return lastCrashAt > 0L && crashCount > 0;
        }

        static Snapshot empty() {
            return new Snapshot(0L, 0, "unknown");
        }
    }
}
