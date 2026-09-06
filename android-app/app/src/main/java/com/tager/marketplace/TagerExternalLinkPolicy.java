package com.tager.marketplace;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Pure-Java policy for links that are allowed to leave the Tager WebView. */
final class TagerExternalLinkPolicy {
    private static final int MAX_EXTERNAL_URL_LENGTH = 4096;
    private static final Pattern PACKAGE_NAME = Pattern.compile("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+");
    private static final Pattern ENCODED_CONTROL = Pattern.compile(
            "%(?:0[0-9a-f]|1[0-9a-f]|7f)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ENCODED_BACKSLASH = Pattern.compile("%5c", Pattern.CASE_INSENSITIVE);

    private TagerExternalLinkPolicy() { }

    static boolean isAllowedExternalScheme(String scheme) {
        if (scheme == null) return false;
        switch (scheme.toLowerCase(Locale.ROOT)) {
            case "http":
            case "https":
            case "tel":
            case "mailto":
            case "sms":
            case "whatsapp":
            case "market":
            case "geo":
                return true;
            default:
                return false;
        }
    }

    static boolean isSafeBrowserFallback(String value) {
        if (value == null) return false;
        String candidate = value.trim();
        if (candidate.isEmpty() || candidate.length() > MAX_EXTERNAL_URL_LENGTH) return false;
        if (containsControlCharacter(candidate)
                || candidate.indexOf('\\') >= 0
                || ENCODED_CONTROL.matcher(candidate).find()
                || ENCODED_BACKSLASH.matcher(candidate).find()) {
            return false;
        }
        try {
            URI uri = new URI(candidate);
            String scheme = uri.getScheme();
            if (scheme == null || uri.isOpaque()) return false;
            String lower = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(lower) && !"https".equals(lower)) return false;
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) return false;
            if (uri.getRawUserInfo() != null) return false;
            return true;
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return false;
        }
    }

    static boolean isSafeIntentPackage(String packageName) {
        return packageName != null
                && packageName.length() <= 255
                && PACKAGE_NAME.matcher(packageName).matches();
    }

    static boolean isBlockedWebViewScheme(String scheme) {
        if (scheme == null) return true;
        switch (scheme.toLowerCase(Locale.ROOT)) {
            case "file":
            case "content":
            case "javascript":
            case "data":
            case "about":
            case "blob":
                return true;
            default:
                return false;
        }
    }

    private static boolean containsControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x1F || c == 0x7F) return true;
        }
        return false;
    }
}
