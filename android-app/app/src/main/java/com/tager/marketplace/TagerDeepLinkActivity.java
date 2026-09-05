package com.tager.marketplace;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * Dedicated verified-App-Link entry point.
 *
 * TagerActivity remains the stable launcher/runtime. This subclass reuses that
 * complete runtime (cookies, uploads, downloads, offline recovery and native
 * navigation) and then replaces the generic hash-only navigation with the full
 * verified production URL so product/order/vendor/RFQ identifiers in the path,
 * query string and fragment are not lost.
 */
public final class TagerDeepLinkActivity extends TagerActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String target = resolveTrustedTarget(getIntent());
        super.onCreate(savedInstanceState);
        loadTrustedTarget(target);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String target = resolveTrustedTarget(intent);
        super.onNewIntent(intent);
        setIntent(intent);
        loadTrustedTarget(target);
    }

    private String resolveTrustedTarget(Intent intent) {
        if (intent == null) return null;
        Uri data = intent.getData();
        if (data == null) return null;
        String raw = data.toString();
        return TagerTrustedLinkPolicy.isTrustedUrl(raw) ? raw : null;
    }

    private void loadTrustedTarget(String target) {
        String contextual = TagerTrustedLinkPolicy.withAndroidContext(target, BuildConfig.VERSION_NAME);
        if (contextual == null) return;

        WebView view = findViewById(R.id.webView);
        if (view == null) return;
        if (contextual.equals(view.getUrl())) return;

        WebSettings settings = view.getSettings();
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        view.loadUrl(contextual);
    }
}
