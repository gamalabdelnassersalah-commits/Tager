package com.tager.marketplace;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Native, privacy-scoped download center for files started by Tager. */
public class TagerDownloadsActivity extends Activity {
    private static final String TAGER_DOWNLOAD_MARKER = "Tager | تاجر";
    private static final long AUTO_REFRESH_MS = 2000L;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefresh = new Runnable() {
        @Override public void run() {
            refreshDownloads();
            refreshHandler.postDelayed(this, AUTO_REFRESH_MS);
        }
    };

    private DownloadManager downloadManager;
    private LinearLayout listContainer;
    private TextView summaryView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        getWindow().setStatusBarColor(getColor(R.color.tager_teal_dark));
        setTitle("تنزيلات تاجر");
        setContentView(buildContent());
        refreshDownloads();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshDownloads();
        refreshHandler.removeCallbacks(autoRefresh);
        refreshHandler.postDelayed(autoRefresh, AUTO_REFRESH_MS);
    }

    @Override protected void onPause() {
        refreshHandler.removeCallbacks(autoRefresh);
        super.onPause();
    }

    @Override protected void onDestroy() {
        refreshHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(16), dp(18), dp(16), dp(18));
        root.setBackgroundColor(getColor(R.color.tager_mint));

        TextView title = new TextView(this);
        title.setText("مركز تنزيلات تاجر");
        title.setTextSize(24f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(getColor(R.color.tager_teal_dark));
        root.addView(title);

        summaryView = new TextView(this);
        summaryView.setText("جاري قراءة التنزيلات…");
        summaryView.setTextSize(14f);
        summaryView.setTextColor(getColor(R.color.tager_text_muted));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.topMargin = dp(6);
        root.addView(summaryView, summaryParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Button refresh = new Button(this);
        refresh.setText("تحديث");
        refresh.setAllCaps(false);
        refresh.setMinHeight(dp(48));
        refresh.setOnClickListener(v -> refreshDownloads());
        actions.addView(refresh, new LinearLayout.LayoutParams(0, dp(52), 1f));

        Button openFolder = new Button(this);
        openFolder.setText("مجلد التنزيلات");
        openFolder.setAllCaps(false);
        openFolder.setMinHeight(dp(48));
        openFolder.setOnClickListener(v -> openSystemDownloads());
        LinearLayout.LayoutParams folderParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
        folderParams.setMarginStart(dp(8));
        actions.addView(openFolder, folderParams);

        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, -2);
        actionsParams.topMargin = dp(12);
        root.addView(actions, actionsParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(listContainer, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        scrollParams.topMargin = dp(12);
        root.addView(scroll, scrollParams);
        return root;
    }

    private void refreshDownloads() {
        if (listContainer == null) return;
        listContainer.removeAllViews();
        List<DownloadItem> items = loadDownloads();
        if (items.isEmpty()) {
            summaryView.setText("لا توجد تنزيلات من تاجر حتى الآن");
            TextView empty = new TextView(this);
            empty.setText("أي ملف يتم تنزيله من داخل تاجر سيظهر هنا تلقائيًا.");
            empty.setTextSize(16f);
            empty.setTextColor(getColor(R.color.tager_text_muted));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(16), dp(40), dp(16), dp(40));
            listContainer.addView(empty);
            return;
        }

        int active = 0, complete = 0, failed = 0;
        for (DownloadItem item : items) {
            if (item.status == DownloadManager.STATUS_RUNNING
                    || item.status == DownloadManager.STATUS_PENDING
                    || item.status == DownloadManager.STATUS_PAUSED) active++;
            else if (item.status == DownloadManager.STATUS_SUCCESSFUL) complete++;
            else if (item.status == DownloadManager.STATUS_FAILED) failed++;
            listContainer.addView(buildDownloadRow(item));
        }
        summaryView.setText("الكل: " + items.size() + "  •  جاري: " + active
                + "  •  مكتمل: " + complete + (failed > 0 ? "  •  فشل: " + failed : ""));
    }

    private List<DownloadItem> loadDownloads() {
        if (downloadManager == null) return Collections.emptyList();
        ArrayList<DownloadItem> items = new ArrayList<>();
        int allStatuses = DownloadManager.STATUS_PENDING | DownloadManager.STATUS_RUNNING
                | DownloadManager.STATUS_PAUSED | DownloadManager.STATUS_SUCCESSFUL
                | DownloadManager.STATUS_FAILED;
        DownloadManager.Query query = new DownloadManager.Query().setFilterByStatus(allStatuses);

        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor == null) return items;
            int idCol = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
            int titleCol = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
            int descriptionCol = cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION);
            int statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int reasonCol = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
            int currentCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            int totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            int mimeCol = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE);
            int modifiedCol = cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP);

            while (cursor.moveToNext()) {
                if (idCol < 0 || statusCol < 0 || descriptionCol < 0) continue;
                if (!TAGER_DOWNLOAD_MARKER.equals(cursor.getString(descriptionCol))) continue;
                DownloadItem item = new DownloadItem();
                item.id = cursor.getLong(idCol);
                item.title = titleCol >= 0 ? cursor.getString(titleCol) : null;
                item.status = cursor.getInt(statusCol);
                item.reason = reasonCol >= 0 ? cursor.getInt(reasonCol) : 0;
                item.currentBytes = currentCol >= 0 ? cursor.getLong(currentCol) : -1L;
                item.totalBytes = totalCol >= 0 ? cursor.getLong(totalCol) : -1L;
                item.mimeType = mimeCol >= 0 ? cursor.getString(mimeCol) : null;
                item.modifiedAt = modifiedCol >= 0 ? cursor.getLong(modifiedCol) : 0L;
                items.add(item);
            }
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
        Collections.sort(items, (left, right) -> Long.compare(right.modifiedAt, left.modifiedAt));
        return items;
    }

    private View buildDownloadRow(DownloadItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(R.color.white));
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), getColor(R.color.tager_mint));
        card.setBackground(background);

        TextView title = new TextView(this);
        title.setText(item.title == null || item.title.trim().isEmpty() ? "ملف من تاجر" : item.title.trim());
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(getColor(R.color.tager_teal_dark));
        title.setMaxLines(2);
        card.addView(title);

        TextView status = new TextView(this);
        status.setText(statusText(item));
        status.setTextSize(14f);
        status.setTextColor(statusColor(item.status));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(5);
        card.addView(status, statusParams);

        if (item.modifiedAt > 0L) {
            TextView date = new TextView(this);
            date.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                    Locale.getDefault()).format(new Date(item.modifiedAt)));
            date.setTextSize(12f);
            date.setTextColor(getColor(R.color.tager_text_muted));
            card.addView(date);
        }

        if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
            LinearLayout fileActions = new LinearLayout(this);
            fileActions.setOrientation(LinearLayout.HORIZONTAL);
            fileActions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

            Button open = new Button(this);
            open.setText("فتح الملف");
            open.setAllCaps(false);
            open.setMinHeight(dp(48));
            open.setOnClickListener(v -> openDownloadedFile(item));
            fileActions.addView(open, new LinearLayout.LayoutParams(0, dp(50), 1f));

            Button share = new Button(this);
            share.setText("مشاركة");
            share.setAllCaps(false);
            share.setMinHeight(dp(48));
            share.setOnClickListener(v -> shareDownloadedFile(item));
            LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(0, dp(50), 1f);
            shareParams.setMarginStart(dp(8));
            fileActions.addView(share, shareParams);

            LinearLayout.LayoutParams fileActionsParams = new LinearLayout.LayoutParams(-1, -2);
            fileActionsParams.topMargin = dp(8);
            card.addView(fileActions, fileActionsParams);
        }

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);
        return card;
    }

    private String statusText(DownloadItem item) {
        switch (item.status) {
            case DownloadManager.STATUS_PENDING:
                return "في انتظار بدء التنزيل";
            case DownloadManager.STATUS_RUNNING:
                if (item.totalBytes > 0L && item.currentBytes >= 0L) {
                    long percent = Math.min(100L, (item.currentBytes * 100L) / item.totalBytes);
                    return "جاري التنزيل — " + percent + "%  •  " + formatBytes(item.currentBytes)
                            + " / " + formatBytes(item.totalBytes);
                }
                return "جاري التنزيل";
            case DownloadManager.STATUS_PAUSED:
                return "التنزيل متوقف مؤقتًا — " + pausedReason(item.reason);
            case DownloadManager.STATUS_SUCCESSFUL:
                return "مكتمل" + (item.totalBytes > 0L ? "  •  " + formatBytes(item.totalBytes) : "");
            case DownloadManager.STATUS_FAILED:
                return "فشل التنزيل — " + failedReason(item.reason);
            default:
                return "حالة غير معروفة";
        }
    }

    private String pausedReason(int reason) {
        switch (reason) {
            case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                return "في انتظار اتصال بالإنترنت";
            case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                return "في انتظار شبكة Wi‑Fi مناسبة";
            case DownloadManager.PAUSED_WAITING_TO_RETRY:
                return "سيحاول Android مرة أخرى تلقائيًا";
            default:
                return "سيتم استكماله عند توفر الظروف المناسبة";
        }
    }

    private String failedReason(int reason) {
        switch (reason) {
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                return "المساحة المتاحة غير كافية";
            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                return "مكان التخزين غير متاح";
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                return "يوجد ملف بنفس الاسم";
            case DownloadManager.ERROR_CANNOT_RESUME:
                return "تعذر استكمال التنزيل؛ أعد المحاولة";
            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                return "انقطع نقل البيانات من الخادم";
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                return "تعذر الوصول للملف بسبب تحويلات كثيرة";
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                return "الخادم رفض طلب التنزيل";
            case DownloadManager.ERROR_FILE_ERROR:
                return "تعذر حفظ الملف على الجهاز";
            default:
                return "تعذر تنزيل الملف؛ أعد المحاولة من تاجر";
        }
    }

    private int statusColor(int status) {
        if (status == DownloadManager.STATUS_FAILED) return getColor(R.color.tager_orange);
        if (status == DownloadManager.STATUS_SUCCESSFUL) return getColor(R.color.tager_teal);
        return getColor(R.color.tager_text_muted);
    }

    private void openDownloadedFile(DownloadItem item) {
        DownloadHandle handle = resolveDownload(item);
        if (handle == null) return;
        Intent open = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(handle.uri, handle.mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(open);
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, "لا يوجد تطبيق مناسب لفتح هذا الملف", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareDownloadedFile(DownloadItem item) {
        DownloadHandle handle = resolveDownload(item);
        if (handle == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(handle.mimeType);
        share.putExtra(Intent.EXTRA_STREAM, handle.uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(share, "مشاركة ملف من تاجر"));
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, "لا يوجد تطبيق مناسب لمشاركة هذا الملف", Toast.LENGTH_SHORT).show();
        }
    }

    private DownloadHandle resolveDownload(DownloadItem item) {
        if (downloadManager == null || item == null) return null;
        Uri uri = downloadManager.getUriForDownloadedFile(item.id);
        if (uri == null) {
            Toast.makeText(this, "الملف لم يعد متاحًا في التنزيلات", Toast.LENGTH_SHORT).show();
            refreshDownloads();
            return null;
        }
        String mime = item.mimeType;
        if (mime == null || mime.trim().isEmpty()) mime = downloadManager.getMimeTypeForDownloadedFile(item.id);
        if (mime == null || mime.trim().isEmpty()) mime = "*/*";
        return new DownloadHandle(uri, mime);
    }

    private void openSystemDownloads() {
        try {
            startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "تعذر فتح مجلد التنزيلات على هذا الجهاز", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 0L) return "";
        if (bytes < 1024L) return bytes + " B";
        double kb = bytes / 1024d;
        if (kb < 1024d) return String.format(Locale.getDefault(), "%.1f KB", kb);
        double mb = kb / 1024d;
        if (mb < 1024d) return String.format(Locale.getDefault(), "%.1f MB", mb);
        return String.format(Locale.getDefault(), "%.2f GB", mb / 1024d);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static final class DownloadItem {
        long id;
        String title;
        String mimeType;
        int status;
        int reason;
        long currentBytes;
        long totalBytes;
        long modifiedAt;
    }

    private static final class DownloadHandle {
        final Uri uri;
        final String mimeType;
        DownloadHandle(Uri uri, String mimeType) {
            this.uri = uri;
            this.mimeType = mimeType;
        }
    }
}
