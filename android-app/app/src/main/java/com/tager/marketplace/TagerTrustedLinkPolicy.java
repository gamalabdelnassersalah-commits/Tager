package com.tager.marketplace;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java trust policy for URLs entering Tager from shares, notifications,
 * App Links, and other Android surfaces. Keeping this class Android-free makes
 * the security rules independently unit-testable on every CI build.
 */
final class TagerTrustedLinkPolicy {
    static final String PRODUCTION_HOST = "tager-new.vercel.app";
    private static final int MAX_URL_LENGTH = 4096;
    private static final Pattern HTTPS_URL = Pattern.compile(
            "https://[^\\s<>\\\"']+",
            Pattern.CASE_INSENSITIVE);

    private TagerTrustedLinkPolicy() { }

    static boolean isTrustedUrl(String value) {
        if (value == null) return false;
        String candidate = value.trim();
        if (candidate.isEmpty() || candidate.length() > MAX_URL_LENGTH) return false;
        if (containsControlCharacter(candidate) || containsEncodedControl(candidate)) return false;
        if (candidate.indexOf('\\\\') >= 0) return false;

        try {
            URI uri = new URI(candidate);
            if (uri.isOpaque()) return false;
            if (uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
            if (uri.getHost() == null || !PRODUCTION_HOST.equalsIgnoreCase(uri.getHost())) return false;
            if (uri.getRawUserInfo() != null) return false;
            int port = uri.getPort();
            if (port != -1 && port != 443) return false;
            return true;
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return false;
        }
    }

    static String findTrustedUrl(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        String direct = trimTrailingPunctuation(text.trim());
        if (isTrustedUrl(direct)) return direct;

        Matcher matcher = HTTPS_URL.matcher(text);
        while (matcher.find()) {
            String candidate = trimTrailingPunctuation(matcher.group());
            if (isTrustedUrl(candidate)) return candidate;
        }
        return null;
    }

    private static boolean containsControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x1F || c == 0x7F) return true;
        }
        return false;
    }

    private static boolean containsEncodedControl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("%00") || lower.contains("%0a") || lower.contains("%0d");
    }

    private static String trimTrailingPunctuation(String value) {
        if (value == null) return "";
        String result = value.trim();
        while (!result.isEmpty()) {
            char last = result.charAt(result.length() - 1);
            if (last == '.' || last == ',' || last == ';'
                    || last == ')' || last == ']' || last == '}' || last == '>'
                    || last == '،' || last == '؛' || last == '؟' || last == '。') {
                result = result.substring(0, result.length() - 1);
            } else {
                break;
            }
        }
        return result;
    }
}
