# Orientation

Orientation is the `wgt-system` **local-first personal spatial exploration and mobility bounded context** and the system owner of reusable generic geospatial capabilities.

Its product space answers three connected questions:

- **Discover** — What is where, and which spatial candidates satisfy the question I am asking?
- **Explore** — What is this place or spatial object, what evidence/information do I have about it, and what can I do with it?
- **Navigate** — How do I get there?

Current Location supports the same space: Orientation can consume and visualize a position supplied by a host while OS/browser permission ownership stays with that host.

## Standalone product workflow

Orientation is independently useful; it is not only a map/geocoder/router for other bounded contexts.

The released v0.5.0 baseline provides this first-class standalone loop:

```text
spatial question / criteria
        ↓
Orientation-generated research prompt
        ↓
external ChatGPT / research interaction
        ↓
versioned structured JSON
        ↓
strict Orientation validation + import
        ↓
local SQLite-backed Orientation discovery collection + provenance
        ↓
reopen after restart
        ↓
map/list exploration and evidence inspection
        ↓
select destination + explicit origin
        ↓
DRIVING / CYCLING / WALKING direct Route
        or
public-transit Journey alternatives with depart-at / arrive-by
```

Public-transit planning is a separate time-dependent `Journey` model rather than another value in the direct-routing `TravelProfile`. The first adapter is MOTIS v2.11.0; deterministic acceptance uses a pinned self-hosted MOTIS runtime and pinned OSM/GTFS fixture. Public Transit is part of the **released v0.5.0 baseline**.

Research results are evidence-backed input, not automatically authoritative truth. Heuristic matches must not silently become asserted sensitive personal characteristics.

A paid LLM API is not required. Prompt semantics and structured import contracts remain Orientation-owned rather than becoming a generic system-wide prompt service.

## Ownership

Orientation owns:

- personal spatial discovery/research semantics;
- Orientation-owned spatial research prompt/import contracts;
- imported discovery collections, provenance and evidence;
- local persistence for Orientation-owned discovery state;
- map scenes, geometry, features, layers, clustering and hit testing;
- framework-independent map rendering;
- map style/tile/provider integration;
- place/POI discovery;
- geocoding and reverse geocoding;
- direct routing, route geometry, distance/duration and generic directions;
- public-transit Journey semantics, Journey alternatives and Journey rendering;
- generic current-position/accuracy representation;
- mobility-provider adapters and generic mobility-planning semantics introduced by accepted slices;
- provider adapters, caching, failure handling and technical geospatial policy.

Orientation does **not** own:

- Vocation Work Location, Opportunity, Company, Posting or job-market decision semantics;
- Illumination learning semantics;
- Wiiii Got This product-shell/navigation semantics;
- OS permission or device-trust semantics;
- Conveyance durable cross-device delivery;
- foreign authoritative persistence merely because foreign objects can be shown spatially;
- a universal business-entity catalog or generic LLM platform.

## Product integration

Orientation and Vocation remain separate bounded contexts. Vocation remains authoritative for job-market meaning while Orientation owns generic place resolution, spatial exploration and mobility.

`Wiiii Got This` may compose Orientation capabilities into platform-specific product UI without becoming owner of Orientation semantics. The released `orientation.host-bridge` remains version `1.0`; the standalone Orientation browser application is separate from the reusable Embed Host and Reference Host.

Provider-owned rich spatial projections are valid. A marker may expose provider-owned information and external resources without transferring their business meaning to Orientation.

## Repository / runtime shape

```text
orientation/
├── backend/       Java 25 + Maven + Spring Boot + local SQLite persistence
├── map/           TypeScript + MapLibre GL JS; standalone, Reference and Embed hosts
├── contracts/     versioned boundary/import schemas and examples
├── deployment/    external runtime integration such as Valhalla
├── docs/
└── scripts/
```

Valhalla is the upstream engine for direct DRIVING/CYCLING/WALKING routes. MOTIS is the upstream engine behind the public-transit Journey adapter. Neither upstream engine is a WGT-owned codebase or part of Orientation domain semantics.

## Released foundation

- **v0.1.0** — provider-neutral Spatial Scene and Map Surface, rich feature interaction, host-supplied Current Location, `orientation.host-bridge` 1.0, Reference Host and Embed Host.
- **v0.1.1** — OpenFreeMap Liberty street/place basemap.
- **v0.1.2** — explicit MapLibre GL JS 6 Vite worker packaging and vector-source readiness gate.
- **v0.2.0** — Orientation-owned Place model, forward place search and reverse geocoding through a replaceable Photon adapter and narrow HTTP endpoints.
- **v0.3.0** — provider-neutral two-point routing, Valhalla 3.8.3 integration, DRIVING/CYCLING/WALKING, route rendering and Reference Host route planning.
- **v0.4.0** — spatial-research contract 1.0, deterministic external-research prompts, strict import, local SQLite discovery persistence, restart/reopen support and the first standalone discovery-to-route application workflow.
- **v0.5.0** — provider-neutral public-transit Journey planning, MOTIS v2.11.0 integration, Journey rendering and standalone Journey comparison/selection while preserving the v0.4 discovery and direct-routing baseline.

## v0.5.0

The released v0.5.0 baseline adds:

- a provider-neutral, offset-aware public-transit `Journey` boundary with `DEPART_AT` / `ARRIVE_BY`;
- ordered WALK/transit legs, scheduled timing plus optional realtime-adjusted timing, transfers and bounded decoded geometry;
- MOTIS v2.11.0 behind the replaceable Journey provider boundary;
- a dedicated Journey overlay on the reusable Map Surface, separate from direct Route state;
- public-transit alternatives in the standalone app with explicit selection, replacement and clear behavior;
- deterministic self-hosted MOTIS acceptance and real production-browser Journey acceptance while preserving the v0.4 discovery and direct-routing regression path.

v0.5.0 does **not** claim shared mobility/GBFS, fares or ticketing, booking, complete realtime coverage, multimodal optimization across independent sharing providers, turn-by-turn live navigation, automatic background crawling, paid LLM/API execution, Vocation migration completion, cross-device synchronization or physical-iPhone support.

See [`docs/16_PRODUCT_DIRECTION.md`](docs/16_PRODUCT_DIRECTION.md) for the product direction and [`docs/INDEX.md`](docs/INDEX.md) for the complete specification set.
