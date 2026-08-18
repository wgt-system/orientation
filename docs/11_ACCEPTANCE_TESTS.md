# Orientation – Acceptance Tests

**Status:** Orientation v0.1.0 through v0.5.0 are released and accepted. v0.5.0 public-transit Journey work #40–#44 completed the first public-transit release baseline.

## Architecture invariants

1. Orientation domain code can compile/test without Spring, MapLibre, Valhalla, MOTIS or foreign domain packages.
2. No Orientation code directly reads/writes a Vocation, Illumination, WGT or Conveyance database.
3. Map Surface public types contain no Vocation-specific domain names.
4. Map Surface public contract types contain no MapLibre implementation objects.
5. Direct-routing provider implementation does not leak Valhalla response types across the application boundary.
6. Journey provider implementation does not leak MOTIS DTOs, provider mode enums or error bodies across the application boundary.
7. Current Location is not persisted by default.
8. Host interaction rather than renderer core owns product navigation/external-resource execution.
9. Orientation-owned discovery persistence does not become persistence for foreign authoritative domain state.
10. Imported research claims remain evidence/provenance-bearing research data; heuristic matches do not silently become asserted sensitive personal characteristics.
11. Direct `Route` and time-dependent `Journey` remain distinct models; public transit is not represented by adding `TRANSIT` to the released direct `TravelProfile`.

## Bootstrap and release checks

- Java backend builds/tests on Java 25.
- Spring application context test passes.
- TypeScript strict typecheck and map tests pass.
- Standalone, Reference and Embed production artifacts build.
- CI runs backend and map checks independently.
- dependency-security scanning runs as a release gate.
- `git diff --check` is clean before release promotion.
- `orientation.host-bridge` 1.0 remains byte-for-byte unchanged unless an independently reviewed consumer need requires a successor.

## Released acceptance baseline

### v0.1.0 — Map-surface foundation

Accepted provider-neutral Spatial Scene/Map Surface, rich spatial feature interaction, host-supplied Current Location, `orientation.host-bridge` 1.0, Reference Host and Embed Host. Evidence covers deterministic scene lifecycle, generic viewport behavior, accessibility, current-position presentation and real browser behavior.

### v0.1.1 — Basemap patch

Released and accepted on 2026-08-16. OpenFreeMap Liberty is the default basemap; browser evidence covers visible street/place detail, attribution and controlled renderer/basemap failure state.

### v0.1.2 — MapLibre worker runtime patch

Released and accepted on 2026-08-16. Acceptance requires explicit Vite worker packaging/registration and real `openmaptiles` vector-source readiness in production browser runs.

### v0.2.0 — Geocoding and Place search

Released and accepted on 2026-08-16. Acceptance covers Orientation-owned `Place`, forward search/reverse geocoding, Photon isolation, bounded provider responses, stable failures and first-party HTTP boundaries. Browser hosts never call Photon directly.

### v0.3.0 — Direct routing

Released and accepted on 2026-08-17. Acceptance covers provider-neutral DRIVING/CYCLING/WALKING Route semantics, Valhalla 3.8.3 isolation, decoded/bounded geometry, real-provider smoke, Map Surface Route lifecycle and production Reference Host route planning.

### v0.4.0 — Standalone spatial research and persistent discovery

Released and accepted on 2026-08-17.

Acceptance covers:

- Spatial Research Bundle 1.0 strict shape/semantic validation and provenance;
- deterministic research prompt generation;
- validate-before-mutation SQLite import and canonical unchanged re-import handling;
- backend/repository restart and reopen of the same collection;
- standalone question → prompt → import → evidence/map → selected destination flow;
- real production-browser direct routing to an imported candidate;
- dependency-security and repository consistency gates.

The v0.4 browser regression remains a release gate for later versions: import → restart → reopen → DRIVING route → clear must keep discovery state intact.

## v0.5.0 — Public-transit Journey

Released and accepted on 2026-08-18.

### #40 — Provider-neutral Journey boundary

Acceptance covers:

- explicit origin/destination;
- `DEPART_AT` / `ARRIVE_BY` with offset-aware time;
- one to eight alternatives;
- ordered bounded legs with at least one transit leg;
- provider-neutral modes (`WALK`, `RAIL`, `SUBURBAN_RAIL`, `SUBWAY`, `TRAM`, `BUS`, `COACH`, `FERRY`, `OTHER_TRANSIT`);
- scheduled timing plus optional realtime-adjusted timing without claiming realtime availability when absent;
- stable Journey provider failures;
- `POST /api/v1/journeys`.

### #41 — MOTIS v2.11.0 provider

Acceptance covers deterministic request/response adapter fixtures plus a real self-hosted MOTIS smoke.

The real gate pins:

- MOTIS v2.11.0 and its reviewed archive SHA-256;
- a fixed Aachen OSM/GTFS test-data commit and blob identities;
- a deterministic valid timetable window derived from the pinned GTFS data.

The gate proves `pinned OSM+GTFS → MOTIS import/server → Orientation → /api/v1/journeys → provider-neutral Journey`, including real transit legs and decoded geometry. Public Transitous is not a CI/release dependency.

### #42 — Journey Map Surface

Acceptance covers a Journey overlay independent of direct Route state:

- deterministic set/replace/clear/current lifecycle;
- WALK vs transit line semantics that do not rely on color alone;
- explicit transit-stop and endpoint markers;
- bounded provider-neutral geometry;
- antimeridian-safe viewport fitting;
- cleanup on destroy;
- unchanged Spatial Scene, Current Position, direct Route and Host Bridge semantics.

### #43 — Standalone Journey product flow

The focused production-browser acceptance uses the same pinned self-hosted MOTIS fixture plus a deterministic local Photon origin and proves:

1. import a mapped discovery destination through the standalone UI;
2. select an explicit origin;
3. choose Public transit and an explicit depart-at/arrive-by time;
4. obtain real Journey alternatives;
5. inspect duration/transfers/ordered WALK+transit legs and scheduled/realtime timing state;
6. select and render a Journey on the Map Surface;
7. replace the alternative when more than one is returned;
8. switch to direct routing and verify stale Journey presentation disappears without losing discovery state;
9. request another Journey and clear it explicitly while preserving discovery state.

### #44 — Integrated hardening/release gate

Release acceptance required all of the following on the final release candidate:

- backend CI PASS;
- map tests/typecheck/production build PASS;
- dependency-security PASS;
- self-hosted MOTIS Journey smoke PASS;
- production standalone Journey browser flow PASS;
- real Valhalla direct-route smoke PASS for DRIVING/CYCLING/WALKING;
- Reference Host Chrome smoke PASS;
- v0.4 standalone import → restart → reopen → direct-route regression PASS;
- documentation/repository release-state consistency;
- unchanged `orientation.host-bridge` 1.0;
- no unsupported claim of shared mobility/GBFS, fares/ticketing, booking, complete realtime coverage, arbitrary multimodal sharing optimization, live turn-by-turn navigation, Vocation migration completion, cross-device synchronization or physical-iPhone support.

The gates passed and explicit Control Plane approval authorized promotion to `main`, tag/release creation, #44 closure and milestone closure.

## Cross-repository integration state

### Vocation

Vocation remains authoritative for job-domain location and job-market semantics. Orientation v0.5.0 does not require or claim Vocation migration completion. A future Vocation route/journey action may consume Orientation without moving job semantics into Orientation.

### WGT Windows

Desktop integration uses the Orientation surface through WGT-owned presentation composition. WGT remains shell/navigation/presentation owner and `orientation.host-bridge` remains 1.0.

### WGT iPhone

Physical iPhone support is not established and is not an Orientation v0.5.0 release claim or blocker.

### Conveyance

Orientation does not duplicate durable cross-device relay behavior. Conveyance remains the owner of generic durable opaque delivery.
