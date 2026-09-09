package com.tager.marketplace;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Intent;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TagerIntentSanitizerInstrumentationTest {

    @Test
    public void knownSchemesRemainAllowed() {
        Intent maps = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:24.7136,46.6753?q=Riyadh"));
        maps.setPackage("com.google.android.apps.maps");
        assertNotNull(TagerIntentLinkSanitizer.sanitize(maps));

        Intent whatsapp = new Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?phone=966500000000"));
        whatsapp.setPackage("com.whatsapp");
        assertNotNull(TagerIntentLinkSanitizer.sanitize(whatsapp));

        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.example.app"));
        market.setPackage("com.android.vending");
        assertNotNull(TagerIntentLinkSanitizer.sanitize(market));
    }

    @Test
    public void packageNameCannotAuthorizeUnknownScheme() {
        Intent unknown = new Intent(Intent.ACTION_VIEW, Uri.parse("evilapp://open/private"));
        unknown.setPackage("com.example.legitimateLookingPackage");
        assertNull(TagerIntentLinkSanitizer.sanitize(unknown));
    }

    @Test
    public void blockedSchemesStayBlockedEvenWithPackage() {
        Intent file = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///sdcard/secret.txt"));
        file.setPackage("com.example.viewer");
        assertNull(TagerIntentLinkSanitizer.sanitize(file));

        Intent content = new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.example.private/data"));
        content.setPackage("com.example.viewer");
        assertNull(TagerIntentLinkSanitizer.sanitize(content));
    }
}
