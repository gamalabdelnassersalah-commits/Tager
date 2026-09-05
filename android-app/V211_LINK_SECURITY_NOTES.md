# Tager Android 2.1.1 — Link Security

Development branch: `android-v2.1.1-link-security`

## Added
- Pure-Java trusted-link policy with real JUnit coverage.
- Exact production-host enforcement for `tager-new.vercel.app`.
- HTTPS-only policy.
- User-info authority rejection.
- Non-standard port rejection; HTTPS 443 remains accepted.
- Encoded CR/LF/NUL rejection.
- URL length cap and control-character rejection.
- Arabic share-text extraction and punctuation cleanup.
- Query string and fragment are preserved by the routing policy.

## Preserved
- Same package: `com.tager.marketplace`.
- Target/compile SDK 36.
- Same 3 Android permissions only.
- Existing WebView, camera/file upload, download, offline recovery, renderer recovery, settings, notification channels, and Play update infrastructure.

## Remaining integration item
`TagerActivity` still maps incoming website intents primarily to the page fragment. The routing layer now preserves the full trusted URL safely; a later runtime patch should teach the main activity to consume the complete trusted URL when the web application's direct-link contract is finalized.
