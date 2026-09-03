package com.tager.marketplace;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Native pull-to-refresh surface for the Tager WebView.
 *
 * The legacy activity still contains a fallback touch gesture, but this parent
 * intercepts a real pull gesture first and provides the standard Android refresh
 * indicator. Refresh is only triggered with validated internet connectivity and
 * the indicator stays active until WebView finishes or a timeout is reached.
 */
public class TagerSwipeRefreshLayout extends SwipeRefreshLayout {
    private static final long REFRESH_TIMEOUT_MS = 15000L;
    private static final long PROGRESS_POLL_MS = 120L;

    public TagerSwipeRefreshLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public TagerSwipeRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setColorSchemeResources(R.color.tager_orange, R.color.tager_teal);
        setProgressBackgroundColorSchemeResource(R.color.white);
        setDistanceToTriggerSync(dp(88));
        setSlingshotDistance(dp(72));
        setOnChildScrollUpCallback((parent, child) -> {
            WebView webView = findWebView();
            return webView != null && webView.canScrollVertically(-1);
        });
        setOnRefreshListener(this::refreshWebView);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void refreshWebView() {
        WebView webView = findWebView();
        if (webView == null) {
            setRefreshing(false);
            return;
        }
        if (!isOnline()) {
            setRefreshing(false);
            Toast.makeText(getContext(), "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show();
            return;
        }

        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.reload();
        waitForCompletion(webView, SystemClock.elapsedRealtime());
    }

    private void waitForCompletion(WebView webView, long startedAt) {
        postDelayed(() -> {
            if (!isRefreshing()) return;
            boolean finished = webView.getProgress() >= 100;
            boolean timedOut = SystemClock.elapsedRealtime() - startedAt >= REFRESH_TIMEOUT_MS;
            if (finished || timedOut || !isAttachedToWindow()) {
                setRefreshing(false);
                return;
            }
            waitForCompletion(webView, startedAt);
        }, PROGRESS_POLL_MS);
    }

    @Nullable
    private WebView findWebView() {
        if (getChildCount() == 0) return null;
        View child = getChildAt(0);
        return child instanceof WebView ? (WebView) child : null;
    }

    private boolean isOnline() {
        ConnectivityManager manager =
                (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
