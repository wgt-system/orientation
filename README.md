# Orientation

Orientation is the `wgt-system` **local-first personal spatial exploration and mobility bounded context** and the system owner of reusable generic geospatial capabilities.

Its product space answers three connected questions:

- **Discover** — What is where, and which spatial candidates satisfy the question I am asking?
- **Explore** — What is this place or spatial object, what evidence/information do I have about it, and what can I do with it?
- **Navigate** — How do I get there?

Current Location supports the same space: Orientation can consume and visualize a position supplied by a host while OS/browser permission ownership stays with that host.

## Standalone product workflow

Orientation is independently useful; it is not only a map/geocoder/router for other bounded contexts.

The released v0.5.0 baseline provides the first complete loop:

```text
spatial question / criteria
        ↓
Orientation-generated research prompt
        ↓
external user-controlled research interaction
        ↓
versioned structured JSON
        ↓
strict Orientation validation + import
        ↓
local SQLite discovery collection + provenance
        ↓
map/list exploration and evidence inspection
        ↓
select destination + explicit origin
        ↓
DRIVING / CYCLING / WALKING direct Route
        or
public-transit Journey alternatives with depart-at / arrive-by
```

Public-transit planning is a separate time-dependent `Journey` model rather than another direct-routing `TravelProfile`. MOTIS v2.11.0 is the first Journey adapter; deterministic acceptance uses pinned self-hosted MOTIS plus pinned OSM/GTFS fixtures.

Research results are evidence-backed input, not automatically authoritative truth. Heuristic matches must not silently become asserted sensitive personal characteristics.

## Local-first runtime

Current post-v0.5 hardening on `dev` removes the silent external Photon runtime path.

The intended default topology is:

```text
Browser
  |
  +-- OpenFreeMap hosted basemap  <-- intentional external map resource
  |
  +-- Orientation backend @ 127.0.0.1
          |
          +-- local SQLite
          +-- local MOTIS @ 127.0.0.1:8081
          |      +-- place search / reverse geocoding
          |      +-- public-transit Journey planning
          |
          +-- local Valhalla @ 127.0.0.1:8002
                 +-- DRIVING / CYCLING / WALKING Route planning
```

There is **no automatic hosted provider fallback** for place search, geocoding or mobility. A missing local runtime fails visibly instead of silently forwarding a search, origin, destination or Journey time to a third party.

OpenFreeMap remains intentionally external because the browser can fetch only the map resources needed for the visible viewport instead of shipping a large basemap dataset with Orientation. Offline basemap packaging/caching is a separate later capability.

The standalone browser UI is responsive on narrow/mobile viewports, but this does not imply that large MOTIS/Valhalla datasets should be installed on a phone. Mobile runtime distribution is a separate deployment concern.

## Ownership

Orientation owns:

- personal spatial discovery/research semantics;
- Orientation-owned spatial research prompt/import contracts;
- imported discovery collections, provenance and evidence;
- local persistence for Orientation-owned discovery state;
- map scenes, geometry, features, layers, clustering and hit testing;
- framework-independent map rendering;
- map style/tile/provider integration;
- place/POI discovery, geocoding and reverse geocoding;
- direct routing and public-transit Journey semantics;
- generic current-position/accuracy representation;
- mobility-provider adapters and technical geospatial policy.

Orientation does **not** own Vocation job-market semantics, Illumination learning semantics, WGT product-shell semantics, OS permission policy, Conveyance delivery, foreign authoritative persistence or a generic LLM platform.

## Browser surfaces

The map package intentionally has three separate entries:

1. `app.html` — first-class standalone Orientation product surface;
2. `index.html` — Reference/acceptance/development host;
3. `embed.html` — reusable Embed Host exposing `orientation.host-bridge` 1.0.

The standalone app keeps Research, Collections and Navigate directly reachable. Product controls do not become part of the reusable Map Surface or Host Bridge.

## Repository / runtime shape

```text
orientation/
├── backend/       Java 25 + Maven + Spring Boot + local SQLite persistence
├── map/           TypeScript + MapLibre GL JS; standalone, Reference and Embed hosts
├── contracts/     versioned boundary/import schemas and examples
├── deployment/    local runtime integration such as Valhalla
├── docs/
└── scripts/
```

## Released foundation

- **v0.1.0** — provider-neutral Spatial Scene and Map Surface, rich feature interaction, host-supplied Current Location, `orientation.host-bridge` 1.0, Reference Host and Embed Host.
- **v0.1.1** — OpenFreeMap Liberty street/place basemap.
- **v0.1.2** — explicit MapLibre GL JS 6 Vite worker packaging and vector-source readiness gate.
- **v0.2.0** — Orientation-owned Place model, forward place search and reverse geocoding through the then-current Photon adapter and narrow HTTP endpoints.
- **v0.3.0** — provider-neutral two-point routing, Valhalla 3.8.3 integration, DRIVING/CYCLING/WALKING, route rendering and Reference Host route planning.
- **v0.4.0** — spatial-research contract 1.0, deterministic external-research prompts, strict import, local SQLite discovery persistence, restart/reopen support and the first standalone discovery-to-route workflow.
- **v0.5.0** — provider-neutral public-transit Journey planning, MOTIS v2.11.0 integration, Journey rendering and standalone Journey comparison/selection while preserving the v0.4 discovery and direct-routing baseline.

v0.5.0 does **not** claim shared mobility/GBFS, fares/ticketing, booking, complete realtime coverage, turn-by-turn live navigation, automatic crawling, cross-device synchronization or physical-iPhone support.

See [`docs/16_PRODUCT_DIRECTION.md`](docs/16_PRODUCT_DIRECTION.md) for product direction and [`docs/INDEX.md`](docs/INDEX.md) for the specification set.
