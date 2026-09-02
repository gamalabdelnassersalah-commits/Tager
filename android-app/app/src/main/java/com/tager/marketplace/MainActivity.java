package com.tager.marketplace;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.RenderProcessGoneDetail;
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
    private static final String EXTRA_RECOVERY_PAGE = "tager_recovery_page";
    private static final String PREFS_NAME = "tager_app_state";
    private static final String PREF_LAST_PAGE = "last_page";
    private static final String PREF_LAST_GOOD_URL = "last_good_url";
    private static final long NAV_DEBOUNCE_MS = 260L;
    private static final long SLOW_LOAD_WARNING_MS = 9000L;
    private static final long CAMERA_CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L;

    private WebView webView;
    private ProgressBar progressBar;
    private View nativeLoading;
    private View offlinePanel;
    private TextView statusBanner;
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
    private long lastNavigationAt = 0L;
    private String lastNavigationPage = "";
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean networkCallbackRegistered = false;
    private boolean lastKnownOnline = false;
    private boolean lowRamDevice = false;
    private SharedPreferences preferences;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable slowLoadWarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        nativeLoading = findViewById(R.id.nativeLoading);
        offlinePanel = findViewById(R.id.offlinePanel);
        statusBanner = findViewById(R.id.statusBanner);
        navHome = findViewById(R.id.navHome);
        navProducts = findViewById(R.id.navProducts);
        navSuppliers = findViewById(R.id.navSuppliers);
        navTrack = findViewById(R.id.navTrack);
        navCart = findViewById(R.id.navCart);
        Button retryButton = findViewById(R.id.retryButton);

        cleanupCameraCache();
        retryButton.setOnClickListener(v -> retryCurrentPage());
        configureNativeNavigation();
        configureWebView();
        lastKnownOnline = isOnline();
        registerConnectivityWatcher();

        if (savedInstanceState == null) {
            String requestedPage = resolveRequestedPage(getIntent());
            updateSelectedNavigation(requestedPage);
            showLoading(true);
            if (!lastKnownOnline) {
                webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                showStatus("لا يوجد اتصال مؤكد — سيتم استخدام المحتوى المحفوظ إن توفر");
            }
            webView.loadUrl(getPageUrl(requestedPage));
        } else {
            webView.restoreState(savedInstanceState);
            firstPageLoaded = webView.getUrl() != null;
            showLoading(false);
            syncNavigationFromUrl(webView.getUrl());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        navigateTo(resolveRequestedPage(intent));
    }

    private String getPageUrl(String page) {
        return HOME_BASE_URL + "?tager_app=android&app_version=" + BuildConfig.VERSION_NAME + "#" + sanitizePage(page);
    }

    private String sanitizePage(String page) {
        if (page == null || page.isEmpty()) return "home";
        if (!page.matches("[A-Za-z0-9_-]{1,64}")) return "home";
        return page;
    }

    private String pageFromUrl(String url) {
        if (url == null || url.isEmpty()) return "home";
        try {
            return sanitizePage(Uri.parse(url).getFragment());
        } catch (Exception ignored) {
            return "home";
        }
    }

    private String resolveRequestedPage(Intent intent) {
        if (intent != null) {
            String recovery = intent.getStringExtra(EXTRA_RECOVERY_PAGE);
            if (recovery != null && !recovery.isEmpty()) return sanitizePage(recovery);

            Uri data = intent.getData();
            if (data != null) {
                String scheme = data.getScheme() == null ? "" : data.getScheme().toLowerCase();
                if ("tager".equals(scheme)) {
                    String host = data.getHost();
                    if (host != null && !host.isEmpty() && !"open".equalsIgnoreCase(host)) return sanitizePage(host);
                    String lastPath = data.getLastPathSegment();
                    if (lastPath != null && !lastPath.isEmpty()) return sanitizePage(lastPath);
                }
                if (("http".equals(scheme) || "https".equals(scheme)) && isTagerHost(data.getHost())) {
                    String fragment = data.getFragment();
                    return fragment == null || fragment.isEmpty() ? "home" : sanitizePage(fragment);
                }
            }
        }
        return sanitizePage(preferences == null ? "home" : preferences.getString(PREF_LAST_PAGE, "home"));
    }

    private void rememberPage(String page) {
        page = sanitizePage(page);
        if (preferences == null) return;
        String current = preferences.getString(PREF_LAST_PAGE, "home");
        if (!page.equals(current)) preferences.edit().putString(PREF_LAST_PAGE, page).apply();
    }

    private void rememberGoodUrl(String url) {
        if (preferences == null || !isTagerUrl(url)) return;
        String current = preferences.getString(PREF_LAST_GOOD_URL, "");
        if (!url.equals(current)) preferences.edit().putString(PREF_LAST_GOOD_URL, url).apply();
    }

    private void configureNativeNavigation() {
        navHome.setOnClickListener(v -> navigateTo("home"));
        navProducts.setOnClickListener(v -> navigateTo("products"));
        navSuppliers.setOnClickListener(v -> navigateTo("suppliers"));
        navTrack.setOnClickListener(v -> navigateTo("track"));
        navCart.setOnClickListener(v -> navigateTo("cart"));
        updateSelectedNavigation(resolveRequestedPage(getIntent()));
    }

    private void navigateTo(String page) {
        page = sanitizePage(page);
        if (webView == null) return;

        String currentUrl = webView.getUrl();
        String currentPage = pageFromUrl(currentUrl);
        if (firstPageLoaded && isTagerUrl(currentUrl) && page.equals(currentPage)) {
            rememberPage(page);
            updateSelectedNavigation(page);
            return;
        }

        long now = System.currentTimeMillis();
        if (page.equals(lastNavigationPage) && now - lastNavigationAt < NAV_DEBOUNCE_MS) return;
        lastNavigationPage = page;
        lastNavigationAt = now;

        rememberPage(page);
        if (offlinePanel != null) offlinePanel.setVisibility(View.GONE);
        updateSelectedNavigation(page);

        if (firstPageLoaded && isTagerUrl(currentUrl)) {
            String safePage = page.replace("'", "");
            webView.evaluateJavascript("(function(){var p='" + safePage + "';if(location.hash!=='#'+p){location.hash=p;}else if(typeof window.go==='function'){window.go(p);}})();", null);
            return;
        }

        webView.getSettings().setCacheMode(isOnline() ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);
        showLoading(true);
        webView.loadUrl(getPageUrl(page));
    }

    private boolean isTagerHost(String host) {
        if (host == null || host.isEmpty()) return false;
        String normalized = host.toLowerCase();
        return "tager-new.vercel.app".equals(normalized) || normalized.endsWith(".tager-new.vercel.app");
    }

    private boolean isTagerUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        try {
            return isTagerHost(Uri.parse(url).getHost());
        } catch (Exception ignored) {
            return false;
        }
    }

    private void syncNavigationFromUrl(String url) {
        if (url == null) return;
        String page = pageFromUrl(url);
        lastNavigationPage = page;
        rememberPage(page);
        updateSelectedNavigation(page);
    }

    private void updateSelectedNavigation(String page) {
        page = sanitizePage(page);
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

        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        lowRamDevice = activityManager != null && activityManager.isLowRamDevice();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !lowRamDevice) s.setOffscreenPreRaster(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.setSafeBrowsingEnabled(true);
            webView.setRendererPriorityPolicy(lowRamDevice ? WebView.RENDERER_PRIORITY_WAIVED : WebView.RENDERER_PRIORITY_BOUND, lowRamDevice);
        }

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
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
                if (offlinePanel != null) offlinePanel.setVisibility(View.GONE);
                if (!firstPageLoaded) showLoading(true);
                scheduleSlowLoadWarning();
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                firstPageLoaded = true;
                showLoading(false);
                hideStatus();
                if (offlinePanel != null) offlinePanel.setVisibility(View.GONE);
                syncNavigationFromUrl(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                cancelSlowLoadWarning();
                firstPageLoaded = true;
                showLoading(false);
                if (offlinePanel != null) offlinePanel.setVisibility(View.GONE);
                if (isOnline()) view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                rememberGoodUrl(url);
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
                if (request != null && request.isForMainFrame()) {
                    cancelSlowLoadWarning();
                    if (!isOnline()) showOffline();
                    else showStatus("تعذر تحميل الصفحة. اضغط إعادة المحاولة إذا استمرت المشكلة");
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                cancelSlowLoadWarning();
                String recoveryPage = pageFromUrl(view == null ? null : view.getUrl());
                rememberPage(recoveryPage);
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                    fileCallback = null;
                }
                restartAfterRendererFailure(recoveryPage);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                if (!firstPageLoaded && newProgress >= 60) showLoading(false);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = filePathCallback;
                openFileAndCameraChooser(fileChooserParams);
                return true;
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                startDownload(url, userAgent, contentDisposition, mimetype);
            }
        });

        webView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) touchStartY = event.getY();
            else if (event.getAction() == MotionEvent.ACTION_UP) {
                float distance = event.getY() - touchStartY;
                if (webView.getScrollY() == 0 && distance > 460) {
                    if (isOnline()) {
                        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                        webView.reload();
                    } else showOffline();
                }
            }
            return false;
        });
    }

    private void scheduleSlowLoadWarning() {
        cancelSlowLoadWarning();
        slowLoadWarning = () -> {
            if (webView != null && webView.getProgress() < 85) {
                showStatus(isOnline() ? "التحميل أبطأ من المعتاد — جاري إكمال الصفحة" : "الاتصال غير مستقر — نحاول استخدام المحتوى المحفوظ");
            }
        };
        mainHandler.postDelayed(slowLoadWarning, SLOW_LOAD_WARNING_MS);
    }

    private void cancelSlowLoadWarning() {
        if (slowLoadWarning != null) {
            mainHandler.removeCallbacks(slowLoadWarning);
            slowLoadWarning = null;
        }
    }

    private void showStatus(String message) {
        if (statusBanner == null) return;
        statusBanner.setText(message);
        statusBanner.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        if (statusBanner != null) statusBanner.setVisibility(View.GONE);
    }

    private void restartAfterRendererFailure(String page) {
        Intent restart = new Intent(this, MainActivity.class);
        restart.putExtra(EXTRA_RECOVERY_PAGE, sanitizePage(page));
        restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(restart);
    }

    private void registerConnectivityWatcher() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) { handleConnectivityChanged(); }
            @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) { handleConnectivityChanged(); }
            @Override public void onLost(Network network) { handleConnectivityChanged(); }
        };

        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
            networkCallbackRegistered = true;
        } catch (Exception ignored) {
            networkCallbackRegistered = false;
        }
    }

    private void handleConnectivityChanged() {
        runOnUiThread(() -> {
            boolean online = isOnline();
            boolean restored = online && !lastKnownOnline;
            lastKnownOnline = online;

            if (webView != null) webView.getSettings().setCacheMode(online ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);
            if (!online) showStatus("الاتصال بالإنترنت غير متاح حاليًا");
            else if (restored) {
                showStatus("عاد الاتصال بالإنترنت");
                mainHandler.postDelayed(this::hideStatus, 1300L);
            }

            if (restored && offlinePanel != null && offlinePanel.getVisibility() == View.VISIBLE && webView != null) retryCurrentPage();
        });
    }

    private void unregisterConnectivityWatcher() {
        if (!networkCallbackRegistered || connectivityManager == null || networkCallback == null) return;
        try { connectivityManager.unregisterNetworkCallback(networkCallback); } catch (Exception ignored) {}
        networkCallbackRegistered = false;
    }

    private void injectAppPresentation(WebView view) {
        String script = "(function(){" +
                "document.documentElement.classList.add('tager-native-android');" +
                "document.body.classList.add('tager-native-android');" +
                "document.documentElement.style.webkitTextSizeAdjust='100%';" +
                "if(!window.__tagerNativeLinks){window.__tagerNativeLinks=true;document.addEventListener('click',function(e){var t=e.target;var a=t&&t.closest?t.closest('a[target=\\\"_blank\\\"]'):null;if(a){a.target='_self';}},true);}" +
                "})();";
        view.evaluateJavascript(script, null);
    }

    private boolean handleNavigation(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

        if (("http".equals(scheme) || "https".equals(scheme)) && isTagerHost(host)) return false;

        try {
            if ("tager".equals(scheme)) {
                navigateTo(resolvePageFromTagerUri(uri));
                return true;
            }
            if ("intent".equals(scheme)) {
                Intent parsedIntent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                parsedIntent.addCategory(Intent.CATEGORY_BROWSABLE);
                parsedIntent.setComponent(null);
                parsedIntent.setSelector(null);
                if (parsedIntent.resolveActivity(getPackageManager()) != null) startActivity(parsedIntent);
                else {
                    String fallbackUrl = parsedIntent.getStringExtra("browser_fallback_url");
                    if (fallbackUrl != null && !fallbackUrl.isEmpty()) handleNavigation(Uri.parse(fallbackUrl));
                    else Toast.makeText(this, "التطبيق المطلوب غير مثبت", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            if ("tel".equals(scheme) || "mailto".equals(scheme) || "sms".equals(scheme) || "whatsapp".equals(scheme) || "market".equals(scheme) || "http".equals(scheme) || "https".equals(scheme)) {
                Intent externalIntent = new Intent(Intent.ACTION_VIEW, uri);
                if (externalIntent.resolveActivity(getPackageManager()) != null) startActivity(externalIntent);
                else Toast.makeText(this, "لا يوجد تطبيق لفتح الرابط", Toast.LENGTH_SHORT).show();
                return true;
            }
        } catch (Exception ignored) {
            Toast.makeText(this, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private String resolvePageFromTagerUri(Uri uri) {
        if (uri == null) return "home";
        String host = uri.getHost();
        if (host != null && !host.isEmpty() && !"open".equalsIgnoreCase(host)) return sanitizePage(host);
        return sanitizePage(uri.getLastPathSegment());
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            else request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName);
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
            Toast.makeText(this, "بدأ تحميل " + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            try {
                Intent externalIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (externalIntent.resolveActivity(getPackageManager()) != null) startActivity(externalIntent);
                else Toast.makeText(this, "تعذر تحميل الملف", Toast.LENGTH_SHORT).show();
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

    private void cleanupCameraCache() {
        try {
            File dir = new File(getCacheDir(), "camera");
            File[] files = dir.listFiles();
            if (files == null) return;
            long cutoff = System.currentTimeMillis() - CAMERA_CACHE_MAX_AGE_MS;
            for (File file : files) if (file.isFile() && file.lastModified() < cutoff) file.delete();
        } catch (Exception ignored) {}
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
                } else if (data.getData() != null) result = new Uri[]{data.getData()};
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
        lastKnownOnline = true;
        hideStatus();
        if (offlinePanel != null) offlinePanel.setVisibility(View.GONE);
        if (webView == null) return;
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        String currentUrl = webView.getUrl();
        if (currentUrl == null || currentUrl.startsWith("about:")) {
            showLoading(true);
            String lastPage = preferences == null ? "home" : preferences.getString(PREF_LAST_PAGE, "home");
            webView.loadUrl(getPageUrl(lastPage));
        } else webView.reload();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void showLoading(boolean show) {
        if (nativeLoading != null) nativeLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showOffline() {
        cancelSlowLoadWarning();
        showLoading(false);
        showStatus("أنت غير متصل بالإنترنت");
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (offlinePanel != null) offlinePanel.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
        if (offlinePanel != null && offlinePanel.getVisibility() == View.VISIBLE && isOnline()) retryCurrentPage();
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            rememberPage(pageFromUrl(webView.getUrl()));
            webView.onPause();
            webView.pauseTimers();
        }
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (webView == null) return;
        if (level >= TRIM_MEMORY_RUNNING_LOW && webView.getUrl() != null) rememberGoodUrl(webView.getUrl());
        if (level >= TRIM_MEMORY_UI_HIDDEN) webView.pauseTimers();
    }

    @Override
    protected void onDestroy() {
        cancelSlowLoadWarning();
        mainHandler.removeCallbacksAndMessages(null);
        unregisterConnectivityWatcher();
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }
        if (webView != null) {
            try {
                webView.stopLoading();
                webView.removeAllViews();
                webView.destroy();
            } catch (Exception ignored) {}
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (offlinePanel != null && offlinePanel.getVisibility() == View.VISIBLE) {
            offlinePanel.setVisibility(View.GONE);
            hideStatus();
            if (webView != null && webView.canGoBack()) webView.goBack();
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        String currentPage = webView == null ? "home" : pageFromUrl(webView.getUrl());
        if (!"home".equals(currentPage)) {
            navigateTo("home");
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBackPressedAt < 1800) moveTaskToBack(true);
        else {
            lastBackPressedAt = now;
            Toast.makeText(this, "اضغط رجوع مرة أخرى للخروج", Toast.LENGTH_SHORT).show();
        }
    }
}
