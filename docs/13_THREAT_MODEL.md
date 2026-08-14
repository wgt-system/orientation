# Orientation – Threat Model

**Status:** Bootstrap baseline

## Sensitive assets

- precise current location;
- route origins/destinations;
- provider API keys/tokens;
- external-resource URIs;
- provider-derived place/address data;
- foreign domain information embedded in map scenes.

## Main threats

### Location privacy

Precise current location can reveal highly sensitive personal information.

Controls:

- no persistence/history by default;
- host owns permission acquisition;
- Orientation accepts only validated finite coordinates, non-negative accuracy and serialized UTC timestamps;
- do not log coordinates at normal info level;
- explicit retention decision required before storing position history;
- avoid sending current position to providers that do not require it.

### Malicious/untrusted rich content

Provider/domain strings can reach marker/details UI.

Controls:

- render text as text, not arbitrary HTML;
- do not accept executable HTML/script fragments in Spatial Feature contracts;
- validate resource URIs and allow only HTTP(S) schemes in the generic web-resource model;
- construct details controls with DOM text nodes and buttons, never provider HTML or CSS;
- keep resource/action execution host-mediated.

### External URLs

Controls:

- validate URI scheme;
- prefer HTTPS for external web resources;
- reject script/data/file schemes at generic web-resource boundaries unless a separate host-specific decision exists;
- host decides actual navigation/open behavior.

### Provider/API abuse

Controls:

- timeouts;
- bounded retries;
- rate limiting/backoff where provider requires it;
- no secrets in logs/contracts;
- explicit provider configuration.

### SSRF

If backend adapters accept configured URLs:

- do not allow arbitrary user/provider scene data to become backend fetch targets;
- constrain provider endpoints through trusted configuration;
- validate redirects/network targets where relevant.

### WebView/browser bridge

Controls:

- narrow bridge/event contract;
- validate all inbound messages;
- no arbitrary JavaScript evaluation as a domain command channel;
- Content Security Policy for reference/embedded host where practical.

### Tile/style supply chain

Controls:

- explicit provider configuration and attribution;
- fail visibly when required resources are unavailable;
- do not treat remote styles as trusted application code.

### Cross-context leakage

Controls:

- no foreign DB access;
- no broad serialization of foreign aggregates;
- provider decides what may be published into a spatial projection;
- Orientation stores no foreign authoritative state by default.
