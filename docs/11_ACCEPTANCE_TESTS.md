# Orientation – Acceptance Tests

**Status:** Orientation v0.1.0 through v0.3.0 acceptance evidence is recorded below.

## Architecture invariants

1. Orientation domain code can compile/test without Spring, MapLibre, Valhalla or foreign domain packages.
2. No Orientation code directly reads/writes a Vocation, Illumination, WGT or Conveyance database.
3. Map surface public types contain no Vocation-specific domain names.
4. Map surface public contract types contain no MapLibre implementation objects.
5. Routing provider implementation does not leak Valhalla response types across the application boundary.
6. Current location is not persisted by default.
7. Host interaction rather than core renderer code owns product navigation/external-resource execution.

## Bootstrap checks

- Java backend builds on Java 25.
- Spring application context test passes.
- TypeScript strict typecheck passes.
- Map reference host builds.
- Map model tests pass.
- CI runs backend and map checks independently.
- `git diff --check` is clean.

## First renderer proof

Given three generic Spatial Features:

- map initializes successfully;
- all features can be represented;
- selecting a feature emits its opaque feature ref;
- rich resources/actions can be presented without provider-specific code;
- the map can be destroyed/recreated without leaking state.

Issue #1 additionally requires deterministic repeated scene replacement, empty-scene clearing,
feature/source identity selection events, generic empty/focus/fit/preserve viewport resolution,
explicit renderer lifecycle states, and no duplicate marker handlers after updates.

Issue #2 additionally requires validated generic information/resources/actions, duplicate and
unsafe-URI rejection, immutable rich snapshots, opaque resource/action activation events,
keyboard-accessible text-only details controls, stale-detail replacement cleanup, and host-owned
resource/action execution.

Issue #3 additionally requires host-supplied immutable PositionFix validation, independent
set/update/clear behavior, geographic accuracy visualization, no automatic viewport following,
no retained location history, and deterministic cleanup across renderer lifecycle transitions.

Issue #4 acceptance evidence includes the validated `orientation.host-bridge` 1.0
JSON envelope/schema, independently testable protocol core, deterministic bridge/map
lifecycle, separate Embed Host artifact, malformed-message rejection, timestamp/
accuracy/antimeridian hardening, a model-only 500-feature fixture, and real
Reference/Embed browser evidence for 500 rendered markers, replacement, clearing,
reload, selection, rich actions, current location and antimeridian viewport behavior.
Accuracy areas larger than 5,000 km or enclosing a pole are intentionally omitted
from the display while the validated position point remains available.

The v0.1.0 published baseline includes the provider-neutral Spatial Scene/Map
Surface, Rich Spatial Feature Interaction, host-supplied Current Location,
`orientation.host-bridge` 1.0, Reference Host, and embeddable browser/WebView
Host. WGT Windows integration, physical-iPhone proof, Vocation migration,
geocoding, Place Discovery, and Routing/Valhalla remain outside this release.

## v0.1.1

Focus: basemap patch. Released and accepted on 2026-08-16.

The default basemap is OpenFreeMap Liberty
(`https://tiles.openfreemap.org/styles/liberty`) with OpenStreetMap/OpenMapTiles
data. Browser evidence covers visible street/place detail, attribution,
Reference and Embed rendering, map failure status, and preservation of v0.1.0
scene, bridge, rich interaction, current-location and viewport behavior.
Remote basemap availability remains external/best-effort with no SLA promise.

## v0.1.2

Focus: MapLibre GL JS 6 Vite worker runtime patch. Released and accepted on
2026-08-16.

The released behavior explicitly bundles the worker as a Vite worker asset and
sets `setWorkerUrl(...)` before Map creation. The Reference and Embed browser
checks confirmed that the `openmaptiles` vector source reached
`isSourceLoaded(...) === true`, alongside visible streets, road labels and
city/place labels. Raster-only `ne2_shaded` relief was not accepted as evidence.
Dev-server and production-build static-host checks verified the bundled worker
request and vector-tile requests without worker 404s or persistent
vector-source failures. The OpenFreeMap Liberty selection and v0.1.1 contracts
remain unchanged.

## v0.2.0

Focus: Geocoding and place search.

Released and accepted on 2026-08-16.

Package #7 acceptance covers deterministic domain/application validation,
Photon adapter mapping and failure handling through a local HTTP stub, the
Orientation-owned search/reverse HTTP endpoints, stateless operation, map
regressions and a minimal live Photon smoke for `Hamburg Hauptbahnhof`,
`Brandenburger Tor` and one reverse coordinate.

Package #8 adds the Reference Host consumer boundary: validated relative API
DTOs, explicit-submit search, explicit map-center reverse lookup, stale-request
protection, current-scene replacement, immediate details and marker focus.
The browser never calls Photon directly, PositionFix is not forwarded, and the
host bridge remains `orientation.host-bridge` 1.0.

The released backend scope is the Orientation-owned Place model, forward place
search, reverse geocoding, provider-neutral ports, configurable Photon
infrastructure, provider error mapping and bounded response handling. Spring
Boot 4.1 uses Jackson 3; raw Photon `type` is not exposed as `Place.kind`,
Photon extents are normalized into Orientation BoundingBox ordering, and
unknown-length provider bodies read at most `MAX + 1` bytes after a 1 MiB hard
limit. The browser/backend boundary is relative Orientation `/api` traffic
only; Vite dev/preview proxying targets the backend and no permissive CORS
workaround is used.

The Reference Host release includes explicit-submit search, result-list
selection, immediate details, map focus, current-scene marker reselection and
an explicit `Identify map center` reverse action. No automatic reverse lookup,
autocomplete, PositionFix forwarding, routing, persistence or foreign-domain
migration is included. OpenFreeMap Liberty, MapLibre 6 worker/vector readiness,
current-location visualization, `orientation.host-bridge` 1.0 and all schemas
remain unchanged.

## v0.3.0 — Routing

Focus: route planning/routing, not full live navigation. Released and accepted
on 2026-08-17.

### Issue #10 — generic routing boundary

Accepted provider-neutral semantics include the three generic Travel Profiles,
two-point Route Requests, immutable bounded Route Geometry (2–10,000
Coordinates), finite non-negative route distance/duration, `RoutingPort`,
`RoutingService`, stable routing failures and `POST /api/v1/routes`.

The HTTP boundary accepts only Orientation DTOs, validates coordinates/profile,
returns decoded Orientation Route geometry, and maps failures to stable
Orientation outcomes. No Valhalla term, provider JSON, encoded polyline,
PositionFix, persistence or network-provider detail crosses this boundary.

### Issue #11 — Valhalla provider

Valhalla 3.8.3 is accepted as the first replaceable routing provider behind the
Orientation infrastructure boundary. The reviewed runtime uses
`ghcr.io/valhalla/valhalla-scripted:3.8.3` plus retained Hamburg monthly OSM
data. Adapter tests cover DRIVING/CYCLING/WALKING profile mapping, bounded
responses, timeouts/errors, stable failure mapping and full polyline6 decoding
before a Route crosses `RoutingPort`.

The real provider smoke passes through Orientation for all supported profiles:

- DRIVING: 136 decoded points, 2251.0 m, 302.823 s;
- CYCLING: 93 decoded points, 1342.0 m, 314.502 s;
- WALKING: 58 decoded points, 1103.0 m, 781.912 s.

Decoded route endpoints are verified near the requested coordinates.

### Issue #12 — route rendering

The Map Surface accepts provider-neutral decoded Route geometry and renders a
route casing/line plus origin/destination points. Acceptance covers validated
immutable route snapshots, deterministic set/replace/clear behavior, lifecycle
cleanup, preservation of Spatial Scene and Current Position overlays, and
ordinary plus antimeridian-safe viewport fitting.

Integrated visible evidence through the Reference Host proves real route
rendering. With an unchanged fitted map viewport, clearing the Driving route
changed 5,935 map pixels and clearing the Cycling route changed 3,421 map
pixels, while measured normal screenshot drift was 0 pixels.

### Issue #13 — Reference Host route planning

The accepted Reference Host flow provides explicit Start and Destination place
searches and user selection, explicit DRIVING/CYCLING/WALKING profile choice,
relative Orientation route API calls, distance/duration/profile summaries,
controlled invalid-request/no-route/rate-limit/unavailable/provider-response
states, stale-request cancellation and explicit route clear/replacement.

The integrated production-browser smoke uses a deterministic Photon provider,
real Valhalla 3.8.3, the Orientation backend, production Vite Reference Host and
Chrome/ChromeDriver. It proves existing general Place Search, Reverse Lookup and
Current Position behavior, explicit Start/Destination selection, visible
Driving route render and clear, profile-change invalidation, visible Cycling
replacement and clear, plus constrained Desktop and mobile scrolling behavior.

The normal post-merge `dev` CI and the complete Valhalla + production-browser
smoke both pass on commit `7e6902bb8c1015aef506bd5a94ede702c705c198`.

Preserved release invariants: `orientation.host-bridge` remains 1.0; Valhalla
wire/provider semantics remain infrastructure-only; v0.3.0 adds no turn-by-turn
guidance, live GPS rerouting, persistence, OS/device permission ownership or
foreign-domain semantics.

## Future integration gates

### Vocation

- Vocation can consume Orientation geocoding without transferring Work Location/Precision authority.
- rich Vocation spatial projection supports required external resources.
- Vocation reference map no longer needs its own generic Leaflet implementation after migration.

### WGT Windows

- WGT can host the Orientation map surface;
- selection/resource/action events reach the WGT presentation adapter;
- WGT shell/navigation remains WGT-owned.

### WGT iPhone

- same renderer capability works on a physical iPhone host;
- touch/pan/zoom/selection are usable;
- lifecycle/reload behavior is correct;
- current-position input works when host permission is granted.

Legacy renderer deletion occurs only after the relevant gates pass.
