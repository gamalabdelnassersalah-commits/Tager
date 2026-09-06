package com.tager.marketplace;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Android adapter around the pure-Java TagerTrustedLinkPolicy.
 * Only verified production HTTPS URLs are allowed to enter Tager as web links.
 */
final class TagerLinkRouter {
    static final String PRODUCTION_HOST = TagerTrustedLinkPolicy.PRODUCTION_HOST;
    static final String EXTRA_TARGET_URL = "tager_target_url";

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
        } else if (target != null && "tager".equalsIgnoreCase(target.getScheme())) {
            intent = new Intent(Intent.ACTION_VIEW, target, context, TagerActivity.class);
        } else {
            intent = new Intent(context, TagerActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    static Uri safeNotificationTarget(String url, String fallbackPage) {
        Uri trusted = findTrustedProductionUrl(url);
        if (trusted != null) return trusted;
        return Uri.parse("tager://" + sanitizePage(fallbackPage));
    }

    static String sanitizePage(String page) {
        if (page == null || !page.matches("[A-Za-z0-9_-]{1,64}")) return "home";
        return page;
    }
}
