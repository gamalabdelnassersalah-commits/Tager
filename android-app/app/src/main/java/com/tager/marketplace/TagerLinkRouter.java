package com.tager.marketplace;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small, dependency-free router for links entering Tager from Android surfaces.
 * It deliberately accepts only the production HTTPS host so shared text and
 * future notification payloads cannot redirect the app to an arbitrary site.
 */
final class TagerLinkRouter {
    static final String PRODUCTION_HOST = "tager-new.vercel.app";
    private static final Pattern URL_PATTERN = Pattern.compile("https://[^\\s<>\\\"]+", Pattern.CASE_INSENSITIVE);

    private TagerLinkRouter() { }

    static boolean isTrustedProductionUrl(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return "https".equals(scheme) && PRODUCTION_HOST.equals(host);
    }

    static Uri findTrustedProductionUrl(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        String value = text.trim();
        try {
            Uri direct = Uri.parse(value);
            if (isTrustedProductionUrl(direct)) return direct;
        } catch (RuntimeException ignored) {
        }

        Matcher matcher = URL_PATTERN.matcher(value);
        while (matcher.find()) {
            String candidate = trimTrailingPunctuation(matcher.group());
            try {
                Uri uri = Uri.parse(candidate);
                if (isTrustedProductionUrl(uri)) return uri;
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    static Intent buildOpenIntent(Context context, Uri trustedUrl) {
        Intent intent;
        if (isTrustedProductionUrl(trustedUrl)) {
            intent = new Intent(Intent.ACTION_VIEW, trustedUrl, context, TagerActivity.class);
        } else {
            intent = new Intent(context, TagerActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    static Uri safeNotificationTarget(String url, String fallbackPage) {
        Uri trusted = findTrustedProductionUrl(url);
        if (trusted != null) return trusted;
        String page = sanitizePage(fallbackPage);
        return Uri.parse("tager://" + page);
    }

    static String sanitizePage(String page) {
        if (page == null || !page.matches("[A-Za-z0-9_-]{1,64}")) return "home";
        return page;
    }

    private static String trimTrailingPunctuation(String value) {
        if (value == null) return "";
        String result = value;
        while (!result.isEmpty()) {
            char last = result.charAt(result.length() - 1);
            if (last == '.' || last == ',' || last == ';' || last == ')' || last == ']' || last == '}') {
                result = result.substring(0, result.length() - 1);
            } else {
                break;
            }
        }
        return result;
    }
}
