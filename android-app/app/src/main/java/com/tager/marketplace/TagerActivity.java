package com.tager.marketplace;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SafeBrowsingResponse;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Production activity for Tager Android 2.x.
 *
 * This class intentionally owns the full runtime path instead of layering new
 * Android behavior over the legacy MainActivity. That keeps production behavior
 * deterministic while preserving the same marketplace routes, WebView contract,
 * uploads, downloads, connectivity recovery and native navigation.
 */
public class TagerActivity extends Activity {
    private static final String HOME_BASE_URL = "https://tager-new.vercel.app/";
    private static final int FILE_CHOOSER_REQUEST = 4101;
    private static final String EXTRA_RECOVERY_PAGE = "tager_recovery_page";
    private static final String EXTRA_SAFE_MODE = "tager_safe_mode";
    private static final String PREFS_NAME = "tager_app_state";
    private static final String PREF_LAST_PAGE = "last_page";
    private static final String PREF_LAST_GOOD_URL = "last_good_url";
    private static final String PREF_RENDER_CRASH_AT = "render_crash_at";
    private static final String PREF_RENDER_CRASH_COUNT = "render_crash_count";
    private static final long NAV_DEBOUNCE_MS = 260L;
    private static final long SLOW_LOAD_WARNING_MS = 9000L;
    private static final long RENDER_CRASH_WINDOW_MS = 30000L;
    private static final long CAMERA_CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L;

    private WebView webView;
    private TagerSwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View nativeLoading;
    private View offlinePanel;
    private View nativeBottomNav;
    private TextView statusBanner;
    private TextView navHome;
    private TextView navProducts;
    private TextView navSuppliers;
    private TextView navTrack;
    private TextView navCart;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraOutputUri;
    private File cameraOutputFile;
    private SharedPreferences preferences;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private OnBackInvokedCallback backInvokedCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable slowLoadWarning;
    private boolean firstPageLoaded;
    private boolean mainFrameLoadFailed;
    private boolean networkCallbackRegistered;
    private boolean lastKnownOnline;
    private boolean safeMode;
    private long lastBackPressedAt;
    private long lastNavigationAt;
    private String lastNavigationPage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        safeMode = getIntent().getBooleanExtra(EXTRA_SAFE_MODE, false);
        bindViews();
        configureSystemBarsAndInsets();
        cleanupCameraCache();
        configureNativeNavigation();
        configureWebView();
        registerConnectivityWatcher();
        registerModernBackHandler();
        lastKnownOnline = isOnline();

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            firstPageLoaded = webView.getUrl() != null;
            mainFrameLoadFailed = false;
            showLoading(false);
            finishNativeRefresh();
            if (isTagerUrl(webView.getUrl())) syncNavigationFromUrl(webView.getUrl());
            return;
        }

        String page = resolveRequestedPage(getIntent());
        updateSelectedNavigation(page);
        showLoading(true);
        if (!lastKnownOnline) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            showStatus("لا يوجد اتصال مؤكد — سنستخدم المحتوى المحفوظ إن توفر");
        }
        webView.loadUrl(getPageUrl(page));
    }

    private void bindViews() {
        webView = findViewById(R.id.webView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        nativeLoading = findViewById(R.id.nativeLoading);
        offlinePanel = findViewById(R.id.offlinePanel);
        nativeBottomNav = findViewById(R.id.nativeBottomNav);
        statusBanner = findViewById(R.id.statusBanner);
        navHome = findViewById(R.id.navHome);
        navProducts = findViewById(R.id.navProducts);
        navSuppliers = findViewById(R.id.navSuppliers);
        navTrack = findViewById(R.id.navTrack);
        navCart = findViewById(R.id.navCart);
        Button retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> retryCurrentPage());
        statusBanner.setOnClickListener(v -> retryCurrentPage());
    }

    private void configureSystemBarsAndInsets() {
        View content = findViewById(android.R.id.content);
        if (content == null) return;

        int contentLeft = content.getPaddingLeft();
        int contentTop = content.getPaddingTop();
        int contentRight = content.getPaddingRight();
        int contentBottom = content.getPaddingBottom();
        int navLeft = nativeBottomNav == null ? 0 : nativeBottomNav.getPaddingLeft();
        int navTop = nativeBottomNav == null ? 0 : nativeBottomNav.getPaddingTop();
        int navRight = nativeBottomNav == null ? 0 : nativeBottomNav.getPaddingRight();
        int navBottom = nativeBottomNav == null ? 0 : nativeBottomNav.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(
                    contentLeft + bars.left,
                    contentTop + bars.top,
                    contentRight + bars.right,
                    contentBottom);
            if (nativeBottomNav != null) {
                nativeBottomNav.setPadding(navLeft, navTop, navRight, navBottom + bars.bottom);
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    private void registerModernBackHandler() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        backInvokedCallback = this::handleBackNavigation;
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                backInvokedCallback);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasExtra(EXTRA_SAFE_MODE)) {
            safeMode = intent.getBooleanExtra(EXTRA_SAFE_MODE, false);
        }
        navigateTo(resolveRequestedPage(intent));
    }

    private String getPageUrl(String page) {
        return HOME_BASE_URL + "?tager_app=android&app_version=" + BuildConfig.VERSION_NAME + "#" + sanitizePage(page);
    }

    private String sanitizePage(String page) {
        if (page == null || page.isEmpty() || !page.matches("[A-Za-z0-9_-]{1,64}")) return "home";
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
                String scheme = data.getScheme() == null ? "" : data.getScheme().toLowerCase(Locale.ROOT);
                if ("tager".equals(scheme)) {
                    String host = data.getHost();
                    if (host != null && !host.isEmpty() && !"open".equalsIgnoreCase(host)) return sanitizePage(host);
                    return sanitizePage(data.getLastPathSegment());
                }
                if (("http".equals(scheme) || "https".equals(scheme)) && isTagerHost(data.getHost())) {
                    return sanitizePage(data.getFragment());
                }
            }
        }
        return sanitizePage(preferences.getString(PREF_LAST_PAGE, "home"));
    }

    private void rememberPage(String page) {
        page = sanitizePage(page);
        if (!page.equals(preferences.getString(PREF_LAST_PAGE, "home"))) {
            preferences.edit().putString(PREF_LAST_PAGE, page).apply();
        }
    }

    private void rememberGoodUrl(String url) {
        if (mainFrameLoadFailed || url == null || !isTagerUrl(url)) return;
        if (!url.equals(preferences.getString(PREF_LAST_GOOD_URL, ""))) {
            preferences.edit().putString(PREF_LAST_GOOD_URL, url).apply();
        }
    }

    private void configureNativeNavigation() {
        navHome.setOnClickListener(v -> navigateFromButton(v, "home"));
        navProducts.setOnClickListener(v -> navigateFromButton(v, "products"));
        navSuppliers.setOnClickListener(v -> navigateFromButton(v, "suppliers"));
        navTrack.setOnClickListener(v -> navigateFromButton(v, "track"));
        navCart.setOnClickListener(v -> navigateFromButton(v, "cart"));

        View.OnLongClickListener shareCurrentPage = v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            shareCurrentPage();
            return true;
        };
        navHome.setOnLongClickListener(shareCurrentPage);
        navProducts.setOnLongClickListener(shareCurrentPage);
        navSuppliers.setOnLongClickListener(shareCurrentPage);
        navTrack.setOnLongClickListener(shareCurrentPage);
        navCart.setOnLongClickListener(shareCurrentPage);

        updateSelectedNavigation(resolveRequestedPage(getIntent()));
    }

    private void navigateFromButton(View view, String page) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        navigateTo(page);
    }

    private void navigateTo(String page) {
        page = sanitizePage(page);
        if (webView == null) return;

        String currentUrl = webView.getUrl();
        String currentPage = pageFromUrl(currentUrl);
        if (firstPageLoaded && isTagerUrl(currentUrl) && page.equals(currentPage)) {
            updateSelectedNavigation(page);
            rememberPage(page);
            return;
        }

        long now = System.currentTimeMillis();
        if (page.equals(lastNavigationPage) && now - lastNavigationAt < NAV_DEBOUNCE_MS) return;
        lastNavigationPage = page;
        lastNavigationAt = now;
        rememberPage(page);
        updateSelectedNavigation(page);
        offlinePanel.setVisibility(View.GONE);

        if (firstPageLoaded && isTagerUrl(currentUrl)) {
            String safePage = page.replace("'", "");
            webView.evaluateJavascript(
                    "(function(){var p='" + safePage + "';if(location.hash!=='#'+p){location.hash=p;}else if(typeof window.go==='function'){window.go(p);}})();",
                    null);
            return;
        }

        webView.getSettings().setCacheMode(isOnline() ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);
        showLoading(true);
        webView.loadUrl(getPageUrl(page));
    }

    private boolean isTagerHost(String host) {
        if (host == null) return false;
        String value = host.toLowerCase(Locale.ROOT);
        return "tager-new.vercel.app".equals(value) || value.endsWith(".tager-new.vercel.app");
    }

    private boolean isTagerUrl(String url) {
        try {
            if (url == null) return false;
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            return ("http".equals(scheme) || "https".equals(scheme)) && isTagerHost(uri.getHost());
        } catch (Exception ignored) {
            return false;
        }
    }

    private void syncNavigationFromUrl(String url) {
        if (!isTagerUrl(url)) return;
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

    private boolean isLowRamDevice() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        return activityManager != null && activityManager.isLowRamDevice();
    }

    private boolean shouldUseOffscreenPreRaster() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !safeMode && !isLowRamDevice();
    }

    @SuppressWarnings("deprecation")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setGeolocationEnabled(false);
        settings.setSaveFormData(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        int textZoom = Math.round(getResources().getConfiguration().fontScale * 100f);
        settings.setTextZoom(Math.max(85, Math.min(150, textZoom)));
        settings.setDefaultTextEncodingName("utf-8");
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " TagerAndroidApp/" + BuildConfig.VERSION_NAME);

        boolean lowRam = isLowRamDevice();
        if (shouldUseOffscreenPreRaster()) {
            settings.setOffscreenPreRaster(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
            boolean conserveRenderer = lowRam || safeMode;
            webView.setRendererPriorityPolicy(
                    conserveRenderer ? WebView.RENDERER_PRIORITY_WAIVED : WebView.RENDERER_PRIORITY_BOUND,
                    conserveRenderer);
        }

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        WebView.setWebContentsDebuggingEnabled(false);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return request == null || handleNavigation(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return url == null || handleNavigation(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mainFrameLoadFailed = false;
                offlinePanel.setVisibility(View.GONE);
                if (!firstPageLoaded) showLoading(true);
                scheduleSlowLoadWarning();
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                if (mainFrameLoadFailed) return;
                firstPageLoaded = true;
                showLoading(false);
                hideStatus();
                offlinePanel.setVisibility(View.GONE);
                syncNavigationFromUrl(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                cancelSlowLoadWarning();
                finishNativeRefresh();
                if (mainFrameLoadFailed) {
                    showLoading(false);
                    return;
                }
                firstPageLoaded = true;
                showLoading(false);
                offlinePanel.setVisibility(View.GONE);
                if (isOnline()) view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                rememberGoodUrl(url);
                syncNavigationFromUrl(url);
                injectAppPresentation(view);
                scheduleCrashCounterReset();
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                if (!mainFrameLoadFailed) syncNavigationFromUrl(url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    mainFrameLoadFailed = true;
                    cancelSlowLoadWarning();
                    finishNativeRefresh();
                    showLoading(false);
                    if (!isOnline()) showOffline();
                    else showStatus("تعذر تحميل الصفحة — اضغط هنا لإعادة المحاولة");
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                if (request != null && request.isForMainFrame() && errorResponse != null && errorResponse.getStatusCode() >= 400) {
                    mainFrameLoadFailed = true;
                    cancelSlowLoadWarning();
                    finishNativeRefresh();
                    showLoading(false);
                    showStatus("الخادم أعاد خطأ " + errorResponse.getStatusCode() + " — اضغط لإعادة المحاولة");
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                mainFrameLoadFailed = true;
                handler.cancel();
                cancelSlowLoadWarning();
                finishNativeRefresh();
                showLoading(false);
                showStatus("تعذر التحقق من أمان الاتصال — لم يتم فتح الصفحة");
            }

            @Override
            public void onSafeBrowsingHit(
                    WebView view,
                    WebResourceRequest request,
                    int threatType,
                    SafeBrowsingResponse callback) {
                mainFrameLoadFailed = true;
                cancelSlowLoadWarning();
                finishNativeRefresh();
                showLoading(false);
                showStatus("تم حظر صفحة غير آمنة لحماية حسابك وبياناتك");
                if (callback != null) callback.backToSafety(true);
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                mainFrameLoadFailed = true;
                cancelSlowLoadWarning();
                finishNativeRefresh();
                String page = pageFromUrl(view == null ? null : view.getUrl());
                cancelFileChooser(null);
                if (view != null) {
                    try {
                        view.stopLoading();
                        view.removeAllViews();
                        view.destroy();
                    } catch (RuntimeException ignored) {
                    }
                }
                if (view == webView) webView = null;
                restartAfterRendererFailure(page);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (request != null) request.deny();
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (callback == null) return false;
                cancelFileChooser(null);
                fileCallback = callback;
                openFileAndCameraChooser(params);
                return true;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
                startDownload(url, userAgent, contentDisposition, mimeType));
    }

    private void finishNativeRefresh() {
        if (swipeRefresh != null) swipeRefresh.finishRefresh();
    }

    private void scheduleSlowLoadWarning() {
        cancelSlowLoadWarning();
        slowLoadWarning = () -> {
            if (webView != null && webView.getProgress() < 85) {
                showStatus(isOnline()
                        ? "التحميل أبطأ من المعتاد — اضغط لإعادة المحاولة"
                        : "الاتصال غير مستقر — نحاول استخدام المحتوى المحفوظ");
            }
        };
        mainHandler.postDelayed(slowLoadWarning, SLOW_LOAD_WARNING_MS);
    }

    private void cancelSlowLoadWarning() {
        if (slowLoadWarning != null) mainHandler.removeCallbacks(slowLoadWarning);
        slowLoadWarning = null;
    }

    private void showStatus(String message) {
        statusBanner.setText(message);
        statusBanner.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusBanner.setVisibility(View.GONE);
    }

    private void scheduleCrashCounterReset() {
        mainHandler.postDelayed(() -> preferences.edit().putInt(PREF_RENDER_CRASH_COUNT, 0).apply(), 5000L);
    }

    private void restartAfterRendererFailure(String page) {
        long now = System.currentTimeMillis();
        long last = preferences.getLong(PREF_RENDER_CRASH_AT, 0L);
        int count = now - last <= RENDER_CRASH_WINDOW_MS
                ? preferences.getInt(PREF_RENDER_CRASH_COUNT, 0) + 1
                : 1;
        preferences.edit()
                .putLong(PREF_RENDER_CRASH_AT, now)
                .putInt(PREF_RENDER_CRASH_COUNT, count)
                .putString(PREF_LAST_PAGE, sanitizePage(page))
                .apply();

        Intent restart = new Intent(this, TagerActivity.class);
        restart.putExtra(EXTRA_RECOVERY_PAGE, sanitizePage(page));
        restart.putExtra(EXTRA_SAFE_MODE, count >= 2);
        restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(restart);
        finish();
    }

    private void registerConnectivityWatcher() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) { handleConnectivityChanged(); }
            @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) { handleConnectivityChanged(); }
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
            if (webView != null) {
                webView.getSettings().setCacheMode(
                        online ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);
            }
            if (!online) {
                finishNativeRefresh();
                showStatus("الاتصال بالإنترنت غير متاح حاليًا");
            } else if (restored) {
                showStatus("عاد الاتصال بالإنترنت");
                mainHandler.postDelayed(() -> {
                    if (isOnline() && offlinePanel.getVisibility() != View.VISIBLE) hideStatus();
                }, 1300L);
            }
            if (restored && offlinePanel.getVisibility() == View.VISIBLE) retryCurrentPage();
        });
    }

    private void unregisterConnectivityWatcher() {
        if (!networkCallbackRegistered || connectivityManager == null || networkCallback == null) return;
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (Exception ignored) {
        }
        networkCallbackRegistered = false;
    }

    private void injectAppPresentation(WebView view) {
        String script = "(function(){"
                + "document.documentElement.classList.add('tager-native-android');"
                + "document.body.classList.add('tager-native-android');"
                + "if(!window.__tagerNativeLinks){window.__tagerNativeLinks=true;"
                + "document.addEventListener('click',function(e){var t=e.target;"
                + "var a=t&&t.closest?t.closest('a[target=\\\"_blank\\\"]'):null;"
                + "if(a){a.target='_self';}},true);}})();";
        view.evaluateJavascript(script, null);
    }

    private boolean handleNavigation(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (("http".equals(scheme) || "https".equals(scheme)) && isTagerHost(host)) return false;

        try {
            if ("tager".equals(scheme)) {
                if (handleTagerCommand(uri)) return true;
                navigateTo(resolvePageFromTagerUri(uri));
                return true;
            }
            if ("intent".equals(scheme)) {
                Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                parsed.addCategory(Intent.CATEGORY_BROWSABLE);
                parsed.setComponent(null);
                parsed.setSelector(null);
                if (parsed.resolveActivity(getPackageManager()) != null) {
                    startActivity(parsed);
                } else {
                    String fallback = parsed.getStringExtra("browser_fallback_url");
                    if (fallback != null && !fallback.isEmpty()) {
                        Uri fallbackUri = Uri.parse(fallback);
                        String fallbackScheme = fallbackUri.getScheme() == null
                                ? ""
                                : fallbackUri.getScheme().toLowerCase(Locale.ROOT);
                        if ("http".equals(fallbackScheme) || "https".equals(fallbackScheme)) {
                            handleNavigation(fallbackUri);
                        } else {
                            Toast.makeText(this, "رابط الرجوع غير آمن", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "التطبيق المطلوب غير مثبت", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
            if ("http".equals(scheme) || "https".equals(scheme)) {
                openExternalWebLink(uri);
                return true;
            }
            if ("tel".equals(scheme)
                    || "mailto".equals(scheme)
                    || "sms".equals(scheme)
                    || "whatsapp".equals(scheme)
                    || "market".equals(scheme)) {
                Intent external = new Intent(Intent.ACTION_VIEW, uri);
                if (external.resolveActivity(getPackageManager()) != null) {
                    startActivity(external);
                } else {
                    Toast.makeText(this, "لا يوجد تطبيق لفتح الرابط", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        } catch (Exception ignored) {
            Toast.makeText(this, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show();
            return true;
        }

        Toast.makeText(this, "تم حظر رابط غير مدعوم", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void openExternalWebLink(Uri uri) {
        try {
            CustomTabsIntent customTabs = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setUrlBarHidingEnabled(true)
                    .setToolbarColor(getColor(R.color.tager_teal))
                    .setNavigationBarColor(getColor(R.color.white))
                    .build();
            customTabs.launchUrl(this, uri);
        } catch (RuntimeException error) {
            Intent fallback = new Intent(Intent.ACTION_VIEW, uri);
            if (fallback.resolveActivity(getPackageManager()) != null) {
                startActivity(fallback);
            } else {
                Toast.makeText(this, "لا يوجد متصفح لفتح الرابط", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean handleTagerCommand(Uri uri) {
        if (uri == null) return false;
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!"share".equals(host)) return false;

        String url = uri.getQueryParameter("url");
        String text = uri.getQueryParameter("text");
        if (url == null || !isTagerUrl(url)) {
            url = webView == null ? getPageUrl("home") : webView.getUrl();
        }
        shareText(text, url);
        return true;
    }

    private void shareCurrentPage() {
        String url = webView == null ? null : webView.getUrl();
        if (!isTagerUrl(url)) {
            url = getPageUrl(preferences.getString(PREF_LAST_PAGE, "home"));
        }
        shareText("Tager | تاجر", url);
    }

    private void shareText(String text, String url) {
        try {
            String safeText = text == null || text.trim().isEmpty() ? "Tager | تاجر" : text.trim();
            String safeUrl = isTagerUrl(url) ? url : HOME_BASE_URL;
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, "Tager | تاجر");
            share.putExtra(Intent.EXTRA_TEXT, safeText + "\n" + safeUrl);
            startActivity(Intent.createChooser(share, "مشاركة من تاجر"));
        } catch (RuntimeException error) {
            Toast.makeText(this, "تعذر فتح المشاركة", Toast.LENGTH_SHORT).show();
        }
    }

    private String resolvePageFromTagerUri(Uri uri) {
        String host = uri == null ? null : uri.getHost();
        if (host != null && !host.isEmpty() && !"open".equalsIgnoreCase(host)) {
            return sanitizePage(host);
        }
        return sanitizePage(uri == null ? null : uri.getLastPathSegment());
    }

    private void startDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new IllegalArgumentException("Unsupported download scheme");
            }

            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                    .replaceAll("[\\\\/:*?\"<>|]", "_");
            if (fileName.trim().isEmpty()) fileName = "tager-download";
            if (fileName.length() > 120) fileName = fileName.substring(0, 120);

            DownloadManager.Request request = new DownloadManager.Request(uri);
            if (mimeType != null && !mimeType.isEmpty()) request.setMimeType(mimeType);
            request.setTitle(fileName);
            request.setDescription("Tager | تاجر");
            if (userAgent != null && !userAgent.isEmpty()) request.addRequestHeader("User-Agent", userAgent);
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null && !cookie.isEmpty()) request.addRequestHeader("Cookie", cookie);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            } else {
                request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName);
            }
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager == null) throw new IllegalStateException("DownloadManager unavailable");
            manager.enqueue(request);
            Toast.makeText(this, "بدأ تحميل " + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "تعذر بدء التحميل", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean tryStartFileChooser(Intent intent) {
        if (intent == null) return false;
        try {
            startActivityForResult(intent, FILE_CHOOSER_REQUEST);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void cancelFileChooser(String message) {
        ValueCallback<Uri[]> callback = fileCallback;
        fileCallback = null;
        try {
            if (callback != null) callback.onReceiveValue(null);
        } catch (RuntimeException ignored) {
        }
        releaseCameraUriPermissions();
        deletePendingCameraFile();
        cameraOutputUri = null;
        if (message != null && !message.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void openFileAndCameraChooser(WebChromeClient.FileChooserParams params) {
        boolean allowMultiple = params != null
                && params.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE;
        String[] acceptTypes = params == null ? new String[0] : params.getAcceptTypes();
        boolean acceptsImages = acceptsImages(acceptTypes);

        Intent fileIntent;
        if (acceptsImages && !allowMultiple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            fileIntent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            fileIntent.setType("image/*");
        } else try {
            fileIntent = params == null ? null : params.createIntent();
            if (fileIntent == null) throw new IllegalStateException("No file intent");
            fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
            fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        } catch (Exception error) {
            fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
            fileIntent.setType(resolvePrimaryMimeType(acceptTypes));
            if (acceptTypes.length > 1) {
                fileIntent.putExtra(Intent.EXTRA_MIME_TYPES, sanitizeAcceptTypes(acceptTypes));
            }
            fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        }

        Intent cameraIntent = acceptsImages ? createFullResolutionCameraIntent() : null;
        if (params != null && params.isCaptureEnabled() && cameraIntent != null) {
            if (tryStartFileChooser(cameraIntent)) return;
            releaseCameraUriPermissions();
            deletePendingCameraFile();
            cameraOutputUri = null;
            cameraIntent = null;
        }

        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_INTENT, fileIntent);
        chooser.putExtra(Intent.EXTRA_TITLE, acceptsImages ? "اختر ملفًا أو التقط صورة" : "اختر ملفًا");
        if (cameraIntent != null) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
        }
        if (!tryStartFileChooser(chooser)) {
            cancelFileChooser("تعذر فتح الكاميرا أو مدير الملفات");
        }
    }

    private boolean acceptsImages(String[] acceptTypes) {
        if (acceptTypes == null || acceptTypes.length == 0) return true;
        for (String type : acceptTypes) {
            if (type == null
                    || type.trim().isEmpty()
                    || "*/*".equals(type.trim())
                    || type.toLowerCase(Locale.ROOT).startsWith("image/")) {
                return true;
            }
        }
        return false;
    }

    private String resolvePrimaryMimeType(String[] acceptTypes) {
        if (acceptTypes == null || acceptTypes.length == 0) return "*/*";
        for (String type : acceptTypes) {
            if (type != null && type.contains("/") && !type.trim().isEmpty()) return type.trim();
        }
        return "*/*";
    }

    private String[] sanitizeAcceptTypes(String[] acceptTypes) {
        if (acceptTypes == null || acceptTypes.length == 0) return new String[]{"*/*"};
        int count = 0;
        for (String type : acceptTypes) {
            if (type != null && type.contains("/") && !type.trim().isEmpty()) count++;
        }
        if (count == 0) return new String[]{"*/*"};
        String[] result = new String[count];
        int index = 0;
        for (String type : acceptTypes) {
            if (type != null && type.contains("/") && !type.trim().isEmpty()) result[index++] = type.trim();
        }
        return result;
    }

    private Intent createFullResolutionCameraIntent() {
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (camera.resolveActivity(getPackageManager()) == null) return null;
        try {
            File dir = new File(getCacheDir(), "camera");
            if (!dir.exists() && !dir.mkdirs()) return null;
            File image = File.createTempFile("tager_camera_", ".jpg", dir);
            cameraOutputFile = image;
            cameraOutputUri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    image);

            int flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION;
            camera.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputUri);
            camera.setClipData(ClipData.newRawUri("Tager camera", cameraOutputUri));
            camera.addFlags(flags);

            List<ResolveInfo> handlers = getPackageManager().queryIntentActivities(
                    camera,
                    PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo info : handlers) {
                if (info.activityInfo != null && info.activityInfo.packageName != null) {
                    grantUriPermission(info.activityInfo.packageName, cameraOutputUri, flags);
                }
            }
            return camera;
        } catch (IOException | RuntimeException ignored) {
            releaseCameraUriPermissions();
            deletePendingCameraFile();
            cameraOutputUri = null;
            return null;
        }
    }

    private void releaseCameraUriPermissions() {
        if (cameraOutputUri == null) return;
        try {
            revokeUriPermission(
                    cameraOutputUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
    }

    private void deletePendingCameraFile() {
        File pending = cameraOutputFile;
        cameraOutputFile = null;
        if (pending == null) return;
        try {
            if (pending.isFile()) pending.delete();
        } catch (RuntimeException ignored) {
        }
    }

    private void cleanupCameraCache() {
        try {
            File[] files = new File(getCacheDir(), "camera").listFiles();
            if (files == null) return;
            long cutoff = System.currentTimeMillis() - CAMERA_CACHE_MAX_AGE_MS;
            for (File file : files) {
                if (file.isFile() && file.lastModified() < cutoff) file.delete();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || fileCallback == null) return;

        ValueCallback<Uri[]> callback = fileCallback;
        fileCallback = null;
        Uri[] result = null;
        try {
            if (resultCode == RESULT_OK) {
                if (data != null && data.getClipData() != null) {
                    ClipData clips = data.getClipData();
                    result = new Uri[clips.getItemCount()];
                    for (int i = 0; i < clips.getItemCount(); i++) {
                        result[i] = clips.getItemAt(i).getUri();
                    }
                } else if (data != null && data.getData() != null) {
                    result = new Uri[]{data.getData()};
                } else if (cameraOutputUri != null) {
                    result = new Uri[]{cameraOutputUri};
                }
            }
        } catch (RuntimeException ignored) {
            result = null;
        }

        boolean usedCameraOutput = result != null
                && cameraOutputUri != null
                && result.length == 1
                && cameraOutputUri.equals(result[0]);
        try {
            callback.onReceiveValue(result);
        } catch (RuntimeException ignored) {
        } finally {
            releaseCameraUriPermissions();
            if (!usedCameraOutput) deletePendingCameraFile();
            else cameraOutputFile = null;
            cameraOutputUri = null;
        }
    }

    private void retryCurrentPage() {
        if (!isOnline()) {
            showOffline();
            Toast.makeText(this, "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show();
            return;
        }

        lastKnownOnline = true;
        mainFrameLoadFailed = false;
        hideStatus();
        offlinePanel.setVisibility(View.GONE);
        if (webView == null) return;
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        String current = webView.getUrl();
        if (current == null || current.startsWith("about:")) {
            String lastGood = preferences.getString(PREF_LAST_GOOD_URL, "");
            showLoading(true);
            webView.loadUrl(
                    isTagerUrl(lastGood)
                            ? lastGood
                            : getPageUrl(preferences.getString(PREF_LAST_PAGE, "home")));
        } else {
            if (!firstPageLoaded) showLoading(true);
            webView.reload();
        }
    }

    private boolean restoreLastGoodPage() {
        if (webView == null) return false;
        String lastGood = preferences.getString(PREF_LAST_GOOD_URL, "");
        if (!isTagerUrl(lastGood)) return false;
        mainFrameLoadFailed = false;
        webView.getSettings().setCacheMode(
                isOnline() ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);
        showLoading(true);
        webView.loadUrl(lastGood);
        return true;
    }

    private boolean isOnline() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = manager.getNetworkCapabilities(network);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void showLoading(boolean show) {
        nativeLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showOffline() {
        cancelSlowLoadWarning();
        finishNativeRefresh();
        mainFrameLoadFailed = true;
        showLoading(false);
        showStatus("أنت غير متصل بالإنترنت");
        progressBar.setVisibility(View.GONE);
        offlinePanel.setVisibility(View.VISIBLE);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    webView.getSettings().setOffscreenPreRaster(shouldUseOffscreenPreRaster());
                } catch (RuntimeException ignored) {
                }
            }
            webView.onResume();
            webView.resumeTimers();
        }
        if (offlinePanel.getVisibility() == View.VISIBLE && isOnline()) retryCurrentPage();
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            if (!mainFrameLoadFailed) {
                rememberPage(pageFromUrl(webView.getUrl()));
                rememberGoodUrl(webView.getUrl());
            }
            webView.onPause();
            webView.pauseTimers();
        }
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (webView != null && webView.getUrl() != null && !mainFrameLoadFailed) {
            rememberGoodUrl(webView.getUrl());
        }
        if (webView == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && level >= TRIM_MEMORY_RUNNING_LOW) {
            try {
                webView.getSettings().setOffscreenPreRaster(false);
            } catch (RuntimeException ignored) {
            }
        }
        if (level >= TRIM_MEMORY_UI_HIDDEN) webView.pauseTimers();
    }

    @Override
    protected void onDestroy() {
        cancelSlowLoadWarning();
        mainHandler.removeCallbacksAndMessages(null);
        unregisterConnectivityWatcher();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backInvokedCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backInvokedCallback);
            backInvokedCallback = null;
        }
        finishNativeRefresh();
        cancelFileChooser(null);
        if (webView != null) {
            try {
                webView.stopLoading();
                webView.removeAllViews();
                webView.destroy();
            } catch (Exception ignored) {
            }
            webView = null;
        }
        swipeRefresh = null;
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        handleBackNavigation();
    }

    private void handleBackNavigation() {
        if (offlinePanel.getVisibility() == View.VISIBLE) {
            offlinePanel.setVisibility(View.GONE);
            hideStatus();
            if (webView != null && webView.canGoBack()) {
                mainFrameLoadFailed = false;
                webView.goBack();
                return;
            }
            if (restoreLastGoodPage()) return;
            if (!isOnline()) {
                moveTaskToBack(true);
                return;
            }
            navigateTo(preferences.getString(PREF_LAST_PAGE, "home"));
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        String page = webView == null ? "home" : pageFromUrl(webView.getUrl());
        if (!"home".equals(page)) {
            navigateTo("home");
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBackPressedAt < 1800L) {
            moveTaskToBack(true);
        } else {
            lastBackPressedAt = now;
            Toast.makeText(this, "اضغط رجوع مرة أخرى للخروج", Toast.LENGTH_SHORT).show();
        }
    }
}
