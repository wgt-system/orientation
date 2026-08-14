# Security

Do not report sensitive location data, credentials, API keys, provider tokens, or private provider payloads in public issues.

Security-sensitive areas include:

- current-location privacy and retention;
- external-resource/URL handling;
- untrusted provider responses;
- SSRF and provider URL configuration;
- map style/tile resources;
- WebView/browser bridges;
- routing/geocoding provider credentials;
- cross-context data leakage.

The initial bootstrap intentionally stores no current-location history and defines no shared persistent geospatial database.
