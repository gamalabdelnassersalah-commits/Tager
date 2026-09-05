package com.tager.marketplace;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TagerStartupPolicyTest {
    @Test
    public void resumesTrustedProductAndPreservesIdentity() {
        String source = "https://tager-new.vercel.app/product/P-44?id=44#productDetails";
        assertTrue(TagerStartupPolicy.isSafeResumeUrl(source));
        assertEquals(
                "https://tager-new.vercel.app/product/P-44?id=44&tager_app=android&app_version=2.3.0#productDetails",
                TagerStartupPolicy.resolveTarget(source, "products", "2.3.0"));
    }

    @Test
    public void rejectsSensitiveTokenResume() {
        String source = "https://tager-new.vercel.app/?access_token=secret#home";
        assertFalse(TagerStartupPolicy.isSafeResumeUrl(source));
        assertEquals("tager://products", TagerStartupPolicy.resolveTarget(source, "products", "2.3.0"));
    }

    @Test
    public void rejectsOauthCallbackAndAuthorizationCode() {
        assertFalse(TagerStartupPolicy.isSafeResumeUrl(
                "https://tager-new.vercel.app/auth/callback?code=abc"));
        assertEquals(
                "tager://home",
                TagerStartupPolicy.resolveTarget(
                        "https://tager-new.vercel.app/oauth/callback?code=abc",
                        "bad page!",
                        "2.3.0"));
    }

    @Test
    public void rejectsSensitiveFragmentTokens() {
        assertFalse(TagerStartupPolicy.isSafeResumeUrl(
                "https://tager-new.vercel.app/#access_token=secret&home"));
    }

    @Test
    public void rejectsUntrustedLastGoodUrl() {
        assertEquals(
                "tager://cart",
                TagerStartupPolicy.resolveTarget(
                        "https://evil.example/?id=5#cart",
                        "cart",
                        "2.3.0"));
    }
}
