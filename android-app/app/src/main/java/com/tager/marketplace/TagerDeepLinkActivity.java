package com.tager.marketplace;

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
