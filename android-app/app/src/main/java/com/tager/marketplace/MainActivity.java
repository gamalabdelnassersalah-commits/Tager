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
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final String HOME_BASE_URL = "https://tager-new.vercel.app/";
    private static final int FILE_CHOOSER_REQUEST = 4101;

    private WebView webView;
    private ProgressBar progressBar;
    private View nativeLoading;
    private View offlinePanel;
    private TextView navHome;
    private TextView navProducts;
    private TextView navSuppliers;
    private TextView navTrack;
    private TextView navCart;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraOutputUri;
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
        navHome = findViewById(R.id.navHome);
        navProducts = findViewById(R.id.navProducts);
        navSuppliers = findViewById(R.id.navSuppliers);
        navTrack = findViewById(R.id.navTrack);
        navCart = findViewById(R.id.navCart);
        Button retryButton = findViewById(R.id.retryButton);

        retryButton.setOnClickListener(v -> retryCurrentPage());
        configureNativeNavigation();
        configureWebView();

        if (savedInstanceState == null) {
            if (isOnline()) {
                showLoading(true);
                webView.loadUrl(getPageUrl("home"));
            } else {
                showOffline();
            }
        } else {
            webView.restoreState(savedInstanceState);
            firstPageLoaded = webView.getUrl() != null;
            showLoading(false);
            syncNavigationFromUrl(webView.getUrl());
        }
    }

    private String getPageUrl(String page) {
        return HOME_BASE_URL + "?tager_app=android&app_version=" + BuildConfig.VERSION_NAME + "#" + page;
    }

    private void configureNativeNavigation() {
        navHome.setOnClickListener(v -> navigateTo("home"));
        navProducts.setOnClickListener(v -> navigateTo("products"));
        navSuppliers.setOnClickListener(v -> navigateTo("suppliers"));
        navTrack.setOnClickListener(v -> navigateTo("track"));
        navCart.setOnClickListener(v -> navigateTo("cart"));
        updateSelectedNavigation("home");
    }

    private void navigateTo(String page) {
        offlinePanel.setVisibility(View.GONE);
        updateSelectedNavigation(page);

        String current = webView.getUrl();
        if (firstPageLoaded && isTagerUrl(current)) {
            String safePage = page.replace("'", "");
            String js = "(function(){var p='" + safePage + "';if(location.hash!=='#'+p){location.hash=p;}else if(typeof window.go==='function'){window.go(p);}})();";
            webView.evaluateJavascript(js, null);
            return;
        }

        if (!isOnline()) {
            showOffline();
            return;
        }

        showLoading(true);
        webView.loadUrl(getPageUrl(page));
    }

    private boolean isTagerUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            return host != null && ("tager-new.vercel.app".equalsIgnoreCase(host) || host.toLowerCase().endsWith(".tager-new.vercel.app"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private void syncNavigationFromUrl(String url) {
        if (url == null) return;
        try {
            String fragment = Uri.parse(url).getFragment();
            if (fragment != null && !fragment.isEmpty()) updateSelectedNavigation(fragment);
        } catch (Exception ignored) {
        }
    }

    private void updateSelectedNavigation(String page) {
        navHome.setSelected("home".equals(page));
        navProducts.setSelected("products".equals(page));
        navSuppliers.setSelected("suppliers".equals(page));
        navTrack.setSelected("track".equals(page));
        navCart.setSelected("cart".equals(page));
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccess(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " TagerAndroidApp/" + BuildConfig.VERSION_NAME);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            s.setOffscreenPreRaster(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.setSafeBrowsingEnabled(true);
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true);
        }

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
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
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                firstPageLoaded = true;
                showLoading(false);
                offlinePanel.setVisibility(View.GONE);
                syncNavigationFromUrl(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                firstPageLoaded = true;
                showLoading(false);
                offlinePanel.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
                syncNavigationFromUrl(url);
                injectAppPresentation(view);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                syncNavigationFromUrl(url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request != null && request.isForMainFrame()) showOffline();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                if (!firstPageLoaded && newProgress >= 65) showLoading(false);
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
                if (webView.getScrollY() == 0 && distance > 420) {
                    if (isOnline()) webView.reload();
                    else showOffline();
                }
            }
            return false;
        });
    }

    private void injectAppPresentation(WebView view) {
        String script = "(function(){" +
                "document.documentElement.classList.add('tager-native-android');" +
                "document.body.classList.add('tager-native-android');" +
                "document.documentElement.style.webkitTextSizeAdjust='100%';" +
                "})();";
        view.evaluateJavascript(script, null);
    }

    private boolean handleNavigation(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

        if (("http".equals(scheme) || "https".equals(scheme)) &&
                ("tager-new.vercel.app".equals(host) || host.endsWith(".tager-new.vercel.app"))) return false;

        try {
            if ("intent".equals(scheme)) {
                startActivity(Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME));
                return true;
            }
            if ("tel".equals(scheme) || "mailto".equals(scheme) || "sms".equals(scheme) ||
                    "whatsapp".equals(scheme) || "market".equals(scheme) ||
                    "http".equals(scheme) || "https".equals(scheme)) {
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

        Intent cameraIntent = createFullResolutionCameraIntent();
        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_INTENT, fileIntent);
        chooser.putExtra(Intent.EXTRA_TITLE, "اختر ملفًا أو التقط صورة");
        if (cameraIntent != null) chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
        startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
    }

    private Intent createFullResolutionCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) == null) return null;
        try {
            File cameraDir = new File(getCacheDir(), "camera");
            if (!cameraDir.exists() && !cameraDir.mkdirs()) return null;
            File imageFile = File.createTempFile("tager_camera_", ".jpg", cameraDir);
            cameraOutputUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", imageFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputUri);
            cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return cameraIntent;
        } catch (IOException e) {
            cameraOutputUri = null;
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || fileCallback == null) return;

        Uri[] result = null;
        if (resultCode == RESULT_OK) {
            if (data != null) {
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    result = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) result[i] = clipData.getItemAt(i).getUri();
                } else if (data.getData() != null) {
                    result = new Uri[]{data.getData()};
                }
            }
            if (result == null && cameraOutputUri != null) result = new Uri[]{cameraOutputUri};
        }

        fileCallback.onReceiveValue(result);
        fileCallback = null;
        cameraOutputUri = null;
    }

    private void retryCurrentPage() {
        if (!isOnline()) {
            showOffline();
            Toast.makeText(this, "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show();
            return;
        }
        offlinePanel.setVisibility(View.GONE);
        String currentUrl = webView.getUrl();
        if (currentUrl == null || currentUrl.startsWith("about:")) {
            showLoading(true);
            webView.loadUrl(getPageUrl("home"));
        } else {
            webView.reload();
        }
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
        if (offlinePanel.getVisibility() == View.VISIBLE) {
            offlinePanel.setVisibility(View.GONE);
            if (webView.canGoBack()) webView.goBack();
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBackPressedAt < 1800) {
            moveTaskToBack(true);
        } else {
            lastBackPressedAt = now;
            Toast.makeText(this, "اضغط رجوع مرة أخرى للخروج", Toast.LENGTH_SHORT).show();
        }
    }
}
