# Orientation – Implementation Plan

**Status:** v0.1.0 through v0.4.0 are released. The v0.5.0 implementation packages #40–#43 are complete on `dev`; #44 is the active hardening/release gate. v0.5.0 is not released until explicit promotion to `main`, tag and GitHub Release complete.

Milestone names, when created, use semantic versions only. Do not pre-create speculative future milestone ladders.

## Release ledger

### v0.1.0 — Map-surface foundation

Released.

Delivered the provider-neutral Spatial Scene and reusable Map Surface, rich feature interaction, host-supplied Current Location, `orientation.host-bridge` 1.0, Reference Host and Embed Host.

### v0.1.1 — Basemap patch

Released on 2026-08-16.

Replaced the MapLibre demonstration basemap with OpenFreeMap Liberty and made renderer/basemap failure visible while preserving v0.1.0 contracts.

### v0.1.2 — MapLibre worker runtime patch

Released on 2026-08-16.

Bundled and registered the MapLibre GL JS 6 worker explicitly with Vite and added vector-source readiness evidence for production browser hosts.

### v0.2.0 — Geocoding and place search

Released and accepted on 2026-08-16.

Delivered the Orientation-owned `Place` model, forward place search and reverse geocoding through replaceable application ports, the Photon adapter and narrow first-party HTTP endpoints. Browser hosts call Orientation rather than Photon directly.

Vocation remains authoritative for Work Location/Precision and job-market semantics. Migration of legacy Vocation geocoding/rendering remains consumer work, not Orientation release scope.

### v0.3.0 — Direct routing

Released and accepted on 2026-08-17.

Completed packages #10–#13:

1. provider-neutral `RouteRequest`, `TravelProfile`, `Route`, decoded/bounded geometry, `RoutingPort`/`RoutingService` and `POST /api/v1/routes`;
2. Valhalla 3.8.3 adapter for DRIVING/CYCLING/WALKING with stable failures and real-provider smoke;
3. reusable Map Surface Route overlay with replace/clear/lifecycle and antimeridian-safe viewport;
4. Reference Host Start/Destination search, profile selection and direct-route workflow.

`orientation.host-bridge` remained 1.0. Live navigation, GPS rerouting, persistence and transit were not part of v0.3.0.

### v0.4.0 — Standalone spatial research/discovery

Released and accepted on 2026-08-17.

Completed packages #20–#24:

1. **#20 — Spatial research/import semantics**: versioned Spatial Research Bundle 1.0, criteria, provenance/evidence and strict heuristic/sensitive-trait boundaries.
2. **#21 — Prompt generation**: deterministic external-research prompts from explicit user criteria; no paid LLM/API requirement and no generic prompt service.
3. **#22 — Import/persistence**: validate-before-mutation, anti-corruption translation, local SQLite persistence, canonical re-import handling and restart/reopen support.
4. **#23 — Standalone app**: question → prompt → import → collection → map/evidence → selected destination → DRIVING/CYCLING/WALKING Route.
5. **#24 — Hardening/release**: backend/map regression, real Valhalla/Chrome product smoke, dependency-security gate and repository/release consistency.

## v0.5.0 — Public-transit Journey

**Focus:** add first-class time-dependent public-transit planning while preserving the released direct `Route` boundary.

### #40 — Define provider-neutral Journey boundary — complete

Delivered:

- separate `JourneyRequest` / `JourneyPlan` / `Journey` / ordered `JourneyLeg` model;
- explicit `DEPART_AT` / `ARRIVE_BY` with offset-aware time;
- provider-neutral transit modes rather than provider enums;
- scheduled timing plus optional realtime-adjusted timing;
- bounded alternatives, legs, intermediate stops and decoded geometry;
- stable provider failures and `POST /api/v1/journeys`.

`TRANSIT` is deliberately not added to the direct-routing `TravelProfile`.

### #41 — Integrate MOTIS v2.11.0 — complete

Delivered:

- MOTIS `/api/v6/plan` behind `JourneyPort`;
- provider DTO/error/mode isolation;
- explicit transit-mode request set excluding rental/shared/ODM/ride-sharing modes;
- bounded provider responses and decoded polyline6 geometry;
- local MOTIS default configuration;
- deterministic self-hosted acceptance using pinned MOTIS v2.11.0 and pinned Aachen OSM/GTFS test data.

Transitous may be configured explicitly for reference/manual use but is not a deterministic CI or release dependency.

### #42 — Render Journeys on the Map Surface — complete

Delivered:

- Journey overlay/controller/source/layers separate from direct Route state;
- ordered WALK/transit geometry;
- dashed WALK presentation, solid transit presentation and explicit transit-stop markers;
- deterministic set/replace/clear/destroy behavior;
- antimeridian-safe Journey viewport fitting;
- no change to `orientation.host-bridge` 1.0.

### #43 — Integrate Journey planning into the standalone app — complete

Delivered:

- Public transit beside DRIVING/CYCLING/WALKING;
- explicit origin, selected discovery destination, depart-at/arrive-by and local date/time input;
- offset-aware Journey requests;
- alternative comparison with departure/arrival, duration, transfers and ordered legs;
- explicit scheduled vs realtime-adjusted timing presentation;
- alternative selection and Journey Map Surface rendering;
- stale-request cancellation and Route/Journey switching;
- Journey replacement/clear without corrupting discovery state;
- focused production-browser acceptance against self-hosted MOTIS.

### #44 — Harden and release v0.5.0 — active

Release-candidate hardening must prove on the final PR head:

- backend CI PASS;
- map tests/typecheck/production build PASS;
- dependency-security gate PASS;
- deterministic self-hosted MOTIS Journey smoke PASS;
- production standalone Journey browser acceptance PASS;
- existing Valhalla DRIVING/CYCLING/WALKING + Reference Host regression PASS;
- existing v0.4 import → restart → reopen → direct-route regression PASS;
- repository documentation and release-state consistency;
- unchanged `orientation.host-bridge` 1.0 consumer contract;
- no unsupported claims for shared mobility, fares/ticketing, full realtime coverage, live navigation, Vocation migration or physical-iPhone support.

After #44 passes, the release remains blocked on explicit Control Plane approval. Hardening alone must not merge to `main`, create tag `v0.5.0`, publish a GitHub Release or close the milestone.

## Dependency order

```text
#40
 └── #41

#40 + #41
 └── #42

#41 + #42
 └── #43

#40 + #41 + #42 + #43
 └── #44
```

## Sequencing rule

Stabilize the smallest consumed boundary first. Do not create future contracts or milestones merely because a provider exposes additional features. Shared mobility, richer disruption/realtime semantics, multimodal planning and consumer migrations require separately justified product slices after v0.5.0 is released and reviewed.
