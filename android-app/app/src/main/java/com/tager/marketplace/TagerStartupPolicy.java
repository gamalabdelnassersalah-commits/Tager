package com.tager.marketplace;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Pure-Java cold-start resume policy, independently testable in CI. */
final class TagerStartupPolicy {
    private static final Set<String> SENSITIVE_QUERY_KEYS = new HashSet<>(Arrays.asList(
            "access_token",
            "refresh_token",
            "id_token",
            "token",
            "code",
            "otp",
            "password",
            "recovery_token",
            "confirmation_token"
    ));

    private TagerStartupPolicy() { }

    static String resolveTarget(String lastGoodUrl, String lastPage, String appVersion) {
        if (isSafeResumeUrl(lastGoodUrl)) {
            String contextual = TagerTrustedLinkPolicy.withAndroidContext(lastGoodUrl, appVersion);
            if (contextual != null) return contextual;
        }
        return "tager://" + sanitizePage(lastPage);
    }

    static boolean isSafeResumeUrl(String value) {
        if (!TagerTrustedLinkPolicy.isTrustedUrl(value)) return false;
        try {
            URI uri = new URI(value.trim());
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            if (path.contains("/auth/")
                    || path.contains("/oauth/")
                    || path.endsWith("/callback")
                    || path.contains("/auth-callback")) {
                return false;
            }

            String query = uri.getRawQuery();
            if (query != null && !query.isEmpty()) {
                for (String part : query.split("&", -1)) {
                    int equals = part.indexOf('=');
                    String rawKey = equals >= 0 ? part.substring(0, equals) : part;
                    String key = decode(rawKey).toLowerCase(Locale.ROOT);
                    if (SENSITIVE_QUERY_KEYS.contains(key)) return false;
                }
            }

            String fragment = uri.getRawFragment();
            if (fragment != null) {
                String lower = decode(fragment).toLowerCase(Locale.ROOT);
                for (String key : SENSITIVE_QUERY_KEYS) {
                    if (lower.contains(key + "=")) return false;
                }
            }
            return true;
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static String sanitizePage(String page) {
        if (page == null || !page.matches("[A-Za-z0-9_-]{1,64}")) return "home";
        return page;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            return value == null ? "" : value;
        }
    }
}
