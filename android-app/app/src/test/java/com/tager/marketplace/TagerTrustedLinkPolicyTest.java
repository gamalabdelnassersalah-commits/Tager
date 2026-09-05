package com.tager.marketplace;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TagerTrustedLinkPolicyTest {
    @Test
    public void acceptsProductionHttpsAndPreservesQueryAndFragment() {
        String url = "https://tager-new.vercel.app/?productId=P-123#productDetails";
        assertTrue(TagerTrustedLinkPolicy.isTrustedUrl(url));
        assertEquals(url, TagerTrustedLinkPolicy.findTrustedUrl(url));
    }

    @Test
    public void acceptsUppercaseHost() {
        assertTrue(TagerTrustedLinkPolicy.isTrustedUrl("https://TAGER-NEW.VERCEL.APP/#home"));
    }

    @Test
    public void rejectsLookalikeHostSuffix() {
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app.evil.example/#home"));
    }

    @Test
    public void rejectsHttp() {
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("http://tager-new.vercel.app/#home"));
    }

    @Test
    public void rejectsUserInfoAuthorityConfusion() {
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://user@tager-new.vercel.app/#home"));
    }

    @Test
    public void rejectsNonStandardPort() {
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app:8080/#home"));
        assertTrue(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app:443/#home"));
    }

    @Test
    public void extractsTrustedUrlFromArabicShareText() {
        String text = "شاهد المنتج على تاجر https://tager-new.vercel.app/?id=88#productDetails وشكراً";
        assertEquals(
                "https://tager-new.vercel.app/?id=88#productDetails",
                TagerTrustedLinkPolicy.findTrustedUrl(text));
    }

    @Test
    public void trimsArabicTrailingPunctuation() {
        String text = "افتح https://tager-new.vercel.app/#cart،";
        assertEquals("https://tager-new.vercel.app/#cart", TagerTrustedLinkPolicy.findTrustedUrl(text));
    }

    @Test
    public void blocksEncodedControlsAndOversizedInput() {
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app/%0aevil"));
        StringBuilder huge = new StringBuilder("https://tager-new.vercel.app/");
        while (huge.length() <= 4200) huge.append('a');
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl(huge.toString()));
        assertNull(TagerTrustedLinkPolicy.findTrustedUrl("لا يوجد رابط تاجر هنا"));
    }

    @Test
    public void addsAndroidContextWithoutDroppingProductIdentity() {
        String source = "https://tager-new.vercel.app/product/P-123?ref=share#productDetails";
        assertEquals(
                "https://tager-new.vercel.app/product/P-123?ref=share&tager_app=android&app_version=2.2.0#productDetails",
                TagerTrustedLinkPolicy.withAndroidContext(source, "2.2.0"));
    }

    @Test
    public void doesNotDuplicateExistingAndroidContext() {
        String source = "https://tager-new.vercel.app/?tager_app=android&app_version=2.1.1&id=88#orderDetails";
        assertEquals(source, TagerTrustedLinkPolicy.withAndroidContext(source, "2.2.0"));
    }

    @Test
    public void rejectsUntrustedTargetWhenAddingAndroidContext() {
        assertNull(TagerTrustedLinkPolicy.withAndroidContext(
                "https://tager-new.vercel.app.evil.example/?id=88#orderDetails",
                "2.2.0"));
    }
}
