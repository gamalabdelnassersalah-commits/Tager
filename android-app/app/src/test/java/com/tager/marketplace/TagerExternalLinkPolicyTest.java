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
        assertFalse(TagerExternalLinkPolicy.isAllowedExternalScheme("file"));
        assertFalse(TagerExternalLinkPolicy.isAllowedExternalScheme("javascript"));
        assertFalse(TagerExternalLinkPolicy.isAllowedExternalScheme("content"));
    }

    @Test
    public void browserFallbackMustBeHttpOrHttps() {
        assertTrue(TagerExternalLinkPolicy.isSafeBrowserFallback("https://example.com/path?q=1"));
        assertTrue(TagerExternalLinkPolicy.isSafeBrowserFallback("http://example.com/"));
        assertFalse(TagerExternalLinkPolicy.isSafeBrowserFallback("javascript:alert(1)"));
        assertFalse(TagerExternalLinkPolicy.isSafeBrowserFallback("file:///sdcard/a"));
        assertFalse(TagerExternalLinkPolicy.isSafeBrowserFallback("https://example.com\\@evil.example"));
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
