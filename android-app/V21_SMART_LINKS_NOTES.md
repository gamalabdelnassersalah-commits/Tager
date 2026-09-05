# Tager Android 2.1.0 — Smart Links

Development branch: `android-v2.1-smart-links`

## Added
- Android Share target for `text/plain`.
- Local-only extraction of trusted Tager production links from shared text.
- Strict production-host allowlist: `https://tager-new.vercel.app` only.
- Notification helpers that can safely target a production Tager URL with page fallback.
- No additional Android permissions.

## Preserved
- Existing production WebView runtime and cookie/session behavior.
- File/image picker and camera upload path.
- DownloadManager integration.
- Offline recovery and last-good-page behavior.
- Renderer crash recovery and safe mode.
- Android 16 / target SDK 36 compatibility.

## Safety boundary
This change does not modify the web platform, Supabase, Vercel configuration, or database. Shared text is parsed locally and is never uploaded by the share receiver.
