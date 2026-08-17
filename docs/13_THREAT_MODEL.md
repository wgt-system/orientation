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
- bounded retries where deliberately introduced;
- rate limiting/backoff where provider requires it;
- no secrets in logs/contracts;
- explicit trusted provider configuration;
- bounded provider response bodies before parsing.

For v0.2.0, explicitly submitted search text and explicit reverse-geocoding
coordinates are sent to the trusted configured Photon endpoint. PositionFix,
current device location, identity and analytics identifiers are not forwarded
automatically. Search input is bounded, provider URLs are not caller supplied,
and full search text is not logged at INFO by default.

The Reference Host uses only relative Orientation API URLs. Vite's local dev
and preview proxy are configuration for the development host; no Photon URL or
provider secret is exposed through `VITE_*` browser configuration. Search is
explicit-submit only, and reverse geocoding sends only the explicitly chosen
map-center Coordinate. A PositionFix is never used as a search bias or reverse
request.

For v0.3.0, Issue #10 established the local provider-neutral routing boundary.
Issue #11 is the explicit Valhalla provider boundary: only the origin,
destination and generic Travel Profile from an explicit Route Request are
translated into a request to the trusted configured Valhalla endpoint. The
default development endpoint is local (`http://localhost:8002`); deployment may
configure another trusted endpoint. PositionFix, identity, analytics identifiers,
search history and foreign-domain state are not forwarded implicitly.

The Valhalla adapter uses finite connect/read timeouts, does not introduce
automatic retries, bounds provider responses to 2 MiB before parsing, reduces
provider errors to stable Orientation failure kinds, decodes polyline6 before
crossing `RoutingPort`, and enforces the existing 10,000-Coordinate Route
Geometry bound. Raw provider response bodies, error details and route request
coordinates are not exposed in stable error messages.

### SSRF

If backend adapters accept configured URLs:

- do not allow arbitrary user/provider scene data to become backend fetch targets;
- constrain provider endpoints through trusted configuration;
- do not follow redirects unless a provider-specific review requires them;
- validate deployment network targets where relevant.

Photon and Valhalla provider URLs are application configuration, not request
parameters. The JDK HTTP clients used by the current adapters do not opt into
redirect following.

### WebView/browser bridge

Controls:

- narrow bridge/event contract;
- validate all inbound messages;
- no arbitrary JavaScript evaluation as a domain command channel;
- Content Security Policy for reference/embedded host where practical.

The accepted Host Bridge 1.0 boundary treats inbound JSON as untrusted: it
checks contract/version/type, closed payload structure, finite coordinates and
numbers, existing scene/resource/PositionFix rules, safe URI schemes and
canonical timestamps before invoking renderer state. Dispatch uses an explicit
message-type switch; it does not use prototype lookup, eval, Function
constructors, arbitrary navigation or fetch. Bridge errors expose stable safe
codes/messages without raw payloads or stacks.

When embedded, the parent page is the authorized host context. Outbound
`postMessage` uses `"*"` deliberately so the same artifact works from local
files and generic WebViews; the bridge does not send scene or current-position
data without a corresponding host command or user interaction.

### Tile/style supply chain

Controls:

- explicit provider configuration and attribution;
- fail visibly when required resources are unavailable;
- do not treat remote styles as trusted application code.

The v0.1.1 default style is OpenFreeMap Liberty, backed by external
OpenStreetMap/OpenMapTiles resources. Requests are limited to the visible map
viewport and contain no PositionFix data, analytics or telemetry. Availability
is best-effort; a renderer error is surfaced rather than treated as trusted
application state.

### Cross-context leakage

Controls:

- no foreign DB access;
- no broad serialization of foreign aggregates;
- provider decides what may be published into a spatial projection;
- Orientation stores no foreign authoritative state by default.
