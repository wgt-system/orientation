# Orientation – Acceptance Tests

**Status:** Orientation v0.1.0 through v0.4.0 acceptance evidence is recorded below.

## Architecture invariants

1. Orientation domain code can compile/test without Spring, MapLibre, Valhalla or foreign domain packages.
2. No Orientation code directly reads/writes a Vocation, Illumination, WGT or Conveyance database.
3. Map surface public types contain no Vocation-specific domain names.
4. Map surface public contract types contain no MapLibre implementation objects.
5. Routing provider implementation does not leak Valhalla response types across the application boundary.
6. Current location is not persisted by default.
7. Host interaction rather than core renderer code owns product navigation/external-resource execution.
8. Orientation-owned discovery persistence does not become persistence for foreign authoritative domain state.
9. Imported research claims remain evidence/provenance-bearing research data; heuristic matches do not silently become asserted sensitive personal characteristics.

## Bootstrap checks

- Java backend builds on Java 25.
- Spring application context test passes.
- TypeScript strict typecheck passes.
- Map application/reference/embed artifacts build.
- Map model tests pass.
- CI runs backend and map checks independently.
- dependency security scanning runs as a release gate.
- `git diff --check` is clean before release promotion.

## v0.1.0 — Map-surface foundation

Acceptance includes the provider-neutral Spatial Scene/Map Surface, rich spatial feature interaction, host-supplied Current Location, `orientation.host-bridge` 1.0, Reference Host and Embed Host.

Renderer evidence covers deterministic scene replacement/clearing, identity/selection events, generic viewport behavior, lifecycle cleanup, rich resources/actions, safe URI handling, accessibility, current-position update/clear behavior, antimeridian handling and real Reference/Embed browser behavior including the 500-feature fixture.

## v0.1.1 — Basemap patch

Released and accepted on 2026-08-16.

OpenFreeMap Liberty is the default basemap. Browser evidence covers visible street/place detail, attribution, Reference and Embed rendering, explicit map-failure status and preservation of v0.1.0 scene/bridge/current-location behavior.

## v0.1.2 — MapLibre worker runtime patch

Released and accepted on 2026-08-16.

Acceptance requires the explicitly bundled Vite worker to be registered before Map creation and the `openmaptiles` vector source to reach its loaded state in both development and production-build browser runs. Raster-only relief is not accepted as proof of vector readiness.

## v0.2.0 — Geocoding and place search

Released and accepted on 2026-08-16.

Acceptance covers the Orientation-owned Place model, forward search and reverse geocoding, deterministic Photon adapter mapping/failure handling, narrow first-party HTTP boundaries, explicit Reference Host search/reverse actions, stale-request protection and preservation of Current Position/Host Bridge behavior.

The browser never calls Photon directly. PositionFix is not forwarded automatically. Provider responses are bounded and normalized before crossing the Orientation infrastructure boundary.

## v0.3.0 — Routing

Released and accepted on 2026-08-17.

### Generic routing boundary

Accepted semantics include DRIVING/CYCLING/WALKING, two-point Route Requests, bounded decoded Route Geometry, finite non-negative distance/duration, `RoutingPort`, `RoutingService`, stable failures and `POST /api/v1/routes`.

### Valhalla provider

Valhalla 3.8.3 is the first replaceable routing provider behind Orientation infrastructure. Adapter tests cover profile mapping, bounded responses, timeouts/errors, stable failure mapping and polyline6 decoding before a Route crosses `RoutingPort`.

The real provider smoke passes through Orientation for all supported profiles and verifies non-empty geometry, positive distance/duration and endpoints near the requested coordinates.

### Route rendering and Reference Host planning

The Map Surface renders provider-neutral route geometry with deterministic set/replace/clear behavior while preserving Spatial Scene and Current Position overlays. The production-browser smoke uses deterministic Photon, real Valhalla, the Orientation backend, production Vite host and Chrome/ChromeDriver to prove explicit endpoint selection, visible route rendering, profile invalidation, replacement/clear and preservation of Place Search/Reverse/Current Position behavior.

`orientation.host-bridge` remains 1.0; v0.3.0 adds no turn-by-turn guidance, live GPS rerouting, persistence, OS/device permission ownership or foreign-domain semantics.

## v0.4.0 — Standalone spatial research and persistent discovery

### #20 — Spatial research contract

Acceptance covers:

- `orientation.spatial-research` contract version 1.0;
- strict shape plus semantic validation;
- unique local refs and valid source/claim references;
- exactly one claim per requested criterion;
- evidence/provenance retention;
- explicit heuristic/evidence distinction;
- no automatic conversion of heuristic matches into asserted sensitive personal characteristics.

### #21 — Prompt generation

Acceptance covers deterministic prompt generation from validated explicit question/criteria input, embedded contract/version guidance and a narrow HTTP boundary returning copy/export-ready text. No paid LLM/API execution or generic prompt service is introduced.

### #22 — Import and persistence

Acceptance covers validate-before-mutation, explicit contract-to-domain translation, local SQLite persistence, relational provenance/claim retention, one-transaction import, canonical content fingerprinting, deterministic `UNCHANGED` re-import behavior, invalid-import no-mutation behavior and reopen after repository/backend restart.

Provider-backed `Place` data remains distinct from imported researched claims.

### #23 — Standalone discovery application

The standalone browser application is separate from the Reference and Embed Hosts. Acceptance covers:

1. create a spatial research question;
2. generate the matching prompt;
3. reject invalid structured input without mutation;
4. import a valid structured result;
5. list and inspect the persisted collection/candidates/evidence;
6. restart the Orientation backend using the same SQLite database;
7. reopen the same collection;
8. select a researched candidate on the map;
9. route to the selected destination with existing DRIVING/CYCLING/WALKING routing;
10. replace/clear routing without corrupting discovery state.

The real production product smoke uses deterministic Photon, real Valhalla 3.8.3, the Orientation backend, production Vite artifacts and Chrome/ChromeDriver.

### #24 — Integrated hardening/release gate

Release acceptance requires all of the following on the final hardening PR head:

- backend CI PASS;
- map tests/typecheck/production build PASS;
- real Valhalla route smoke PASS for DRIVING/CYCLING/WALKING;
- Reference Host Chrome smoke PASS;
- standalone import -> backend restart -> reopen -> route Chrome smoke PASS;
- dependency-vulnerability scan PASS;
- documentation/repository release-state consistency;
- unchanged `orientation.host-bridge` 1.0 consumer contract;
- no claim of public transit, realtime transit, shared mobility, multimodal routing, live turn-by-turn navigation, automatic background research, paid LLM/API execution, Vocation migration completion, cross-device synchronization or physical-iPhone validation.

## Cross-repository integration state

### Vocation

Vocation remains authoritative for job-domain location and job-market semantics. Orientation v0.4.0 does not require or claim Vocation migration completion.

### WGT Windows

Desktop integration has already replaced the previous generic Mapsui fallback with the accepted Orientation surface through the WGT-owned presentation adapter. WGT remains shell/navigation/presentation owner and `orientation.host-bridge` remains 1.0.

### WGT iPhone

Physical iPhone support is not established. The WGT iPhone integration gate is intentionally deferred/not planned while Desktop is prioritized and a physical Apple validation environment is unavailable. This is not an Orientation v0.4.0 release blocker and must not be described as released iPhone support.

### Conveyance

Orientation does not duplicate durable cross-device relay behavior. Conveyance remains the owner of generic durable opaque delivery.
