package com.tager.marketplace;

/** Prevents duplicate external app launches caused by double taps or repeated WebView callbacks. */
final class TagerExternalLaunchGate {
    static final long DEFAULT_WINDOW_MS = 700L;

    private String lastKey = "";
    private long lastOpenedAt = Long.MIN_VALUE;

    synchronized boolean shouldAllow(String key, long nowMs) {
        if (key == null || key.isEmpty()) return false;
        if (key.equals(lastKey) && nowMs >= lastOpenedAt && nowMs - lastOpenedAt < DEFAULT_WINDOW_MS) {
            return false;
        }
        lastKey = key;
        lastOpenedAt = nowMs;
        return true;
    }
}
