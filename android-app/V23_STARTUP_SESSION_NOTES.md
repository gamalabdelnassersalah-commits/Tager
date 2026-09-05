# Tager Android 2.3.0 — Startup Session

Branch: `android-v2.3-startup-session`

## Added
- Lightweight launcher dispatcher that preserves an existing warm task.
- Cold-start resume to the last successful trusted Tager URL.
- Safe fallback to the last marketplace page when the stored URL is invalid or sensitive.
- Resume denylist for OAuth/auth callbacks and credential-like query/fragment keys.
- Reserved Android context parameters are refreshed to the current app version without dropping product/order/vendor identity.

## Preserved
- Same package: `com.tager.marketplace`.
- Same three Android permissions.
- Existing WebView runtime, cookies/session, uploads, downloads, offline recovery, renderer recovery, native navigation and verified App Links.
- No Supabase, Vercel, database or web-platform changes.
