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
# Never manipulate Chromium/WebView-owned cache paths behind the renderer.
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
grep -q 'onActivityResult' "$UPDATE"
grep -q 'Activity.RESULT_CANCELED' "$UPDATE"
grep -q 'تحديث الآن' "$UPDATE"
grep -q 'إعادة تشغيل وتثبيت' "$UPDATE"

# Privacy-safe local crash health recording.
test -f "$CRASH"
grep -q 'class TagerCrashRecorder' "$CRASH"
grep -q 'BuildConfig.VERSION_NAME' "$CRASH"
grep -q 'Thread.setDefaultUncaughtExceptionHandler' "$APP"
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
grep -q 'ACTION_APPLICATION_DETAILS_SETTINGS' "$SETTINGS"
grep -q 'getCurrentWebViewPackage' "$SETTINGS"
grep -q 'TagerDownloadsActivity.class' "$SETTINGS"
grep -q 'android:name=".TagerSettingsActivity"' "$MANIFEST"
grep -A10 'android:name=".TagerSettingsActivity"' "$MANIFEST" | grep -q 'android:exported="true"'
grep -A10 'android:name=".TagerSettingsActivity"' "$MANIFEST" | grep -q 'android:scheme="tager" android:host="settings"'

# Native download center stays Tager-scoped and metadata-only.
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
if grep -Eq 'COLUMN_URI|COLUMN_LOCAL_URI|CookieManager|getCookie\(' "$DOWNLOADS"; then
  echo 'Download center must not read source URLs or cookies' >&2
  exit 1
fi
# It may be externally routed only through the exact Tager downloads deep link.
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

# Keep one native refresh implementation; prevent the old touch-listener path.
grep -q 'TagerSwipeRefreshLayout' 'app/src/main/res/layout/activity_main.xml'
if grep -q 'setOnTouchListener.*reload' "$ACTIVITY"; then
  echo 'Legacy WebView touch refresh path detected' >&2
  exit 1
fi

echo 'Tager modern Android service locks passed.'
