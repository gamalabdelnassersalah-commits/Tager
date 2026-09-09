package com.tager.marketplace;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TagerExternalLinkPolicyTest {
    @Test
    public void allowsOnlySupportedExternalSchemes() {
        assertTrue(TagerExternalLinkPolicy.isAllowedExternalScheme("https"));
        assertTrue(TagerExternalLinkPolicy.isAllowedExternalScheme("http"));
        assertTrue(TagerExternalLinkPolicy.isAllowedExternalScheme("tel"));
        assertTrue(TagerExternalLinkPolicy.isAllowedExternalScheme("mailto"));
        assertTrue(TagerExternalLinkPolicy.isAllowedExternalScheme("sms"));
        assertTrue(TagerExternalLinkPolicy.isAllowedExternalScheme("whatsapp"));
        assertTrue(TagerExternalLinkPolicy.isAllowedExternalScheme("market"));
        assertTrue(TagerExternalLinkPolicy.isAllowedExternalScheme("geo"));
        assertTrue(TagerExternalLinkPolicy.isAllowedExternalScheme("google.navigation"));
        assertFalse(TagerExternalLinkPolicy.isAllowedExternalScheme("file"));
        assertFalse(TagerExternalLinkPolicy.isAllowedExternalScheme("javascript"));
        assertFalse(TagerExternalLinkPolicy.isAllowedExternalScheme("content"));
    }

    @Test
    public void validatesSafeExternalUris() {
        assertTrue(TagerExternalLinkPolicy.isSafeExternalUri("https://example.com/path?q=1"));
        assertTrue(TagerExternalLinkPolicy.isSafeExternalUri("tel:+966500000000"));
        assertTrue(TagerExternalLinkPolicy.isSafeExternalUri("mailto:support@example.com"));
        assertTrue(TagerExternalLinkPolicy.isSafeExternalUri("geo:24.7136,46.6753?q=Riyadh"));
        assertTrue(TagerExternalLinkPolicy.isSafeExternalUri("google.navigation:q=24.7136,46.6753"));
        assertFalse(TagerExternalLinkPolicy.isSafeExternalUri("javascript:alert(1)"));
        assertFalse(TagerExternalLinkPolicy.isSafeExternalUri("file:///sdcard/a"));
        assertFalse(TagerExternalLinkPolicy.isSafeExternalUri("content://com.example/private"));
        assertFalse(TagerExternalLinkPolicy.isSafeExternalUri("https://example.com/%0aevil"));
        assertFalse(TagerExternalLinkPolicy.isSafeExternalUri("https://example.com\\@evil.example"));
    }

    @Test
    public void validatesIntentUriEnvelopeBeforeAndroidParsing() {
        assertTrue(TagerExternalLinkPolicy.isSafeIntentUri(
                "intent://scan/#Intent;scheme=zxing;package=com.google.zxing.client.android;end"));
        assertTrue(TagerExternalLinkPolicy.isSafeIntentUri(
                "intent://maps/#Intent;scheme=geo;package=com.google.android.apps.maps;end"));
        assertFalse(TagerExternalLinkPolicy.isSafeIntentUri("intent://scan/"));
        assertFalse(TagerExternalLinkPolicy.isSafeIntentUri(
                "intent://scan/#Intent;scheme=zxing;package=com.google.zxing.client.android;end\nfile:///sdcard/a"));
        assertFalse(TagerExternalLinkPolicy.isSafeIntentUri(
                "intent://scan\\evil/#Intent;scheme=zxing;end"));
        assertFalse(TagerExternalLinkPolicy.isSafeIntentUri(
                "intent://scan/#Intent;scheme=zxing;S.browser_fallback_url=https://example.com/%0aevil;end"));
    }

    @Test
    public void browserFallbackMustBeHttpOrHttps() {
        assertTrue(TagerExternalLinkPolicy.isSafeBrowserFallback("https://example.com/path?q=1"));
        assertTrue(TagerExternalLinkPolicy.isSafeBrowserFallback("http://example.com/"));
        assertFalse(TagerExternalLinkPolicy.isSafeBrowserFallback("javascript:alert(1)"));
        assertFalse(TagerExternalLinkPolicy.isSafeBrowserFallback("file:///sdcard/a"));
        assertFalse(TagerExternalLinkPolicy.isSafeBrowserFallback("https://example.com\\@evil.example"));
        assertFalse(TagerExternalLinkPolicy.isSafeBrowserFallback("https://user@example.com/path"));
        assertFalse(TagerExternalLinkPolicy.isSafeBrowserFallback("https:///missing-host"));
        assertFalse(TagerExternalLinkPolicy.isSafeBrowserFallback("https://example.com/%0aevil"));
        assertFalse(TagerExternalLinkPolicy.isSafeBrowserFallback("https://example.com/%5cevil"));
    }

    @Test
    public void validatesIntentPackageNames() {
        assertTrue(TagerExternalLinkPolicy.isSafeIntentPackage("com.google.android.apps.maps"));
        assertTrue(TagerExternalLinkPolicy.isSafeIntentPackage("com.whatsapp"));
        assertFalse(TagerExternalLinkPolicy.isSafeIntentPackage("com.whatsapp;scheme=https"));
        assertFalse(TagerExternalLinkPolicy.isSafeIntentPackage("whatsapp"));
        assertFalse(TagerExternalLinkPolicy.isSafeIntentPackage(null));
    }

    @Test
    public void blocksDangerousWebViewSchemes() {
        assertTrue(TagerExternalLinkPolicy.isBlockedWebViewScheme("file"));
        assertTrue(TagerExternalLinkPolicy.isBlockedWebViewScheme("content"));
        assertTrue(TagerExternalLinkPolicy.isBlockedWebViewScheme("javascript"));
        assertTrue(TagerExternalLinkPolicy.isBlockedWebViewScheme("data"));
        assertTrue(TagerExternalLinkPolicy.isBlockedWebViewScheme("about"));
        assertTrue(TagerExternalLinkPolicy.isBlockedWebViewScheme("blob"));
        assertFalse(TagerExternalLinkPolicy.isBlockedWebViewScheme("https"));
    }
}
