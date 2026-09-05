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

/** Native settings/status center for Tager Android. */
public class TagerSettingsActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 7301;
    private static final int TEST_NOTIFICATION_ID = 730100;

    private TextView notificationStatus;
    private TextView channelStatus;

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

        Button settings = actionButton("إعدادات إشعارات Android");
        settings.setOnClickListener(v -> openNotificationSettings());
        addButton(card, settings);
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
            try {
                PackageInfo webView = WebView.getCurrentWebViewPackage();
                if (webView != null) {
                    report.append("\nWebView: ").append(webView.packageName)
                            .append(" ").append(webView.versionName == null ? "" : webView.versionName);
                }
            } catch (RuntimeException ignored) {
                report.append("\nWebView: unavailable");
            }
        }
        report.append("\nPrivacy: no account, URL, cookie, device ID or crash stack included");
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

    private void openNotificationSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            openApplicationSettings();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            openApplicationSettings();
        }
    }

    private void openApplicationSettings() {
        try {
            Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "تعذر فتح إعدادات Android", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildAppInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Tager ").append(BuildConfig.VERSION_NAME)
                .append(" (build ").append(BuildConfig.VERSION_CODE).append(")")
                .append("\nAndroid ").append(Build.VERSION.RELEASE)
                .append(" — API ").append(Build.VERSION.SDK_INT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PackageInfo webView = WebView.getCurrentWebViewPackage();
                if (webView != null) {
                    info.append("\nWebView: ").append(webView.packageName)
                            .append(" ").append(webView.versionName == null ? "" : webView.versionName);
                }
            } catch (RuntimeException ignored) {
                info.append("\nWebView: غير متاح");
            }
        }
        return info.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
