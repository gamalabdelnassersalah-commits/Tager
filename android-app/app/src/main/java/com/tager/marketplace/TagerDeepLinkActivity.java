package com.tager.marketplace;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Single external link dispatcher for Tager 2.3.1.
 * This activity owns no WebView. It validates HTTPS App Links and variant-safe
 * custom links, then forwards them to one explicit native/runtime destination.
 */
public final class TagerDeepLinkActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forward(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        forward(intent);
    }

    private void forward(Intent source) {
        Uri data = source == null ? null : source.getData();
        Intent open = TagerLinkRouter.buildOpenIntent(this, data);
        startActivity(open);
        finish();
    }
}
