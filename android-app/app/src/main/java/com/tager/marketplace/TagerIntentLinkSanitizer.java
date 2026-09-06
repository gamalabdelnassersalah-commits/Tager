package com.tager.marketplace;

import android.content.Intent;
import android.net.Uri;

/**
 * Sanitizes intent:// links before they are allowed to leave the Tager WebView.
 * Parsed flags, categories, selectors, components and arbitrary extras are never
 * forwarded. Only a clean ACTION_VIEW intent is reconstructed from validated data.
 */
final class TagerIntentLinkSanitizer {
    private static final int MAX_INTENT_DATA_LENGTH = 4096;
    private static final String EXTRA_BROWSER_FALLBACK_URL = "browser_fallback_url";

    private TagerIntentLinkSanitizer() { }

    static Intent sanitize(Intent parsed) {
        if (parsed == null) return null;

        String packageName = parsed.getPackage();
        if (packageName != null && !TagerExternalLinkPolicy.isSafeIntentPackage(packageName)) {
            return null;
        }

        Uri data = parsed.getData();
        if (data == null) return null;
        String raw = data.toString();
        if (raw.isEmpty() || raw.length() > MAX_INTENT_DATA_LENGTH || containsControl(raw) || raw.indexOf('\\') >= 0) {
            return null;
        }

        String scheme = data.getScheme();
        if (scheme == null || TagerExternalLinkPolicy.isBlockedWebViewScheme(scheme)) return null;

        boolean supportedScheme = TagerExternalLinkPolicy.isAllowedExternalScheme(scheme);
        // A package-scoped custom app scheme is permitted only when the package
        // name itself is syntactically safe. This preserves legitimate app links
        // without allowing a generic custom scheme to escape the WebView.
        if (!supportedScheme && packageName == null) return null;

        Intent clean = new Intent(Intent.ACTION_VIEW, data);
        clean.addCategory(Intent.CATEGORY_BROWSABLE);
        if (packageName != null) clean.setPackage(packageName);
        return clean;
    }

    static String safeBrowserFallback(Intent parsed) {
        if (parsed == null) return null;
        String fallback = parsed.getStringExtra(EXTRA_BROWSER_FALLBACK_URL);
        return TagerExternalLinkPolicy.isSafeBrowserFallback(fallback) ? fallback.trim() : null;
    }

    private static boolean containsControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x1F || c == 0x7F) return true;
        }
        return false;
    }
}
