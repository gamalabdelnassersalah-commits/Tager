from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


def write(path, content):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)

# Version identity.
build_path = "android-app/app/build.gradle"
build = read(build_path)
build = replace_once(build, "versionCode 22", "versionCode 23", "versionCode")
build = replace_once(build, "versionName '2.3.0'", "versionName '2.3.1'", "versionName")
write(build_path, build)

# Make TagerActivity the only WebView runtime and let it accept a fully trusted URL directly.
activity_path = "android-app/app/src/main/java/com/tager/marketplace/TagerActivity.java"
src = read(activity_path)

old_on_new_intent = '''    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasExtra(EXTRA_SAFE_MODE)) {
            safeMode = intent.getBooleanExtra(EXTRA_SAFE_MODE, false);
        }
        navigateTo(resolveRequestedPage(intent));
    }

    private String getPageUrl(String page) {
'''
new_on_new_intent = '''    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasExtra(EXTRA_SAFE_MODE)) {
            safeMode = intent.getBooleanExtra(EXTRA_SAFE_MODE, false);
        }
        if (loadTrustedIntentTarget(intent)) return;
        navigateTo(resolveRequestedPage(intent));
    }

    private boolean loadTrustedIntentTarget(Intent intent) {
        if (webView == null) return false;
        String target = resolveTrustedIntentTarget(intent);
        if (target == null) return false;
        String contextual = TagerTrustedLinkPolicy.withAndroidContext(target, BuildConfig.VERSION_NAME);
        if (contextual == null) return false;

        String current = webView.getUrl();
        if (firstPageLoaded && contextual.equals(current)) {
            syncNavigationFromUrl(current);
            return true;
        }

        String page = pageFromUrl(contextual);
        updateSelectedNavigation(page);
        offlinePanel.setVisibility(View.GONE);
        boolean online = isOnline();
        webView.getSettings().setCacheMode(
                online ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);
        if (!online) {
            showStatus("لا يوجد اتصال مؤكد — سنستخدم المحتوى المحفوظ إن توفر");
        }
        showLoading(true);
        webView.loadUrl(contextual);
        return true;
    }

    private String resolveTrustedIntentTarget(Intent intent) {
        if (intent == null) return null;
        String extra = intent.getStringExtra(TagerLinkRouter.EXTRA_TARGET_URL);
        if (TagerTrustedLinkPolicy.isTrustedUrl(extra)) return extra.trim();
        Uri data = intent.getData();
        if (data != null && TagerTrustedLinkPolicy.isTrustedUrl(data.toString())) {
            return data.toString();
        }
        return null;
    }

    private String getPageUrl(String page) {
'''
src = replace_once(src, old_on_new_intent, new_on_new_intent, "onNewIntent + trusted loader")

old_initial_load = '''        String page = resolveRequestedPage(getIntent());
        updateSelectedNavigation(page);
        showLoading(true);
        if (!lastKnownOnline) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            showStatus("لا يوجد اتصال مؤكد — سنستخدم المحتوى المحفوظ إن توفر");
        }
        webView.loadUrl(getPageUrl(page));
'''
new_initial_load = '''        if (loadTrustedIntentTarget(getIntent())) return;

        String page = resolveRequestedPage(getIntent());
        updateSelectedNavigation(page);
        showLoading(true);
        if (!lastKnownOnline) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            showStatus("لا يوجد اتصال مؤكد — سنستخدم المحتوى المحفوظ إن توفر");
        }
        webView.loadUrl(getPageUrl(page));
'''
src = replace_once(src, old_initial_load, new_initial_load, "cold-start trusted URL loading")

old_resolve_http = '''                if (("http".equals(scheme) || "https".equals(scheme)) && isTagerHost(data.getHost())) {
                    return sanitizePage(data.getFragment());
                }
'''
new_resolve_http = '''                if (TagerTrustedLinkPolicy.isTrustedUrl(data.toString())) {
                    return sanitizePage(data.getFragment());
                }
'''
src = replace_once(src, old_resolve_http, new_resolve_http, "resolveRequestedPage policy")

old_policy = '''    private boolean isTagerHost(String host) {
        if (host == null) return false;
        String value = host.toLowerCase(Locale.ROOT);
        return "tager-new.vercel.app".equals(value) || value.endsWith(".tager-new.vercel.app");
    }

    private boolean isTagerUrl(String url) {
        try {
            if (url == null) return false;
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            return ("http".equals(scheme) || "https".equals(scheme)) && isTagerHost(uri.getHost());
        } catch (Exception ignored) {
            return false;
        }
    }
'''
new_policy = '''    private boolean isTagerHost(String host) {
        return host != null && TagerTrustedLinkPolicy.PRODUCTION_HOST.equalsIgnoreCase(host);
    }

    private boolean isTagerUrl(String url) {
        return TagerTrustedLinkPolicy.isTrustedUrl(url);
    }
'''
src = replace_once(src, old_policy, new_policy, "legacy URL policy")

old_navigation = '''        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (("http".equals(scheme) || "https".equals(scheme)) && isTagerHost(host)) return false;
'''
new_navigation = '''        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (TagerTrustedLinkPolicy.isTrustedUrl(uri.toString())) return false;
'''
src = replace_once(src, old_navigation, new_navigation, "WebView navigation policy")
write(activity_path, src)

# Deep-link Activity becomes a no-UI validator/dispatcher; it never owns a WebView.
deep_path = "android-app/app/src/main/java/com/tager/marketplace/TagerDeepLinkActivity.java"
write(deep_path, '''package com.tager.marketplace;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Verified App Link dispatcher for Tager 2.3.1.
 * This activity owns no WebView. It validates external HTTPS links and forwards
 * the full target to the single TagerActivity runtime.
 */
public final class TagerDeepLinkActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forward(getIntent());
    }

    private void forward(Intent source) {
        Uri data = source == null ? null : source.getData();
        Intent open;
        if (TagerLinkRouter.isTrustedProductionUrl(data)) {
            open = new Intent(this, TagerActivity.class);
            open.putExtra(TagerLinkRouter.EXTRA_TARGET_URL, data.toString());
        } else if (data != null && "tager".equalsIgnoreCase(data.getScheme())) {
            open = new Intent(Intent.ACTION_VIEW, data, this, TagerActivity.class);
        } else {
            open = new Intent(this, TagerActivity.class);
        }
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(open);
        finish();
    }
}
''')

# Internal router targets the one WebView runtime directly and carries the full URL in an explicit extra.
router_path = "android-app/app/src/main/java/com/tager/marketplace/TagerLinkRouter.java"
router = read(router_path)
router = replace_once(
    router,
    '''final class TagerLinkRouter {
    static final String PRODUCTION_HOST = TagerTrustedLinkPolicy.PRODUCTION_HOST;
''',
    '''final class TagerLinkRouter {
    static final String PRODUCTION_HOST = TagerTrustedLinkPolicy.PRODUCTION_HOST;
    static final String EXTRA_TARGET_URL = "tager_target_url";
''',
    "router target extra")
old_router = '''    static Intent buildOpenIntent(Context context, Uri target) {
        Intent intent;
        if (isTrustedProductionUrl(target)) {
            intent = new Intent(Intent.ACTION_VIEW, target, context, TagerDeepLinkActivity.class);
        } else if (target != null && "tager".equalsIgnoreCase(target.getScheme())) {
            intent = new Intent(Intent.ACTION_VIEW, target, context, TagerActivity.class);
        } else {
            intent = new Intent(context, TagerActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }
'''
new_router = '''    static Intent buildOpenIntent(Context context, Uri target) {
        Intent intent;
        if (isTrustedProductionUrl(target)) {
            intent = new Intent(context, TagerActivity.class);
            intent.putExtra(EXTRA_TARGET_URL, target.toString());
        } else if (target != null && "tager".equalsIgnoreCase(target.getScheme())) {
            intent = new Intent(Intent.ACTION_VIEW, target, context, TagerActivity.class);
        } else {
            intent = new Intent(context, TagerActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }
'''
router = replace_once(router, old_router, new_router, "single-runtime router")
write(router_path, router)

# Smart launcher also routes directly into the single runtime.
launch_path = "android-app/app/src/main/java/com/tager/marketplace/TagerLaunchActivity.java"
launch = read(launch_path)
old_launch = '''        Uri targetUri = Uri.parse(target);
        Intent open;
        if (TagerTrustedLinkPolicy.isTrustedUrl(target)) {
            open = new Intent(Intent.ACTION_VIEW, targetUri, this, TagerDeepLinkActivity.class);
        } else {
            open = new Intent(Intent.ACTION_VIEW, targetUri, this, TagerActivity.class);
        }
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(open);
'''
new_launch = '''        Uri targetUri = Uri.parse(target);
        Intent open = TagerLinkRouter.buildOpenIntent(this, targetUri);
        startActivity(open);
'''
launch = replace_once(launch, old_launch, new_launch, "single-runtime launcher")
write(launch_path, launch)

# App Links are validated by a no-display dispatcher; only TagerActivity owns the runtime UI.
manifest_path = "android-app/app/src/main/AndroidManifest.xml"
manifest = read(manifest_path)
old_deep_manifest = '''        <activity
            android:name=".TagerDeepLinkActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:resizeableActivity="true"
            android:windowSoftInputMode="adjustResize"
            android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden"
            android:theme="@style/Theme.Tager.Starting">
'''
new_deep_manifest = '''        <activity
            android:name=".TagerDeepLinkActivity"
            android:exported="true"
            android:noHistory="true"
            android:excludeFromRecents="true"
            android:theme="@android:style/Theme.NoDisplay">
'''
manifest = replace_once(manifest, old_deep_manifest, new_deep_manifest, "deep-link dispatcher manifest")
write(manifest_path, manifest)

# Keep network configuration exact-domain oriented as well.
network_path = "android-app/app/src/main/res/xml/network_security_config.xml"
network = read(network_path)
network = replace_once(
    network,
    '<domain includeSubdomains="true">tager-new.vercel.app</domain>',
    '<domain includeSubdomains="false">tager-new.vercel.app</domain>',
    "network exact domain")
write(network_path, network)

# Notes for this stabilization release.
write("android-app/V231_STABILIZATION_NOTES.md", '''# Tager Android 2.3.1 stabilization

- One WebView runtime: `TagerActivity` is the only activity that owns marketplace UI.
- Verified App Links use a no-display validation dispatcher.
- Full trusted path/query/fragment is delivered directly to the runtime before its first page load.
- No generic-page-first double load for trusted cold-start URLs.
- Internal Tager web URLs use the same strict policy as App Links: HTTPS + exact production host.
- HTTP and lookalike/subdomain URLs are no longer treated as internal Tager navigation.
- Smart launcher, notifications and share intake route into the existing `TagerActivity` task.
- Same package, SDK levels, upload/download behavior, offline recovery and three-permission budget.
- No web platform, Supabase, SQL or Vercel changes.
''')

# Final CI for the stabilization branch.
write(".github/workflows/tager-android-v231-stabilization.yml", '''name: Build Tager Android 2.3.1 Stabilization

on:
  push:
    branches: [ android-v2.3.1-stabilization ]
    paths:
      - 'android-app/**'
      - '.github/workflows/tager-android-v231-stabilization.yml'
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      - name: Set up Java 17
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '17'
      - name: Set up Android SDK
        uses: android-actions/setup-android@v3
      - name: Install Android SDK 36
        run: sdkmanager "platforms;android-36" "build-tools;35.0.0"
      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.11.1'
      - name: Lint, unit tests and release build
        working-directory: android-app
        run: gradle --no-daemon :app:lintRelease :app:testReleaseUnitTest :app:assembleRelease :app:bundleRelease
      - name: Verify unit tests
        run: |
          REPORT_DIR=android-app/app/build/test-results/testReleaseUnitTest
          test -d "$REPORT_DIR"
          grep -R -q 'TagerTrustedLinkPolicyTest' "$REPORT_DIR"
          grep -R -q 'TagerStartupPolicyTest' "$REPORT_DIR"
          if grep -R -Eq 'failures="[1-9]|errors="[1-9]' "$REPORT_DIR"; then exit 1; fi
      - name: Verify 2.3.1 identity
        run: |
          BUILD=android-app/app/build.gradle
          grep -q "versionCode 23" "$BUILD"
          grep -q "versionName '2.3.1'" "$BUILD"
          grep -q "applicationId 'com.tager.marketplace'" "$BUILD"
          grep -q "targetSdk 36" "$BUILD"
      - name: Verify single WebView runtime and no double-load architecture
        run: |
          SRC=android-app/app/src/main/java/com/tager/marketplace/TagerActivity.java
          DEEP=android-app/app/src/main/java/com/tager/marketplace/TagerDeepLinkActivity.java
          ROUTER=android-app/app/src/main/java/com/tager/marketplace/TagerLinkRouter.java
          MANIFEST=android-app/app/src/main/AndroidManifest.xml
          grep -q 'loadTrustedIntentTarget(getIntent())' "$SRC"
          grep -q 'EXTRA_TARGET_URL' "$SRC"
          grep -q 'withAndroidContext' "$SRC"
          grep -q 'extends Activity' "$DEEP"
          if grep -q 'extends TagerActivity' "$DEEP"; then exit 1; fi
          if grep -Eq 'WebView|loadUrl' "$DEEP"; then exit 1; fi
          grep -q 'intent.putExtra(EXTRA_TARGET_URL' "$ROUTER"
          grep -q 'android:noHistory="true"' "$MANIFEST"
          grep -q '@android:style/Theme.NoDisplay' "$MANIFEST"
      - name: Verify one strict trusted-link policy
        run: |
          SRC=android-app/app/src/main/java/com/tager/marketplace/TagerActivity.java
          POLICY=android-app/app/src/main/java/com/tager/marketplace/TagerTrustedLinkPolicy.java
          NETWORK=android-app/app/src/main/res/xml/network_security_config.xml
          grep -q 'TagerTrustedLinkPolicy.isTrustedUrl(uri.toString())' "$SRC"
          grep -q 'return TagerTrustedLinkPolicy.isTrustedUrl(url);' "$SRC"
          if grep -q 'endsWith(".tager-new.vercel.app")' "$SRC"; then exit 1; fi
          grep -q '"https".equalsIgnoreCase(uri.getScheme())' "$POLICY"
          grep -q 'PRODUCTION_HOST.equalsIgnoreCase(uri.getHost())' "$POLICY"
          grep -q 'includeSubdomains="false"' "$NETWORK"
      - name: Verify permission and production hardening
        run: |
          MANIFEST=android-app/app/src/main/AndroidManifest.xml
          SRC=android-app/app/src/main/java/com/tager/marketplace/TagerActivity.java
          test "$(grep -c '<uses-permission' "$MANIFEST")" -eq 3
          grep -q 'android.permission.INTERNET' "$MANIFEST"
          grep -q 'android.permission.ACCESS_NETWORK_STATE' "$MANIFEST"
          grep -q 'android.permission.POST_NOTIFICATIONS' "$MANIFEST"
          grep -q 'setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW)' "$SRC"
          grep -q 'WebView.setWebContentsDebuggingEnabled(false)' "$SRC"
          grep -q 'restoreLastGoodPage' "$SRC"
          grep -q 'camera.setClipData' "$SRC"
          grep -q 'android:allowBackup="false"' "$MANIFEST"
          grep -q 'android:usesCleartextTraffic="false"' "$MANIFEST"
          if grep -Eq 'READ_CONTACTS|READ_SMS|SEND_SMS|RECORD_AUDIO|ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION|READ_PHONE_STATE|AD_ID' "$MANIFEST"; then exit 1; fi
      - name: Verify unsigned APK alignment
        run: |
          SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
          "$SDK_ROOT/build-tools/35.0.0/zipalign" -c -P 16 -v 4 android-app/app/build/outputs/apk/release/app-release-unsigned.apk
      - name: Prepare outputs
        run: |
          cp android-app/app/build/outputs/apk/release/app-release-unsigned.apk Tager-v2.3.1-stabilization-unsigned.apk
          cp android-app/app/build/outputs/bundle/release/app-release.aab Tager-v2.3.1-stabilization-unsigned.aab
          sha256sum Tager-v2.3.1-stabilization-unsigned.apk > Tager-v2.3.1-stabilization-unsigned.apk.sha256
          sha256sum Tager-v2.3.1-stabilization-unsigned.aab > Tager-v2.3.1-stabilization-unsigned.aab.sha256
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: Tager-v2.3.1-stabilization-unsigned-APK
          path: |
            Tager-v2.3.1-stabilization-unsigned.apk
            Tager-v2.3.1-stabilization-unsigned.apk.sha256
          retention-days: 30
      - name: Upload AAB
        uses: actions/upload-artifact@v4
        with:
          name: Tager-v2.3.1-stabilization-unsigned-AAB
          path: |
            Tager-v2.3.1-stabilization-unsigned.aab
            Tager-v2.3.1-stabilization-unsigned.aab.sha256
          retention-days: 30
''')

# The bootstrap patch is intentionally self-removing after it has produced the real source commit.
for transient in ["scripts/stabilize_v231.py", ".github/workflows/tager-v231-bootstrap.yml"]:
    target = ROOT / transient
    if target.exists():
        target.unlink()

print("Tager Android 2.3.1 stabilization patch applied")
