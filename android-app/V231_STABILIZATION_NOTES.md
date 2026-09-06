# Tager Android 2.3.1 stabilization

- One WebView runtime: `TagerActivity` is the only activity that owns marketplace UI.
- Verified App Links use a no-display validation dispatcher.
- Full trusted path/query/fragment is delivered directly to the runtime before its first page load.
- No generic-page-first double load for trusted cold-start URLs.
- Internal Tager web URLs use the same strict policy as App Links: HTTPS + exact production host.
- HTTP and lookalike/subdomain URLs are no longer treated as internal Tager navigation.
- Smart launcher, notifications and share intake route into the existing `TagerActivity` task.
- Same package, SDK levels, upload/download behavior, offline recovery and three-permission budget.
- No web platform, Supabase, SQL or Vercel changes.
