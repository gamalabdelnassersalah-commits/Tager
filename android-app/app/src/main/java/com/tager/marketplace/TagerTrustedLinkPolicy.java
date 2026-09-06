package com.tager.marketplace;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java trust policy for URLs entering Tager from shares, notifications,
 * App Links, startup resume, and other Android surfaces.
 */
final class TagerTrustedLinkPolicy {
    static final String PRODUCTION_HOST = "tager-new.vercel.app";
    private static final int MAX_URL_LENGTH = 4096;
    private static final Pattern HTTPS_URL = Pattern.compile(
            "https://[^\\s<>\\\"']+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ENCODED_CONTROL = Pattern.compile(
            "%(?:0[0-9a-f]|1[0-9a-f]|7f)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ENCODED_BACKSLASH = Pattern.compile(
            "%5c",
            Pattern.CASE_INSENSITIVE);

    private TagerTrustedLinkPolicy() { }

    static boolean isTrustedUrl(String value) {
        if (value == null) return false;
        String candidate = value.trim();
        if (candidate.isEmpty() || candidate.length() > MAX_URL_LENGTH) return false;
        if (containsControlCharacter(candidate) || containsEncodedControl(candidate)) return false;
        if (candidate.indexOf('\\') >= 0 || ENCODED_BACKSLASH.matcher(candidate).find()) return false;

        try {
            URI uri = new URI(candidate);
            if (uri.isOpaque()) return false;
            if (uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
            if (uri.getHost() == null || !PRODUCTION_HOST.equalsIgnoreCase(uri.getHost())) return false;
            if (uri.getRawUserInfo() != null) return false;

            String rawAuthority = uri.getRawAuthority();
            if (rawAuthority == null || rawAuthority.indexOf('%') >= 0) return false;
            boolean exactAuthority = PRODUCTION_HOST.equalsIgnoreCase(rawAuthority)
                    || (PRODUCTION_HOST + ":443").equalsIgnoreCase(rawAuthority);
            if (!exactAuthority) return false;

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

    static String withAndroidContext(String value, String appVersion) {
        if (!isTrustedUrl(value)) return null;
        String target = value.trim();
        String safeVersion = sanitizeVersion(appVersion);

        int fragmentIndex = target.indexOf('#');
        String fragment = fragmentIndex >= 0 ? target.substring(fragmentIndex) : "";
        String beforeFragment = fragmentIndex >= 0 ? target.substring(0, fragmentIndex) : target;

        int queryIndex = beforeFragment.indexOf('?');
        String base = queryIndex >= 0 ? beforeFragment.substring(0, queryIndex) : beforeFragment;
        String rawQuery = queryIndex >= 0 ? beforeFragment.substring(queryIndex + 1) : "";
        StringBuilder query = new StringBuilder();

        if (!rawQuery.isEmpty()) {
            for (String part : rawQuery.split("&", -1)) {
                if (part.isEmpty()) continue;
                int equals = part.indexOf('=');
                String rawKey = equals >= 0 ? part.substring(0, equals) : part;
                String key = decodeQueryKey(rawKey);
                if ("tager_app".equals(key) || "app_version".equals(key)) continue;
                if (query.length() > 0) query.append('&');
                query.append(part);
            }
        }

        if (query.length() > 0) query.append('&');
        query.append("tager_app=android&app_version=").append(safeVersion);

        String result = base + "?" + query + fragment;
        return isTrustedUrl(result) ? result : null;
    }

    private static String decodeQueryKey(String value) {
        if (value == null) return "";
        String decoded = value;
        for (int i = 0; i < 2; i++) {
            try {
                String next = URLDecoder.decode(decoded, StandardCharsets.UTF_8.name());
                if (next.equals(decoded)) break;
                decoded = next;
            } catch (Exception ignored) {
                break;
            }
        }
        return decoded.toLowerCase(Locale.ROOT);
    }

    private static String sanitizeVersion(String value) {
        if (value == null) return "unknown";
        String safe = value.replaceAll("[^A-Za-z0-9._-]", "");
        return safe.isEmpty() ? "unknown" : safe;
    }

    private static boolean containsControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x1F || c == 0x7F) return true;
        }
        return false;
    }

    private static boolean containsEncodedControl(String value) {
        return ENCODED_CONTROL.matcher(value).find();
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
