package com.tager.marketplace;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SafeBrowsingResponse;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;

import java.util.Locale;

/**
 * WebView guard that sanitizes links before they can leave the Tager runtime.
 *
 * TagerActivity still owns all normal page callbacks and internal navigation.
 * This wrapper only intercepts external/deep-link schemes that need additional
 * validation, then delegates all other callbacks to the client supplied by the
 * activity.
 */
public final class TagerSecureWebView extends WebView {
    private WebViewClient delegatedClient;

    public TagerSecureWebView(@NonNull Context context) {
        super(context);
    }

    public TagerSecureWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TagerSecureWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        delegatedClient = client == null ? new WebViewClient() : client;
        super.setWebViewClient(new GuardedClient());
    }

    private final class GuardedClient extends WebViewClient {
        @Override
        @TargetApi(Build.VERSION_CODES.N)
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (request == null || request.getUrl() == null) return true;
            Boolean handled = handleGuardedNavigation(request.getUrl());
            return handled != null ? handled : delegatedClient.shouldOverrideUrlLoading(view, request);
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url == null) return true;
            Uri uri;
            try {
                uri = Uri.parse(url);
            } catch (RuntimeException ignored) {
                return true;
            }
            Boolean handled = handleGuardedNavigation(uri);
            return handled != null ? handled : delegatedClient.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            delegatedClient.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageCommitVisible(WebView view, String url) {
            delegatedClient.onPageCommitVisible(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            delegatedClient.onPageFinished(view, url);
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            delegatedClient.doUpdateVisitedHistory(view, url, isReload);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            delegatedClient.onReceivedError(view, request, error);
        }

        @Override
        public void onReceivedHttpError(
                WebView view,
                WebResourceRequest request,
                WebResourceResponse errorResponse) {
            delegatedClient.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            delegatedClient.onReceivedSslError(view, handler, error);
        }

        @Override
        public void onSafeBrowsingHit(
                WebView view,
                WebResourceRequest request,
                int threatType,
                SafeBrowsingResponse callback) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                delegatedClient.onSafeBrowsingHit(view, request, threatType, callback);
            } else if (callback != null) {
                callback.backToSafety(true);
            }
        }

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return delegatedClient.onRenderProcessGone(view, detail);
            }
            return true;
        }
    }

    /**
     * @return null when TagerActivity should continue handling the URL,
     *         true when this guard consumed or blocked it.
     */
    private Boolean handleGuardedNavigation(Uri uri) {
        if (uri == null) return true;
        String raw = uri.toString();
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);

        if (TagerTrustedLinkPolicy.isTrustedUrl(raw) || "tager".equals(scheme)) {
            return null;
        }

        if ("intent".equals(scheme)) {
            openSanitizedIntent(raw);
            return true;
        }

        if (TagerExternalLinkPolicy.isBlockedWebViewScheme(scheme)) {
            showBlockedLinkMessage();
            return true;
        }

        if ("http".equals(scheme) || "https".equals(scheme)) {
            return null;
        }

        if (TagerExternalLinkPolicy.isSafeExternalUri(raw)) {
            openSafeExternalUri(uri);
            return true;
        }

        if (!scheme.isEmpty()) {
            showBlockedLinkMessage();
            return true;
        }
        return null;
    }

    private void openSanitizedIntent(String raw) {
        if (!TagerExternalLinkPolicy.isSafeIntentUri(raw)) {
            showBlockedLinkMessage();
            return;
        }

        try {
            Intent parsed = Intent.parseUri(raw, Intent.URI_INTENT_SCHEME);
            String fallback = TagerIntentLinkSanitizer.safeBrowserFallback(parsed);
            Intent clean = TagerIntentLinkSanitizer.sanitize(parsed);

            if (clean != null) {
                if (!(getContext() instanceof Activity)) {
                    clean.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                try {
                    getContext().startActivity(clean);
                    return;
                } catch (ActivityNotFoundException | SecurityException ignored) {
                    // Use the validated browser fallback below when available.
                }
            }

            if (fallback != null) {
                openSafeFallback(fallback);
                return;
            }
            Toast.makeText(getContext(), "التطبيق المطلوب غير مثبت", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
            showBlockedLinkMessage();
        }
    }

    private void openSafeFallback(String fallback) {
        if (!TagerExternalLinkPolicy.isSafeBrowserFallback(fallback)) {
            showBlockedLinkMessage();
            return;
        }
        if (TagerTrustedLinkPolicy.isTrustedUrl(fallback)) {
            loadUrl(fallback);
            return;
        }
        openExternalWebLink(Uri.parse(fallback));
    }

    private void openSafeExternalUri(Uri uri) {
        try {
            Intent external = new Intent(Intent.ACTION_VIEW, uri);
            external.addCategory(Intent.CATEGORY_BROWSABLE);
            external.setComponent(null);
            external.setSelector(null);
            if (!(getContext() instanceof Activity)) {
                external.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            getContext().startActivity(external);
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(getContext(), "لا يوجد تطبيق لفتح الرابط", Toast.LENGTH_SHORT).show();
        }
    }

    private void openExternalWebLink(Uri uri) {
        try {
            CustomTabsIntent customTabs = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setUrlBarHidingEnabled(true)
                    .setToolbarColor(getResources().getColor(R.color.tager_teal, getContext().getTheme()))
                    .setNavigationBarColor(getResources().getColor(R.color.white, getContext().getTheme()))
                    .build();
            customTabs.launchUrl(getContext(), uri);
        } catch (RuntimeException error) {
            openSafeExternalUri(uri);
        }
    }

    private void showBlockedLinkMessage() {
        Toast.makeText(getContext(), "تم حظر رابط غير آمن", Toast.LENGTH_SHORT).show();
    }
}
