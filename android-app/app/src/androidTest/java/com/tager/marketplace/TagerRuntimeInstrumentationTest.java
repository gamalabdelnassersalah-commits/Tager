package com.tager.marketplace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class TagerRuntimeInstrumentationTest {
    private static final long URL_TIMEOUT_MS = 6000L;
    private static final long POLL_MS = 100L;

    @Before
    public void clearPersistentState() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("tager_app_state", Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences("tager_update_state", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void coldTrustedDeepLinkUsesSingleWebViewAndExactTarget() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        String target = "https://tager-new.vercel.app/product/123?src=android#products";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(target));
        intent.setClass(context, TagerActivity.class);

        try (ActivityScenario<TagerActivity> scenario = ActivityScenario.launch(intent)) {
            WebView webView = waitForWebView(scenario);
            String current = waitForUrl(scenario, "https://tager-new.vercel.app/product/123");

            scenario.onActivity(activity ->
                    assertEquals(1, countWebViews(activity.findViewById(android.R.id.content))));
            assertNotNull(webView);
            assertTrue(current.startsWith("https://tager-new.vercel.app/product/123"));
            assertTrue(current.contains("tager_app=android"));
            assertTrue(current.contains("app_version=2.3.1"));
            assertTrue(current.endsWith("#products"));
        }
    }

    @Test
    public void trustedAppLinkRouterTargetsSingleTagerRuntime() {
        Context context = ApplicationProvider.getApplicationContext();
        Uri target = Uri.parse("https://tager-new.vercel.app/product/456#products");
        Intent open = TagerLinkRouter.buildOpenIntent(context, target);

        assertNotNull(open.getComponent());
        assertEquals(context.getPackageName(), open.getComponent().getPackageName());
        assertEquals(TagerActivity.class.getName(), open.getComponent().getClassName());
        assertEquals(target.toString(), open.getStringExtra(TagerLinkRouter.EXTRA_TARGET_URL));
        assertTrue((open.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
        assertTrue((open.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0);
    }

    @Test
    public void notificationTargetsKeepTrustedHttpsAndSanitizeFallbacks() {
        String scheme = BuildConfig.CUSTOM_SCHEME;
        assertEquals(
                "https://tager-new.vercel.app/order/77#track",
                TagerLinkRouter.safeNotificationTarget(
                        "https://tager-new.vercel.app/order/77#track",
                        "home").toString());
        assertEquals(
                scheme + "://open/cart",
                TagerLinkRouter.safeNotificationTarget(
                        "https://evil.example/phish",
                        "cart").toString());
        assertEquals(
                scheme + "://open/home",
                TagerLinkRouter.safeNotificationTarget(null, "../bad").toString());
    }

    @Test
    public void legacyAndCanonicalCustomLinksResolveToOneSafeForm() {
        String scheme = BuildConfig.CUSTOM_SCHEME;
        assertEquals(
                scheme + "://open/products",
                TagerLinkRouter.canonicalizeTagerUri(Uri.parse(scheme + "://products")).toString());
        assertEquals(
                scheme + "://open/products",
                TagerLinkRouter.canonicalizeTagerUri(Uri.parse(scheme + "://open/products")).toString());
        assertEquals(
                scheme + "://open/home",
                TagerLinkRouter.pageUri("../not-valid").toString());
    }

    @Test
    public void malformedCustomLinksAreRejected() {
        String scheme = BuildConfig.CUSTOM_SCHEME;
        assertNull(TagerLinkRouter.canonicalizeTagerUri(Uri.parse("https://tager-new.vercel.app/#home")));
        assertNull(TagerLinkRouter.canonicalizeTagerUri(Uri.parse(scheme + "://user@products")));
        assertNull(TagerLinkRouter.canonicalizeTagerUri(Uri.parse(scheme + "://products:99")));
        assertNull(TagerLinkRouter.canonicalizeTagerUri(Uri.parse(scheme + "://products\\evil")));
        assertNull(TagerLinkRouter.canonicalizeTagerUri(Uri.parse("wrong-scheme://products")));
    }

    @Test
    public void webViewRuntimeSecurityIsHardened() throws Exception {
        try (ActivityScenario<TagerActivity> scenario = ActivityScenario.launch(TagerActivity.class)) {
            waitForWebView(scenario);
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
                assertFalse(settings.getJavaScriptCanOpenWindowsAutomatically());
                assertFalse(settings.supportMultipleWindows());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    assertTrue(settings.getSafeBrowsingEnabled());
                }
            });
        }
    }

    @Test
    public void untrustedHttpLinkIsNotAcceptedAsInternalTarget() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://tager-new.vercel.app/#products"));
        intent.setClass(context, TagerActivity.class);

        try (ActivityScenario<TagerActivity> scenario = ActivityScenario.launch(intent)) {
            String current = waitForUrl(scenario, "https://tager-new.vercel.app/");
            assertTrue(current.startsWith("https://tager-new.vercel.app/"));
            assertFalse(current.startsWith("http://"));
        }
    }

    private static WebView waitForWebView(ActivityScenario<TagerActivity> scenario) throws Exception {
        AtomicReference<WebView> ref = new AtomicReference<>();
        long deadline = System.currentTimeMillis() + URL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity(activity -> ref.set(activity.findViewById(R.id.webView)));
            if (ref.get() != null) return ref.get();
            Thread.sleep(POLL_MS);
        }
        assertNotNull("WebView was not created in time", ref.get());
        return ref.get();
    }

    private static String waitForUrl(ActivityScenario<TagerActivity> scenario, String prefix) throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        long deadline = System.currentTimeMillis() + URL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                WebView webView = activity.findViewById(R.id.webView);
                ref.set(webView == null ? null : webView.getUrl());
            });
            String current = ref.get();
            if (current != null && current.startsWith(prefix)) return current;
            Thread.sleep(POLL_MS);
        }
        assertNotNull("WebView URL was null after waiting", ref.get());
        assertTrue("Unexpected WebView URL: " + ref.get(), ref.get().startsWith(prefix));
        return ref.get();
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
