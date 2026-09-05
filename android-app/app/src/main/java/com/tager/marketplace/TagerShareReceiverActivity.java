package com.tager.marketplace;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Android share target for Tager links. This activity has no UI and never sends
 * shared text to a server. It only extracts a production Tager URL locally and
 * forwards it to the existing production activity; untrusted text opens home.
 */
public class TagerShareReceiverActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forwardShare(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        forwardShare(intent);
    }

    private void forwardShare(Intent intent) {
        String sharedText = null;
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        }
        Uri target = TagerLinkRouter.findTrustedProductionUrl(sharedText);
        startActivity(TagerLinkRouter.buildOpenIntent(this, target));
        finish();
    }
}
