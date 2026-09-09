package com.tager.marketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TagerIntentLinkSanitizerInstrumentationTest {
    @Test
    public void reconstructsCleanBrowsableIntent() {
        Intent parsed = new Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?text=hello"));
        parsed.setPackage("com.whatsapp");
        parsed.setComponent(new ComponentName("com.evil", "com.evil.Hidden"));
        parsed.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        parsed.putExtra("unexpected", "secret");

        Intent clean = TagerIntentLinkSanitizer.sanitize(parsed);

        assertEquals(Intent.ACTION_VIEW, clean.getAction());
        assertEquals("whatsapp://send?text=hello", clean.getData().toString());
        assertEquals("com.whatsapp", clean.getPackage());
        assertNull(clean.getComponent());
        assertNull(clean.getSelector());
        assertFalse(clean.hasExtra("unexpected"));
        assertEquals(0, clean.getFlags());
        assertTrue(clean.hasCategory(Intent.CATEGORY_BROWSABLE));
    }

    @Test
    public void selectorIsNeverForwarded() {
        Intent parsed = new Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?text=hello"));
        parsed.setSelector(new Intent(Intent.ACTION_VIEW, Uri.parse("javascript:alert(1)")));

        Intent clean = TagerIntentLinkSanitizer.sanitize(parsed);

        assertEquals("whatsapp://send?text=hello", clean.getData().toString());
        assertNull(clean.getSelector());
        assertNull(clean.getComponent());
        assertNull(clean.getPackage());
    }

    @Test
    public void rejectsDangerousAndUnknownCustomSchemes() {
        assertNull(TagerIntentLinkSanitizer.sanitize(
                new Intent(Intent.ACTION_VIEW, Uri.parse("javascript:alert(1)"))));
        assertNull(TagerIntentLinkSanitizer.sanitize(
                new Intent(Intent.ACTION_VIEW, Uri.parse("file:///sdcard/a"))));
        assertNull(TagerIntentLinkSanitizer.sanitize(
                new Intent(Intent.ACTION_VIEW, Uri.parse("customapp://open"))));

        Intent packageScoped = new Intent(Intent.ACTION_VIEW, Uri.parse("mapsapp://place/123"));
        packageScoped.setPackage("com.example.mapsapp");
        assertNull(TagerIntentLinkSanitizer.sanitize(packageScoped));
    }

    @Test
    public void allowsReviewedExternalSchemesWithSafePackages() {
        Intent geo = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:24.7136,46.6753?q=Riyadh"));
        geo.setPackage("com.google.android.apps.maps");
        Intent cleanGeo = TagerIntentLinkSanitizer.sanitize(geo);
        assertEquals("geo:24.7136,46.6753?q=Riyadh", cleanGeo.getData().toString());
        assertEquals("com.google.android.apps.maps", cleanGeo.getPackage());

        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.tager.marketplace"));
        market.setPackage("com.android.vending");
        Intent cleanMarket = TagerIntentLinkSanitizer.sanitize(market);
        assertEquals("market://details?id=com.tager.marketplace", cleanMarket.getData().toString());
        assertEquals("com.android.vending", cleanMarket.getPackage());
    }

    @Test
    public void rejectsMalformedPackageNames() {
        Intent parsed = new Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send"));
        parsed.setPackage("com.whatsapp;scheme=https");
        assertNull(TagerIntentLinkSanitizer.sanitize(parsed));
    }

    @Test
    public void browserFallbackIsStrictlyValidated() {
        Intent safe = new Intent();
        safe.putExtra("browser_fallback_url", "https://example.com/path");
        assertEquals("https://example.com/path", TagerIntentLinkSanitizer.safeBrowserFallback(safe));

        Intent bad = new Intent();
        bad.putExtra("browser_fallback_url", "javascript:alert(1)");
        assertNull(TagerIntentLinkSanitizer.safeBrowserFallback(bad));
    }
}
