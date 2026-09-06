package com.tager.marketplace;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

/**
 * Fast launcher dispatcher for Tager 2.3.
 *
 * A warm task is left untouched: tapping the launcher simply returns the user
 * to the existing activity stack. On a cold start, the dispatcher restores the
 * last trusted successful Tager URL when it is safe to resume, otherwise it
 * falls back to the last known marketplace page.
 */
public final class TagerLaunchActivity extends Activity {
    private static final String PREFS_NAME = "tager_app_state";
    private static final String PREF_LAST_PAGE = "last_page";
    private static final String PREF_LAST_GOOD_URL = "last_good_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent launchIntent = getIntent();
        if (!isTaskRoot()
                && launchIntent != null
                && Intent.ACTION_MAIN.equals(launchIntent.getAction())
                && launchIntent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            finish();
            return;
        }

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String target = TagerStartupPolicy.resolveTarget(
                preferences.getString(PREF_LAST_GOOD_URL, ""),
                preferences.getString(PREF_LAST_PAGE, "home"),
                BuildConfig.VERSION_NAME);

        Uri targetUri = Uri.parse(target);
        Intent open = TagerLinkRouter.buildOpenIntent(this, targetUri);
        startActivity(open);
        finish();
    }
}
