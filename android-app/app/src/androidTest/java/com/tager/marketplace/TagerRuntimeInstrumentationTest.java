package com.tager.marketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TagerRuntimeInstrumentationTest {

    @Test
    public void coldTrustedDeepLinkUsesSingleWebViewAndExactTarget() {
        String target = "https://tager-new.vercel.app/product/123?src=android#products";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(target));
        intent.setClassName("com.tager.marketplace", "com.tager.marketplace.TagerActivity");

        try (ActivityScenario<TagerActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                WebView webView = activity.findViewById(R.id.webView);
                assertNotNull(webView);
                assertEquals(1, countWebViews(activity.findViewById(android.R.id.content)));

                String current = webView.getUrl();
                assertNotNull(current);
                assertTrue(current.startsWith("https://tager-new.vercel.app/product/123"));
                assertTrue(current.contains("tager_app=android"));
                assertTrue(current.contains("app_version=2.3.1"));
                assertTrue(current.endsWith("#products"));
            });
        }
    }

    @Test
    public void webViewRuntimeSecurityIsHardened() {
        try (ActivityScenario<TagerActivity> scenario = ActivityScenario.launch(TagerActivity.class)) {
            scenario.onActivity(activity -> {
                WebView webView = activity.findViewById(R.id.webView);
                assertNotNull(webView);
                WebSettings settings = webView.getSettings();

                assertFalse(settings.getAllowFileAccess());
                assertFalse(settings.getAllowFileAccessFromFileURLs());
                assertFalse(settings.getAllowUniversalAccessFromFileURLs());
                assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, settings.getMixedContentMode());
                assertTrue(settings.getJavaScriptEnabled());
                assertTrue(settings.getDomStorageEnabled());
            });
        }
    }

    @Test
    public void untrustedHttpLinkIsNotAcceptedAsInternalTarget() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://tager-new.vercel.app/#products"));
        intent.setClassName("com.tager.marketplace", "com.tager.marketplace.TagerActivity");

        try (ActivityScenario<TagerActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                WebView webView = activity.findViewById(R.id.webView);
                assertNotNull(webView);
                String current = webView.getUrl();
                assertNotNull(current);
                assertTrue(current.startsWith("https://tager-new.vercel.app/"));
                assertFalse(current.startsWith("http://"));
            });
        }
    }

    private static int countWebViews(View view) {
        if (view == null) return 0;
        int count = view instanceof WebView ? 1 : 0;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                count += countWebViews(group.getChildAt(i));
            }
        }
        return count;
    }
}
