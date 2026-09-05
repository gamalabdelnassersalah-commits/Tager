package com.tager.marketplace;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Native settings/status center for Tager Android. */
public class TagerSettingsActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 7301;
    private static final int TEST_NOTIFICATION_ID = 730100;

    private TextView notificationStatus;
    private TextView channelStatus;
    private TextView runtimeStatus;
    private TextView networkStatus;
    private TextView webViewStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.tager_teal_dark));
        setTitle("إعدادات تاجر");
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotificationStatus();
        refreshRuntimeStatus();
        refreshNetworkStatus();
        refreshWebViewStatus();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(16), dp(18), dp(16), dp(24));
        root.setBackgroundColor(getColor(R.color.tager_mint));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("إعدادات وحالة تطبيق تاجر");
        title.setTextSize(24f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(getColor(R.color.tager_teal_dark));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("إعدادات Android الأصلية الخاصة بالتنبيهات والتنزيلات ومعلومات التطبيق.");
        subtitle.setTextSize(14f);
        subtitle.setTextColor(getColor(R.color.tager_text_muted));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(6);
        root.addView(subtitle, subtitleParams);

        root.addView(buildNotificationCard());
        root.addView(buildNetworkHealthCard());
        root.addView(buildWebViewHealthCard());
        root.addView(buildRuntimeHealthCard());
        root.addView(buildAppInfoCard());
        root.addView(buildActionsCard());
        return scroll;
    }

    private View buildNotificationCard() {
        LinearLayout card = newCard();
        card.addView(heading("الإشعارات"));

        notificationStatus = new TextView(this);
        notificationStatus.setTextSize(15f);
        notificationStatus.setTextColor(getColor(R.color.tager_text_muted));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(6);
        card.addView(notificationStatus, statusParams);

        channelStatus = new TextView(this);
        channelStatus.setTextSize(13f);
        channelStatus.setTextColor(getColor(R.color.tager_text_muted));
        channelStatus.setLineSpacing(0f, 1.15f);
        LinearLayout.LayoutParams channelParams = new LinearLayout.LayoutParams(-1, -2);
        channelParams.topMargin = dp(6);
        card.addView(channelStatus, channelParams);

        Button enable = actionButton("تفعيل الإشعارات");
        enable.setOnClickListener(v -> enableNotifications());
        addButton(card, enable);

        Button test = actionButton("اختبار إشعار تاجر");
        test.setOnClickListener(v -> testNotification());
        addButton(card, test);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Button orders = actionButton("إعدادات إشعارات الطلبات");
            orders.setOnClickListener(v -> openNotificationChannelSettings(TagerApplication.CHANNEL_ORDERS));
            addButton(card, orders);

            Button messages = actionButton("إعدادات إشعارات الرسائل");
            messages.setOnClickListener(v -> openNotificationChannelSettings(TagerApplication.CHANNEL_MESSAGES));
            addButton(card, messages);

            Button downloads = actionButton("إعدادات إشعارات التنزيلات");
            downloads.setOnClickListener(v -> openNotificationChannelSettings(TagerApplication.CHANNEL_DOWNLOADS));
            addButton(card, downloads);
        }

        Button settings = actionButton("كل إعدادات إشعارات Android");
        settings.setOnClickListener(v -> openNotificationSettings());
        addButton(card, settings);
        return card;
    }

    private View buildNetworkHealthCard() {
        LinearLayout card = newCard();
        card.addView(heading("حالة الشبكة"));

        networkStatus = new TextView(this);
        networkStatus.setText("جاري فحص اتصال الشبكة…");
        networkStatus.setTextSize(14f);
        networkStatus.setTextColor(getColor(R.color.tager_text_muted));
        networkStatus.setLineSpacing(0f, 1.2f);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(7);
        card.addView(networkStatus, statusParams);

        TextView privacy = new TextView(this);
        privacy.setText("الفحص محلي فقط ولا يقرأ IP أو اسم Wi‑Fi أو الموقع أو أي معرف للجهاز.");
        privacy.setTextSize(12f);
        privacy.setTextColor(getColor(R.color.tager_text_muted));
        LinearLayout.LayoutParams privacyParams = new LinearLayout.LayoutParams(-1, -2);
        privacyParams.topMargin = dp(7);
        card.addView(privacy, privacyParams);
        return card;
    }

    private View buildWebViewHealthCard() {
        LinearLayout card = newCard();
        card.addView(heading("محرك عرض التطبيق"));

        webViewStatus = new TextView(this);
        webViewStatus.setText("جاري قراءة إصدار Android WebView…");
        webViewStatus.setTextSize(14f);
        webViewStatus.setTextColor(getColor(R.color.tager_text_muted));
        webViewStatus.setLineSpacing(0f, 1.2f);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(7);
        card.addView(webViewStatus, statusParams);

        Button providerSettings = actionButton("فتح إعدادات WebView الحالية");
        providerSettings.setOnClickListener(v -> openCurrentWebViewSettings());
        addButton(card, providerSettings);
        return card;
    }

    private View buildRuntimeHealthCard() {
        LinearLayout card = newCard();
        card.addView(heading("استقرار التطبيق"));

        runtimeStatus = new TextView(this);
        runtimeStatus.setText("جاري قراءة حالة التشغيل المحلية…");
        runtimeStatus.setTextSize(14f);
        runtimeStatus.setTextColor(getColor(R.color.tager_text_muted));
        runtimeStatus.setLineSpacing(0f, 1.2f);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(7);
        card.addView(runtimeStatus, statusParams);

        TextView privacy = new TextView(this);
        privacy.setText("يتم عرض الوقت والعدد ونسخة التطبيق فقط. لا يتم عرض رسالة الخطأ أو Stack Trace أو بيانات الحساب.");
        privacy.setTextSize(12f);
        privacy.setTextColor(getColor(R.color.tager_text_muted));
        LinearLayout.LayoutParams privacyParams = new LinearLayout.LayoutParams(-1, -2);
        privacyParams.topMargin = dp(7);
        card.addView(privacy, privacyParams);
        return card;
    }

    private View buildAppInfoCard() {
        LinearLayout card = newCard();
        card.addView(heading("معلومات التطبيق"));

        TextView info = new TextView(this);
        info.setText(buildAppInfo());
        info.setTextSize(15f);
        info.setTextColor(getColor(R.color.tager_text_muted));
        info.setLineSpacing(0f, 1.2f);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(-1, -2);
        infoParams.topMargin = dp(8);
        card.addView(info, infoParams);

        TextView privacy = new TextView(this);
        privacy.setText("تقرير التشخيص لا يتضمن حسابك أو الروابط أو Cookies أو معرف الجهاز أو تفاصيل Crash.");
        privacy.setTextSize(13f);
        privacy.setTextColor(getColor(R.color.tager_text_muted));
        LinearLayout.LayoutParams privacyParams = new LinearLayout.LayoutParams(-1, -2);
        privacyParams.topMargin = dp(8);
        card.addView(privacy, privacyParams);

        Button copyDiagnostics = actionButton("نسخ تقرير التشخيص الآمن");
        copyDiagnostics.setOnClickListener(v -> copyDiagnostics());
        addButton(card, copyDiagnostics);

        Button shareDiagnostics = actionButton("مشاركة تقرير التشخيص");
        shareDiagnostics.setOnClickListener(v -> shareDiagnostics());
        addButton(card, shareDiagnostics);
        return card;
    }

    private View buildActionsCard() {
        LinearLayout card = newCard();
        card.addView(heading("أدوات سريعة"));

        Button downloads = actionButton("مركز تنزيلات تاجر");
        downloads.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, TagerDownloadsActivity.class));
            } catch (RuntimeException error) {
                Toast.makeText(this, "تعذر فتح مركز التنزيلات", Toast.LENGTH_SHORT).show();
            }
        });
        addButton(card, downloads);

        Button appSettings = actionButton("إعدادات التطبيق في Android");
        appSettings.setOnClickListener(v -> openApplicationSettings());
        addButton(card, appSettings);
        return card;
    }

    private LinearLayout newCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(R.color.white));
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), getColor(R.color.tager_mint));
        card.setBackground(background);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(14);
        card.setLayoutParams(params);
        return card;
    }

    private TextView heading(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(18f);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setTextColor(getColor(R.color.tager_teal_dark));
        return view;
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(48));
        return button;
    }

    private void addButton(LinearLayout parent, Button button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
        params.topMargin = dp(8);
        parent.addView(button, params);
    }

    private void enableNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
            return;
        }
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            openNotificationSettings();
            return;
        }
        Toast.makeText(this, "إشعارات تاجر مفعلة بالفعل", Toast.LENGTH_SHORT).show();
    }

    private void testNotification() {
        if (!TagerNotificationCenter.canNotify(this)) {
            Toast.makeText(this, "فعّل الإشعارات أولًا ثم أعد الاختبار", Toast.LENGTH_SHORT).show();
            enableNotifications();
            return;
        }
        TagerNotificationCenter.showMessageNotification(
                this,
                TEST_NOTIFICATION_ID,
                "اختبار إشعارات تاجر",
                "الإشعارات المحلية تعمل بشكل صحيح على هذا الجهاز.",
                "home");
        Toast.makeText(this, "تم إرسال إشعار اختبار", Toast.LENGTH_SHORT).show();
    }

    private void copyDiagnostics() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, "تعذر الوصول إلى الحافظة", Toast.LENGTH_SHORT).show();
            return;
        }
        String report = buildDiagnosticsReport();
        clipboard.setPrimaryClip(ClipData.newPlainText("Tager diagnostics", report));
        Toast.makeText(this, "تم نسخ تقرير التشخيص بدون بيانات حساب أو روابط", Toast.LENGTH_SHORT).show();
    }

    private void shareDiagnostics() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, "Tager Android diagnostics");
        share.putExtra(Intent.EXTRA_TEXT, buildDiagnosticsReport());
        try {
            startActivity(Intent.createChooser(share, "مشاركة تقرير تشخيص تاجر"));
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, "تعذر فتح تطبيق للمشاركة", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildDiagnosticsReport() {
        boolean permissionGranted = notificationPermissionGranted();
        boolean notificationsEnabled = permissionGranted
                && NotificationManagerCompat.from(this).areNotificationsEnabled();
        TagerCrashRecorder.Snapshot runtime = TagerCrashRecorder.snapshot(this);
        NetworkHealth network = readNetworkHealth();
        PackageInfo webView = currentWebViewPackage();

        StringBuilder report = new StringBuilder();
        report.append("Tager Android diagnostics")
                .append("\nApp: ").append(BuildConfig.VERSION_NAME)
                .append(" (build ").append(BuildConfig.VERSION_CODE).append(")")
                .append("\nAndroid: ").append(Build.VERSION.RELEASE)
                .append(" / API ").append(Build.VERSION.SDK_INT)
                .append("\nNotifications: ").append(notificationsEnabled ? "enabled" : "disabled");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            report.append("\nChannels: orders=").append(channelEnabled(TagerApplication.CHANNEL_ORDERS))
                    .append(", messages=").append(channelEnabled(TagerApplication.CHANNEL_MESSAGES))
                    .append(", downloads=").append(channelEnabled(TagerApplication.CHANNEL_DOWNLOADS));
        }
        if (webView != null) {
            report.append("\nWebView: ").append(webView.packageName)
                    .append(" ").append(webView.versionName == null ? "" : webView.versionName);
        } else {
            report.append("\nWebView: unavailable");
        }
        report.append("\nNetwork: connected=").append(network.connected)
                .append(", validated=").append(network.validated)
                .append(", metered=").append(network.metered)
                .append(", transport=").append(network.transport);
        report.append("\nRuntime health: crashes=").append(runtime.crashCount)
                .append(", last_crash_at=").append(runtime.lastCrashAt)
                .append(", version=").append(runtime.version);
        report.append("\nPrivacy: no account, URL, cookie, IP, Wi-Fi name, location, device ID, crash message or crash stack included");
        return report.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != NOTIFICATION_PERMISSION_REQUEST) return;
        refreshNotificationStatus();
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        Toast.makeText(
                this,
                granted ? "تم تفعيل إشعارات تاجر" : "لم يتم تفعيل الإشعارات",
                Toast.LENGTH_SHORT).show();
    }

    private boolean notificationPermissionGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshNotificationStatus() {
        if (notificationStatus == null) return;
        boolean enabled = notificationPermissionGranted()
                && NotificationManagerCompat.from(this).areNotificationsEnabled();
        notificationStatus.setText(enabled
                ? "الحالة العامة: مفعلة — Android يسمح لتاجر بإظهار الإشعارات."
                : "الحالة العامة: غير مفعلة — يمكنك تفعيلها من الزر أدناه.");
        notificationStatus.setTextColor(getColor(enabled ? R.color.tager_teal : R.color.tager_orange));

        if (channelStatus == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            channelStatus.setText("قنوات الإشعارات المنفصلة متاحة من Android 8 فأحدث.");
            return;
        }
        channelStatus.setText("الطلبات: " + channelLabel(TagerApplication.CHANNEL_ORDERS)
                + "  •  الرسائل: " + channelLabel(TagerApplication.CHANNEL_MESSAGES)
                + "  •  التنزيلات: " + channelLabel(TagerApplication.CHANNEL_DOWNLOADS));
    }

    private void refreshNetworkStatus() {
        if (networkStatus == null) return;
        NetworkHealth network = readNetworkHealth();
        if (!network.connected) {
            networkStatus.setText("الحالة: غير متصل — لا توجد شبكة نشطة متاحة لتاجر حاليًا.");
            networkStatus.setTextColor(getColor(R.color.tager_orange));
            return;
        }
        String validation = network.validated
                ? "تم التحقق من وصول الإنترنت"
                : "الشبكة موجودة لكن Android لم يتحقق من وصول الإنترنت";
        networkStatus.setText("الحالة: متصل — " + transportArabic(network.transport)
                + "\nالإنترنت: " + validation
                + "\nاتصال محسوب (Metered): " + (network.metered ? "نعم" : "لا"));
        networkStatus.setTextColor(getColor(network.validated ? R.color.tager_teal : R.color.tager_orange));
    }

    private void refreshWebViewStatus() {
        if (webViewStatus == null) return;
        PackageInfo webView = currentWebViewPackage();
        if (webView == null) {
            webViewStatus.setText("الحالة: تعذر تحديد مزود Android WebView الحالي على هذا الجهاز.");
            webViewStatus.setTextColor(getColor(R.color.tager_orange));
            return;
        }
        String version = webView.versionName == null || webView.versionName.trim().isEmpty()
                ? "غير معروف" : webView.versionName;
        webViewStatus.setText("الحالة: متاح\nالمزود: " + webView.packageName
                + "\nالإصدار: " + version);
        webViewStatus.setTextColor(getColor(R.color.tager_teal));
    }

    private PackageInfo currentWebViewPackage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null;
        try {
            return WebView.getCurrentWebViewPackage();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void openCurrentWebViewSettings() {
        PackageInfo webView = currentWebViewPackage();
        if (webView == null || webView.packageName == null || webView.packageName.trim().isEmpty()) {
            Toast.makeText(this, "تعذر تحديد تطبيق WebView الحالي", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + webView.packageName));
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, "تعذر فتح إعدادات WebView", Toast.LENGTH_SHORT).show();
        }
    }

    private NetworkHealth readNetworkHealth() {
        try {
            ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) return NetworkHealth.offline();
            Network active = manager.getActiveNetwork();
            if (active == null) return NetworkHealth.offline();
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(active);
            if (capabilities == null) return NetworkHealth.offline();

            boolean connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            boolean validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            boolean metered = manager.isActiveNetworkMetered();
            return new NetworkHealth(connected, validated, metered, detectTransport(capabilities));
        } catch (RuntimeException ignored) {
            return NetworkHealth.offline();
        }
    }

    private String detectTransport(NetworkCapabilities capabilities) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "wifi";
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "cellular";
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "ethernet";
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return "vpn";
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) return "bluetooth";
        return "other";
    }

    private String transportArabic(String transport) {
        if ("wifi".equals(transport)) return "Wi‑Fi";
        if ("cellular".equals(transport)) return "بيانات الجوال";
        if ("ethernet".equals(transport)) return "Ethernet";
        if ("vpn".equals(transport)) return "VPN";
        if ("bluetooth".equals(transport)) return "Bluetooth";
        return "شبكة أخرى";
    }

    private void refreshRuntimeStatus() {
        if (runtimeStatus == null) return;
        TagerCrashRecorder.Snapshot runtime = TagerCrashRecorder.snapshot(this);
        if (!runtime.hasCrash()) {
            runtimeStatus.setText("الحالة: مستقرة — لا يوجد Crash محلي مسجل في فترة الاحتفاظ الحالية.");
            runtimeStatus.setTextColor(getColor(R.color.tager_teal));
            return;
        }
        String when = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT,
                Locale.getDefault()).format(new Date(runtime.lastCrashAt));
        runtimeStatus.setText("تم تسجيل Crash محلي سابقًا\nآخر تسجيل: " + when
                + "\nالعدد خلال فترة الاحتفاظ: " + runtime.crashCount
                + "\nنسخة التطبيق وقت آخر تسجيل: " + runtime.version);
        runtimeStatus.setTextColor(getColor(R.color.tager_orange));
    }

    private String channelLabel(String channelId) {
        return channelEnabled(channelId) ? "مفعلة" : "متوقفة";
    }

    private boolean channelEnabled(String channelId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return false;
        NotificationChannel channel = manager.getNotificationChannel(channelId);
        return channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }

    private void openNotificationChannelSettings(String channelId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            openNotificationSettings();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                .putExtra(Settings.EXTRA_CHANNEL_ID, channelId);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException error) {
            openNotificationSettings();
        }
    }

    private void openNotificationSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            openApplicationSettings();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException error) {
            openApplicationSettings();
        }
    }

    private void openApplicationSettings() {
        try {
            Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, "تعذر فتح إعدادات Android", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildAppInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Tager ").append(BuildConfig.VERSION_NAME)
                .append(" (build ").append(BuildConfig.VERSION_CODE).append(")")
                .append("\nAndroid ").append(Build.VERSION.RELEASE)
                .append(" — API ").append(Build.VERSION.SDK_INT);

        PackageInfo webView = currentWebViewPackage();
        if (webView != null) {
            info.append("\nWebView: ").append(webView.packageName)
                    .append(" ").append(webView.versionName == null ? "" : webView.versionName);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            info.append("\nWebView: غير متاح");
        }
        return info.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class NetworkHealth {
        final boolean connected;
        final boolean validated;
        final boolean metered;
        final String transport;

        NetworkHealth(boolean connected, boolean validated, boolean metered, String transport) {
            this.connected = connected;
            this.validated = validated;
            this.metered = metered;
            this.transport = transport == null ? "other" : transport;
        }

        static NetworkHealth offline() {
            return new NetworkHealth(false, false, false, "none");
        }
    }
}
