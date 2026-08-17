# Orientation – Acceptance Tests

**Status:** Orientation v0.1.0 published baseline; Issues #1–#4 acceptance evidence is recorded below.

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

Focus: route planning/routing, not full live navigation.

### Issue #10 — generic routing boundary

Issue #10 acceptance requires deterministic validation of the three generic
Travel Profiles, two-point Route Requests, immutable bounded Route Geometry
(2–10,000 Coordinates), and finite non-negative Route distance/duration.
`RoutingService` must return a fake-port Route and preserve the distinct
no-route, unavailable, timeout, rate-limit and invalid-provider-response outcomes.

`POST /api/v1/routes` must accept only Orientation DTOs, validate coordinates
and profile, return an Orientation Route envelope with decoded geometry, and
map failures to `400` invalid input, `404` no route, `502` invalid provider
response, `503` unavailable/timeout and `429` rate limiting. No provider JSON,
Valhalla term, encoded polyline, PositionFix, persistence or network request is
allowed in this boundary slice. Existing Place Search and Reverse Geocoding
behavior, `orientation.host-bridge` 1.0 and all schemas remain unchanged.

### Issue #11 — Valhalla provider

Issue #11 acceptance requires a pinned reviewed Valhalla runtime and bounded
development dataset, deterministic adapter tests, profile mapping for DRIVING,
CYCLING and WALKING, provider response/time limits, provider-neutral failure
mapping, and full polyline6 decoding before a Route crosses `RoutingPort`.
A real local provider smoke must prove the three supported profiles through the
Orientation route endpoint. Valhalla JSON, costing names, error codes and
encoded geometry must not leak into domain/application or consumer boundaries.

### Issue #12 — route rendering

Issue #12 acceptance requires provider-neutral decoded Route geometry to render
on the Map Surface with deterministic replacement/clear/lifecycle behavior,
origin/destination presentation and coherent viewport fitting while preserving
Spatial Scene features and Current Position.

### Issue #13 — Reference Host route planning

Issue #13 acceptance requires explicit endpoints/profile selection, relative
Orientation API calls, route geometry plus distance/duration presentation,
loading/error/no-route handling, stale-request protection and browser evidence
for the complete v0.3.0 route-planning workflow.

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
