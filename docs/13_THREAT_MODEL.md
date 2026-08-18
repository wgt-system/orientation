# Orientation – Threat Model

**Status:** current post-v0.5 local-first baseline.

## Sensitive assets

- precise current location;
- place-search text;
- route origins/destinations;
- Journey departure/arrival time;
- imported personal discovery collections and provenance;
- external-resource URIs and provider-derived place/address data.

## Location and mobility privacy

Searches and navigation requests can reveal sensitive personal intent even without a stored PositionFix.

Controls:

- Orientation backend binds to `127.0.0.1` by default;
- MOTIS and Valhalla default to explicit loopback endpoints;
- no automatic hosted provider fallback;
- Place Search and Reverse Geocoding use the local MOTIS provider path;
- direct Route uses local Valhalla by default;
- public-transit Journey uses local MOTIS by default;
- no position history persistence unless a later explicit retention decision allows it;
- do not log full search text, coordinates or Journey request details at normal INFO level;
- host owns current-location permission acquisition.

A deliberately configured non-loopback provider is an operator/deployment choice and must not be treated as privacy-equivalent to the default local topology.

## Intentional external basemap

OpenFreeMap Liberty is the sole intentional external runtime dependency in the default browser product.

Controls:

- only hosted map style/tile resources are requested;
- no Orientation discovery collection, search string, Route request or Journey time is serialized into tile requests by application code;
- map-resource failure is surfaced as renderer failure rather than triggering another provider;
- attribution/provider requirements remain visible and respected;
- remote styles are infrastructure input, not trusted application code.

The visible viewport necessarily determines which remote map resources are requested. An offline/private basemap requires a separate caching/packaging capability.

## External research interaction

Orientation can generate a prompt for explicit user-controlled external research. This is not an automatic backend API call.

Controls:

- external research is initiated explicitly by the user;
- structured results are untrusted until strict contract/semantic validation succeeds;
- imported claims preserve provenance and uncertainty;
- heuristics must not silently become asserted sensitive characteristics;
- rejected imports do not partially mutate persisted Orientation state.

## Malicious/untrusted rich content

Provider/domain strings can reach marker/details UI.

Controls:

- render text as text, never arbitrary provider HTML/CSS;
- Spatial Feature contracts reject executable fragments;
- generic web resources accept only reviewed safe URI schemes;
- resource/action execution remains host-mediated.

## Provider/API abuse and malformed responses

Controls:

- finite connect/read timeouts;
- bounded provider response bodies before parsing;
- stable Orientation failure kinds instead of raw provider bodies;
- provider DTOs remain inside infrastructure;
- no arbitrary user-supplied backend fetch target;
- no automatic retries unless separately justified;
- domain geometry/list bounds remain enforced after translation.

MOTIS handles both Place and Journey provider roles but the application ports remain separate. A malformed geocoding result must not weaken Journey invariants, and vice versa.

## SSRF / provider configuration

MOTIS and Valhalla base URLs are application configuration, not request parameters. User/imported content cannot select an upstream target.

Controls:

- default to loopback targets;
- deployment owners must review any configured non-loopback target;
- do not turn spatial-resource URLs or imported source URLs into backend fetch targets;
- current JDK clients do not opt into arbitrary redirect following.

## WebView/browser bridge

The accepted Host Bridge 1.0 treats inbound JSON as untrusted and validates contract/version/type, closed payload structure, finite coordinates/numbers, safe URI schemes and canonical timestamps before state mutation.

Controls:

- narrow command/event contract;
- no arbitrary JavaScript evaluation;
- explicit message-type dispatch;
- safe stable errors without raw payloads/stacks;
- host remains responsible for product/domain navigation and OS permission policy.

## Cross-context leakage

Controls:

- no foreign DB access;
- no broad persistence of foreign aggregates;
- provider contexts decide what they project spatially;
- Orientation SQLite stores Orientation-owned discovery state only;
- technical MOTIS/Valhalla indexes are not authoritative business data.

## Mobile deployment boundary

Responsive browser support does not justify exposing a desktop backend or provider runtime on the LAN by default.

A future phone workflow must choose an explicit trusted topology (for example a deliberately paired companion or bounded on-device regional runtime). It must not weaken loopback defaults merely to make ad-hoc LAN access convenient.
