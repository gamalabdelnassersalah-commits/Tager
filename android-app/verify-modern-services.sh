#!/usr/bin/env bash
set -euo pipefail

BUILD="app/build.gradle"
MANIFEST="app/src/main/AndroidManifest.xml"
APP="app/src/main/java/com/tager/marketplace/TagerApplication.java"
UPDATE="app/src/main/java/com/tager/marketplace/TagerUpdateCoordinator.java"
NOTIFY="app/src/main/java/com/tager/marketplace/TagerNotificationCenter.java"
WORKER="app/src/main/java/com/tager/marketplace/TagerMaintenanceWorker.java"
CRASH="app/src/main/java/com/tager/marketplace/TagerCrashRecorder.java"
DOWNLOADS="app/src/main/java/com/tager/marketplace/TagerDownloadsActivity.java"
SETTINGS="app/src/main/java/com/tager/marketplace/TagerSettingsActivity.java"
SHORTCUTS="app/src/main/res/xml/shortcuts.xml"
ACTIVITY="app/src/main/java/com/tager/marketplace/TagerActivity.java"
NOTIFICATION_ICON="app/src/main/res/drawable/ic_notification_tager.xml"
DOWNLOAD_ICON="app/src/main/res/drawable/ic_shortcut_downloads.xml"
SETTINGS_ICON="app/src/main/res/drawable/ic_shortcut_settings.xml"

# Core native dependencies and external navigation protection.
grep -q "androidx.browser:browser:1.10.0" "$BUILD"
grep -q "androidx.work:work-runtime:2.10.1" "$BUILD"
grep -q "com.google.android.play:app-update:2.1.0" "$BUILD"
grep -q 'CustomTabsIntent' "$ACTIVITY"
grep -q 'openExternalWebLink' "$ACTIVITY"
grep -q 'onPermissionRequest' "$ACTIVITY"
grep -q 'request.deny()' "$ACTIVITY"

# Background maintenance and user-respecting Play update lifecycle.
grep -q 'PeriodicWorkRequest' "$APP"
grep -q 'ExistingPeriodicWorkPolicy.UPDATE' "$APP"
grep -q 'TagerMaintenanceWorker.class' "$APP"
grep -q 'class TagerMaintenanceWorker' "$WORKER"
grep -q 'TagerCrashRecorder.clearStale' "$WORKER"
if grep -q '"WebView"' "$WORKER"; then
  echo 'Maintenance must not touch WebView-owned cache paths' >&2
  exit 1
fi
grep -q 'new File(context.getCacheDir(), "camera")' "$WORKER"

grep -q 'AppUpdateManagerFactory.create' "$UPDATE"
grep -q 'AppUpdateType.FLEXIBLE' "$UPDATE"
grep -q 'completeUpdate()' "$UPDATE"
grep -q 'CHECK_INTERVAL_MS' "$UPDATE"
grep -q 'KEY_LAST_CHECK_AT' "$UPDATE"
grep -q 'KEY_UPDATE_LATER_AT' "$UPDATE"
grep -q 'KEY_INSTALL_LATER_AT' "$UPDATE"
grep -q 'UPDATE_PROMPT_COOLDOWN_MS' "$UPDATE"
grep -q 'INSTALL_PROMPT_COOLDOWN_MS' "$UPDATE"
grep -q 'updateCheckInProgress' "$UPDATE"
grep -q 'addOnFailureListener' "$UPDATE"
grep -q 'putLong(KEY_LAST_CHECK_AT, System.currentTimeMillis())' "$UPDATE"
grep -q 'onActivityResult' "$UPDATE"
grep -q 'تحديث الآن' "$UPDATE"
grep -q 'إعادة تشغيل وتثبيت' "$UPDATE"

# Privacy-safe local crash health recording and read-only status snapshot.
test -f "$CRASH"
grep -q 'class TagerCrashRecorder' "$CRASH"
grep -q 'BuildConfig.VERSION_NAME' "$CRASH"
grep -q 'Thread.setDefaultUncaughtExceptionHandler' "$APP"
grep -q 'static Snapshot snapshot' "$CRASH"
grep -q 'static final class Snapshot' "$CRASH"
grep -q 'lastCrashAt' "$CRASH"
grep -q 'crashCount' "$CRASH"
if grep -Eq 'getMessage\(|printStackTrace|StackTraceElement' "$CRASH"; then
  echo 'Crash recorder must not persist messages or stack traces' >&2
  exit 1
fi

# Notification readiness plus explicit user-driven native settings.
grep -q 'android.permission.POST_NOTIFICATIONS' "$MANIFEST"
grep -q 'CHANNEL_ORDERS' "$APP"
grep -q 'CHANNEL_MESSAGES' "$APP"
grep -q 'CHANNEL_DOWNLOADS' "$APP"
grep -q 'class TagerNotificationCenter' "$NOTIFY"
grep -q 'PendingIntent.FLAG_IMMUTABLE' "$NOTIFY"
grep -q 'tager://' "$NOTIFY"
grep -q 'ic_notification_tager' "$NOTIFY"
test -f "$NOTIFICATION_ICON"

test -f "$SETTINGS"
grep -q 'class TagerSettingsActivity' "$SETTINGS"
grep -q 'requestPermissions' "$SETTINGS"
grep -q 'POST_NOTIFICATIONS' "$SETTINGS"
grep -q 'ACTION_APP_NOTIFICATION_SETTINGS' "$SETTINGS"
grep -q 'ACTION_CHANNEL_NOTIFICATION_SETTINGS' "$SETTINGS"
grep -q 'Settings.EXTRA_CHANNEL_ID' "$SETTINGS"
grep -q 'openNotificationChannelSettings' "$SETTINGS"
grep -q 'ACTION_APPLICATION_DETAILS_SETTINGS' "$SETTINGS"
grep -q 'getCurrentWebViewPackage' "$SETTINGS"
grep -q 'TagerDownloadsActivity.class' "$SETTINGS"
grep -q 'testNotification' "$SETTINGS"
grep -q 'showMessageNotification' "$SETTINGS"
grep -q 'TEST_NOTIFICATION_ID' "$SETTINGS"
grep -q 'copyDiagnostics' "$SETTINGS"
grep -q 'shareDiagnostics' "$SETTINGS"
grep -q 'ClipboardManager' "$SETTINGS"
grep -q 'Intent.ACTION_SEND' "$SETTINGS"
grep -q 'buildDiagnosticsReport' "$SETTINGS"
grep -q 'channelStatus' "$SETTINGS"
grep -q 'channelEnabled' "$SETTINGS"
grep -q 'getNotificationChannel' "$SETTINGS"
grep -q 'NotificationManager.IMPORTANCE_NONE' "$SETTINGS"
grep -q 'Channels: orders=' "$SETTINGS"
grep -q 'buildRuntimeHealthCard' "$SETTINGS"
grep -q 'refreshRuntimeStatus' "$SETTINGS"
grep -q 'TagerCrashRecorder.snapshot' "$SETTINGS"
grep -q 'Runtime health: crashes=' "$SETTINGS"
grep -q 'buildNetworkHealthCard' "$SETTINGS"
grep -q 'refreshNetworkStatus' "$SETTINGS"
grep -q 'ConnectivityManager' "$SETTINGS"
grep -q 'NetworkCapabilities' "$SETTINGS"
grep -q 'NET_CAPABILITY_VALIDATED' "$SETTINGS"
grep -q 'isActiveNetworkMetered' "$SETTINGS"
grep -q 'Network: connected=' "$SETTINGS"
grep -q 'no account, URL, cookie, IP, Wi-Fi name, location, device ID, crash message or crash stack included' "$SETTINGS"
grep -q 'Build.VERSION.SDK_INT < Build.VERSION_CODES.O' "$SETTINGS"
grep -q 'android:name=".TagerSettingsActivity"' "$MANIFEST"
grep -A10 'android:name=".TagerSettingsActivity"' "$MANIFEST" | grep -q 'android:exported="true"'
grep -A10 'android:name=".TagerSettingsActivity"' "$MANIFEST" | grep -q 'android:scheme="tager" android:host="settings"'

# Native download center stays Tager-scoped, metadata-only, efficient and accessible.
test -f "$DOWNLOADS"
grep -q 'class TagerDownloadsActivity' "$DOWNLOADS"
grep -q 'DownloadManager.Query' "$DOWNLOADS"
grep -q 'getUriForDownloadedFile' "$DOWNLOADS"
grep -q 'ACTION_VIEW_DOWNLOADS' "$DOWNLOADS"
grep -q 'COLUMN_DESCRIPTION' "$DOWNLOADS"
grep -q 'COLUMN_REASON' "$DOWNLOADS"
grep -q 'TAGER_DOWNLOAD_MARKER' "$DOWNLOADS"
grep -q 'Tager | تاجر' "$DOWNLOADS"
grep -q 'shareDownloadedFile' "$DOWNLOADS"
grep -q 'Intent.ACTION_SEND' "$DOWNLOADS"
grep -q 'ERROR_INSUFFICIENT_SPACE' "$DOWNLOADS"
grep -q 'PAUSED_WAITING_FOR_NETWORK' "$DOWNLOADS"
grep -q 'confirmDownloadRemoval' "$DOWNLOADS"
grep -q 'downloadManager.remove' "$DOWNLOADS"
grep -q 'scheduleActiveRefresh' "$DOWNLOADS"
grep -q 'hasActiveDownloads' "$DOWNLOADS"
grep -q 'ProgressBar' "$DOWNLOADS"
grep -q 'progressBarStyleHorizontal' "$DOWNLOADS"
grep -q 'progressPercent' "$DOWNLOADS"
grep -q 'setContentDescription("تقدم التنزيل' "$DOWNLOADS"
if grep -Eq 'COLUMN_URI|COLUMN_LOCAL_URI|CookieManager|getCookie\(' "$DOWNLOADS"; then
  echo 'Download center must not read source URLs or cookies' >&2
  exit 1
fi
if grep -q 'postDelayed(this, AUTO_REFRESH_MS)' "$DOWNLOADS"; then
  echo 'Download center must not poll forever when no active downloads exist' >&2
  exit 1
fi
grep -q 'android:name=".TagerDownloadsActivity"' "$MANIFEST"
grep -A10 'android:name=".TagerDownloadsActivity"' "$MANIFEST" | grep -q 'android:exported="true"'
grep -A10 'android:name=".TagerDownloadsActivity"' "$MANIFEST" | grep -q 'android:scheme="tager" android:host="downloads"'
test -f "$DOWNLOAD_ICON"

# Native launcher shortcuts and deep-link routing.
grep -q 'android.app.shortcuts' "$MANIFEST"
grep -q '@xml/shortcuts' "$MANIFEST"
for page in products track cart; do
  grep -q "android:shortcutId=\"$page\"" "$SHORTCUTS"
  grep -q "android:data=\"tager://$page\"" "$SHORTCUTS"
done
grep -q 'android:shortcutId="downloads"' "$SHORTCUTS"
grep -q 'TagerDownloadsActivity' "$SHORTCUTS"
grep -q 'android:shortcutId="settings"' "$SHORTCUTS"
grep -q 'TagerSettingsActivity' "$SHORTCUTS"
test -f "$SETTINGS_ICON"

# Keep one native refresh implementation; prevent the old touch-listener path.
grep -q 'TagerSwipeRefreshLayout' 'app/src/main/res/layout/activity_main.xml'
if grep -q 'setOnTouchListener.*reload' "$ACTIVITY"; then
  echo 'Legacy WebView touch refresh path detected' >&2
  exit 1
fi

echo 'Tager modern Android service locks passed.'
