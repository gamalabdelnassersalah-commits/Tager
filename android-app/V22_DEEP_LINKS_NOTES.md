# Tager Android 2.2.0 — Full Deep Links

Development branch: `android-v2.2-deep-links`

## User-visible goal
Open a Tager product, supplier, order, RFQ or other verified Tager link at the exact target instead of collapsing the URL to a generic hash-only page.

## Added
- Dedicated `TagerDeepLinkActivity` for verified HTTPS App Links.
- Full preservation of path, query string and fragment.
- Android app context parameters are added only when absent.
- Share-to-Tager verified URLs now use the full deep-link path.
- URL-targeted notifications now use the full deep-link path.
- App Link ownership is isolated to one activity to avoid ambiguous Android routing.

## Preserved
- Existing stable `TagerActivity` runtime remains unchanged.
- Same cookies/session stack.
- Existing file and camera upload behavior.
- DownloadManager and native download center.
- Offline and renderer recovery.
- Android 16 / target SDK 36 compatibility.
- Three-permission budget only.

## Safety boundary
Only `https://tager-new.vercel.app` is accepted by the trusted-link policy. Lookalike hosts, HTTP, authority confusion, control-character injection and non-standard ports remain blocked.
