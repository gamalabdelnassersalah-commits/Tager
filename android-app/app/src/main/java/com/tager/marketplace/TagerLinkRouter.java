package com.tager.marketplace;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Android adapter around the pure-Java TagerTrustedLinkPolicy.
 * Only verified production HTTPS URLs or sanitized Tager commands may enter
 * the single Tager runtime.
 */
final class TagerLinkRouter {
    static final String PRODUCTION_HOST = TagerTrustedLinkPolicy.PRODUCTION_HOST;
    static final String EXTRA_TARGET_URL = "tager_target_url";
    private static final int MAX_CUSTOM_URI_LENGTH = 2048;

    private TagerLinkRouter() { }

    static boolean isTrustedProductionUrl(Uri uri) {
        return uri != null && TagerTrustedLinkPolicy.isTrustedUrl(uri.toString());
    }

    static Uri findTrustedProductionUrl(String text) {
        String trusted = TagerTrustedLinkPolicy.findTrustedUrl(text);
        return trusted == null ? null : Uri.parse(trusted);
    }

    static Intent buildOpenIntent(Context context, Uri target) {
        Intent intent;
        if (isTrustedProductionUrl(target)) {
            intent = new Intent(context, TagerActivity.class);
            intent.putExtra(EXTRA_TARGET_URL, target.toString());
        } else {
            Uri safeCustom = canonicalizeTagerUri(target);
            if (safeCustom != null) {
                intent = new Intent(Intent.ACTION_VIEW, safeCustom, context, TagerActivity.class);
            } else {
                intent = new Intent(context, TagerActivity.class);
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    static Uri safeNotificationTarget(String url, String fallbackPage) {
        Uri trusted = findTrustedProductionUrl(url);
        if (trusted != null) return trusted;
        return pageUri(fallbackPage);
    }

    static Uri canonicalizeTagerUri(Uri target) {
        if (target == null || !"tager".equalsIgnoreCase(target.getScheme())) return null;
        String raw = target.toString();
        if (raw.length() > MAX_CUSTOM_URI_LENGTH || containsControl(raw) || raw.indexOf('\\') >= 0) return null;
        if (target.isOpaque() || target.getUserInfo() != null || target.getPort() != -1) return null;

        String host = target.getHost();
        if (host == null || host.isEmpty()) return null;
        if ("share".equalsIgnoreCase(host)) {
            return Uri.parse("tager://share");
        }

        String page;
        if ("open".equalsIgnoreCase(host)) {
            page = target.getLastPathSegment();
        } else {
            page = host;
        }
        return pageUri(page);
    }

    static Uri pageUri(String page) {
        return Uri.parse("tager://open/" + sanitizePage(page));
    }

    static String sanitizePage(String page) {
        if (page == null || !page.matches("[A-Za-z0-9_-]{1,64}")) return "home";
        return page;
    }

    private static boolean containsControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x1F || c == 0x7F) return true;
        }
        return false;
    }
}
