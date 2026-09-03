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
 * The refresh gesture is intercepted by this parent before the WebView receives
 * a completed long pull, preventing duplicate refreshes from legacy touch
 * fallbacks. Refresh is debounced, requires validated connectivity and keeps the
 * native indicator visible long enough to communicate that a real reload began.
 */
public class TagerSwipeRefreshLayout extends SwipeRefreshLayout {
    private static final long REFRESH_TIMEOUT_MS = 15000L;
    private static final long PROGRESS_POLL_MS = 120L;
    private static final long MIN_INDICATOR_MS = 650L;
    private static final long MIN_REFRESH_INTERVAL_MS = 1200L;

    private Runnable completionPoll;
    private boolean refreshInFlight;
    private long lastRefreshAt;
    private long refreshStartedAt;

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
        long now = SystemClock.elapsedRealtime();
        if (refreshInFlight || now - lastRefreshAt < MIN_REFRESH_INTERVAL_MS) {
            setRefreshing(refreshInFlight);
            return;
        }

        WebView webView = findWebView();
        if (webView == null) {
            finishRefresh();
            return;
        }
        if (!isOnline()) {
            finishRefresh();
            Toast.makeText(getContext(), "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show();
            return;
        }

        refreshInFlight = true;
        lastRefreshAt = now;
        refreshStartedAt = now;
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.reload();
        scheduleCompletionPoll(webView);
    }

    private void scheduleCompletionPoll(WebView webView) {
        if (completionPoll != null) removeCallbacks(completionPoll);
        completionPoll = () -> {
            if (!refreshInFlight) return;

            long elapsed = SystemClock.elapsedRealtime() - refreshStartedAt;
            boolean minimumVisible = elapsed >= MIN_INDICATOR_MS;
            boolean finished = minimumVisible && webView.getProgress() >= 100;
            boolean timedOut = elapsed >= REFRESH_TIMEOUT_MS;
            if (finished || timedOut || !isAttachedToWindow()) {
                finishRefresh();
                return;
            }
            postDelayed(completionPoll, PROGRESS_POLL_MS);
        };
        postDelayed(completionPoll, PROGRESS_POLL_MS);
    }

    public void finishRefresh() {
        refreshInFlight = false;
        if (completionPoll != null) {
            removeCallbacks(completionPoll);
            completionPoll = null;
        }
        if (isRefreshing()) setRefreshing(false);
    }

    @Override
    protected void onDetachedFromWindow() {
        finishRefresh();
        super.onDetachedFromWindow();
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
