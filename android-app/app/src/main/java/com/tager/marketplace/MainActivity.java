package com.tager.marketplace;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://tager-new.vercel.app/?tager_app=android&branding=20260902&app_version=110#home";
    private static final int FILE_CHOOSER_REQUEST = 4101;

    private WebView webView;
    private ProgressBar progressBar;
    private View nativeLoading;
    private View offlinePanel;
    private ValueCallback<Uri[]> fileCallback;
    private float touchStartY;
    private boolean firstPageLoaded = false;
    private long lastBackPressedAt = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        nativeLoading = findViewById(R.id.nativeLoading);
        offlinePanel = findViewById(R.id.offlinePanel);
        Button retryButton = findViewById(R.id.retryButton);

        retryButton.setOnClickListener(v -> retryCurrentPage());
        configureWebView();

        if (savedInstanceState == null) {
            if (isOnline()) {
                showLoading(true);
                webView.loadUrl(HOME_URL);
            } else {
                showOffline();
            }
        } else {
            webView.restoreState(savedInstanceState);
            showLoading(false);
        }
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " TagerAndroidApp/1.1.0 Branding20260902");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.setSafeBrowsingEnabled(true);
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                offlinePanel.setVisibility(View.GONE);
                if (!firstPageLoaded) showLoading(true);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                firstPageLoaded = true;
                showLoading(false);
                offlinePanel.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
                injectAppPresentation(view);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request != null && request.isForMainFrame()) {
                    showOffline();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = filePathCallback;
                openFileAndCameraChooser(fileChooserParams);
                return true;
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                startDownload(url, userAgent, contentDisposition, mimetype);
            }
        });

        webView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchStartY = event.getY();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                float distance = event.getY() - touchStartY;
                if (webView.getScrollY() == 0 && distance > 240) {
                    if (isOnline()) webView.reload();
                    else showOffline();
                }
            }
            return false;
        });
    }

    private void injectAppPresentation(WebView view) {
        String appUiScript =
                "(function(){" +
                "document.documentElement.classList.add('tager-native-android');" +
                "document.body.classList.add('tager-native-android');" +
                "document.documentElement.style.webkitTextSizeAdjust='100%';" +
                "var n=document.querySelector('.tager-mobile-bottom-nav');" +
                "if(n){n.style.display='grid';}" +
                "})();";
        view.evaluateJavascript(appUiScript, null);
    }

    private boolean handleNavigation(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

        if (("http".equals(scheme) || "https".equals(scheme)) &&
                ("tager-new.vercel.app".equals(host) || host.endsWith(".tager-new.vercel.app"))) {
            return false;
        }

        try {
            if ("intent".equals(scheme)) {
                Intent intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                startActivity(intent);
                return true;
            }
            if ("tel".equals(scheme) || "mailto".equals(scheme) || "sms".equals(scheme) ||
                    "whatsapp".equals(scheme) || "market".equals(scheme)) {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
            if ("http".equals(scheme) || "https".equals(scheme)) {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
        } catch (Exception ignored) {
            Toast.makeText(this, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private void startDownload(String url, String userAgent, String contentDisposition, String mimetype) {
        try {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            if (mimetype != null && !mimetype.isEmpty()) request.setMimeType(mimetype);
            request.setTitle(fileName);
            request.setDescription("Tager | تاجر");
            request.addRequestHeader("User-Agent", userAgent);
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) request.addRequestHeader("Cookie", cookie);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            } else {
                request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName);
            }

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(this, "بدأ تحميل " + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) {
                Toast.makeText(this, "تعذر تحميل الملف", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openFileAndCameraChooser(WebChromeClient.FileChooserParams params) {
        Intent fileIntent;
        try {
            fileIntent = params.createIntent();
        } catch (Exception e) {
            fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
            fileIntent.setType("*/*");
            fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_INTENT, fileIntent);
        chooser.putExtra(Intent.EXTRA_TITLE, "اختر ملفًا أو التقط صورة");
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
        }
        startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || fileCallback == null) return;

        Uri[] result = null;
        if (resultCode == RESULT_OK && data != null) {
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                result = new Uri[clipData.getItemCount()];
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    result[i] = clipData.getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                result = new Uri[]{data.getData()};
            } else if (data.getExtras() != null && data.getExtras().get("data") instanceof Bitmap) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                String inserted = MediaStore.Images.Media.insertImage(
                        getContentResolver(), bitmap, "Tager_" + System.currentTimeMillis(), "Tager upload");
                if (inserted != null) result = new Uri[]{Uri.parse(inserted)};
            }
        }

        fileCallback.onReceiveValue(result);
        fileCallback = null;
    }

    private void retryCurrentPage() {
        if (!isOnline()) {
            showOffline();
            Toast.makeText(this, "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show();
            return;
        }
        offlinePanel.setVisibility(View.GONE);
        showLoading(true);
        String currentUrl = webView.getUrl();
        webView.loadUrl(currentUrl == null || currentUrl.startsWith("about:") ? HOME_URL : currentUrl);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void showLoading(boolean show) {
        nativeLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showOffline() {
        showLoading(false);
        progressBar.setVisibility(View.GONE);
        offlinePanel.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (offlinePanel.getVisibility() == View.VISIBLE && webView != null && webView.canGoBack()) {
            offlinePanel.setVisibility(View.GONE);
            webView.goBack();
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBackPressedAt < 1800) {
            super.onBackPressed();
        } else {
            lastBackPressedAt = now;
            Toast.makeText(this, "اضغط رجوع مرة أخرى للخروج", Toast.LENGTH_SHORT).show();
        }
    }
}
