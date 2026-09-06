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
    public void rejectsLookalikesAndInsecureSchemes() {
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app.evil.example/#home"));
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("http://tager-new.vercel.app/#home"));
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://user@tager-new.vercel.app/#home"));
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app:8080/#home"));
        assertTrue(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app:443/#home"));
    }

    @Test
    public void rejectsEncodedAuthorityAndBackslashTricks() {
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app%2eevil.example/#home"));
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app%5cevil.example/#home"));
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app/%5c%5cevil"));
    }

    @Test
    public void extractsTrustedUrlFromArabicShareText() {
        String text = "شاهد المنتج على تاجر https://tager-new.vercel.app/?id=88#productDetails وشكراً";
        assertEquals(
                "https://tager-new.vercel.app/?id=88#productDetails",
                TagerTrustedLinkPolicy.findTrustedUrl(text));
    }

    @Test
    public void blocksEncodedControlsAndOversizedInput() {
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app/%00evil"));
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app/%09evil"));
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app/%0aevil"));
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app/%1fevil"));
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl("https://tager-new.vercel.app/%7fevil"));
        StringBuilder huge = new StringBuilder("https://tager-new.vercel.app/");
        while (huge.length() <= 4200) huge.append('a');
        assertFalse(TagerTrustedLinkPolicy.isTrustedUrl(huge.toString()));
        assertNull(TagerTrustedLinkPolicy.findTrustedUrl("لا يوجد رابط تاجر هنا"));
    }

    @Test
    public void addsCurrentAndroidContextWithoutDroppingProductIdentity() {
        String source = "https://tager-new.vercel.app/product/P-123?ref=share#productDetails";
        assertEquals(
                "https://tager-new.vercel.app/product/P-123?ref=share&tager_app=android&app_version=2.3.0#productDetails",
                TagerTrustedLinkPolicy.withAndroidContext(source, "2.3.0"));
    }

    @Test
    public void replacesStaleReservedAndroidContextWithoutDuplicates() {
        String source = "https://tager-new.vercel.app/?tager_app=ios&app_version=2.1.1&id=88#orderDetails";
        assertEquals(
                "https://tager-new.vercel.app/?id=88&tager_app=android&app_version=2.3.0#orderDetails",
                TagerTrustedLinkPolicy.withAndroidContext(source, "2.3.0"));
    }

    @Test
    public void removesEncodedReservedKeyAliasesToo() {
        String source = "https://tager-new.vercel.app/?tager%5Fapp=ios&id=5#productDetails";
        assertEquals(
                "https://tager-new.vercel.app/?id=5&tager_app=android&app_version=2.3.0#productDetails",
                TagerTrustedLinkPolicy.withAndroidContext(source, "2.3.0"));
    }

    @Test
    public void removesDoubleEncodedReservedKeyAliasesToo() {
        String source = "https://tager-new.vercel.app/?tager%255Fapp=ios&app%255Fversion=1.0&id=7#cart";
        assertEquals(
                "https://tager-new.vercel.app/?id=7&tager_app=android&app_version=2.3.1#cart",
                TagerTrustedLinkPolicy.withAndroidContext(source, "2.3.1"));
    }

    @Test
    public void rejectsUntrustedTargetWhenAddingAndroidContext() {
        assertNull(TagerTrustedLinkPolicy.withAndroidContext(
                "https://tager-new.vercel.app.evil.example/?id=88#orderDetails",
                "2.3.0"));
    }
}
