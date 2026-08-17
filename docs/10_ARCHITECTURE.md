# Orientation – Architecture

**Status:** v0.4.0 is released. The v0.5.0 public-transit Journey implementation is integrated on `dev` and undergoing final #44 hardening.

## Logical architecture

```text
                         Orientation bounded context
        +----------------------------------------------------------------+
        |                                                                |
        |  Java backend                         TypeScript browser/map     |
        |  +---------------------------+         +----------------------+  |
        |  | domain/application        |         | Standalone App       |  |
        |  | Place / Route / Journey   |         | Reference Host       |  |
        |  | ports                     |         | Embed Host           |  |
        |  | discovery repository port |         | reusable Map Surface |  |
        |  +------------+--------------+         +----------+-----------+  |
        |               |                                   |              |
        |       +-------+----------------+                  |              |
        |       | provider adapters      |                  |              |
        |       | Photon / Valhalla /    |                  |              |
        |       | MOTIS                  |                  |              |
        |       +-------+----------------+                  |              |
        |               |                                   |              |
        |       +-------+--------+                          |              |
        |       | local SQLite   |                          |              |
        |       | discovery DB   |                          |              |
        |       +----------------+                          |              |
        +---------------|-----------------------------------|--------------+
                        |                                   |
                external providers /                 browser / WGT host
                local mobility runtimes
```

## Technology baseline

### Backend

- Java 25 LTS
- Maven
- Spring Boot 4.1.x
- framework-independent domain/application layers
- provider interfaces at application boundaries
- local SQLite/JDBC adapter for accepted Orientation-owned Discovery Collection persistence

Spring, JDBC, SQLite, Photon, Valhalla and MOTIS remain host/infrastructure concerns rather than domain types.

Place/geocoding and mobility-provider interactions are request-scoped/stateless at the application boundary. Durable local state exists only for accepted Orientation-owned discovery collections; see ADR-0007.

### Browser/map runtime

- TypeScript
- MapLibre GL JS 6
- ESM/Vite
- framework-independent reusable map core

The map package produces three browser entries:

1. `app.html` — first-class standalone Orientation product surface;
2. `index.html` — Reference/acceptance/development host for isolated generic capabilities;
3. `embed.html` — provider-neutral Embed Host exposing `orientation.host-bridge` 1.0.

The standalone app composes Orientation-owned prompt, discovery, place, direct Route and public-transit Journey APIs. Product controls do not become part of the reusable Map Surface or Host Bridge.

The Reference Host remains a validation surface for generic map/place/direct-routing behavior. The Embed Host remains intentionally narrow and contains no standalone-product/provider-domain UI.

OpenFreeMap Liberty remains the default external street basemap. MapLibre is infrastructure; renderer input/output contracts expose Orientation types only.

Do not depend on React/Avalonia/Vocation semantics in the reusable renderer core.

## Place/geocoding

The v0.2 boundary exposes provider-neutral place search and reverse geocoding. Photon is the current configurable infrastructure adapter.

Browser hosts call Orientation `/api/v1/places/*` boundaries and never call Photon directly. Current device position is not automatically forwarded to external providers.

## Direct Route

Valhalla remains the selected upstream engine behind `RoutingPort`.

The released v0.3 boundary provides two-point DRIVING/CYCLING/WALKING planning, decoded Orientation Route geometry, distance/duration, Map Surface Route overlays and explicit replace/clear behavior.

This direct Route model remains unchanged in v0.5. Public transit is not another `TravelProfile` value.

## Public-transit Journey

ADR-0008 defines the separate time-dependent Journey boundary.

```text
JourneyRequest
  origin + destination
  DEPART_AT | ARRIVE_BY
  offset-aware time
        |
        v
JourneyService -> JourneyPort
        |
        v
MOTIS v2.11.0 adapter
        |
        v
JourneyPlan
  -> alternatives
  -> ordered WALK/transit legs
  -> scheduled + optional realtime timing
  -> bounded decoded geometry
```

MOTIS provider DTOs, mode enums and error bodies stay inside infrastructure. Shared/rental/ODM/ride-sharing modes are excluded from the accepted v0.5 transit request rather than silently normalized into public transit.

Default configuration targets local MOTIS. A hosted Transitous endpoint may be configured explicitly for reference/manual use, but deterministic acceptance and release evidence use a pinned self-hosted MOTIS runtime and pinned OSM/GTFS fixture.

The reusable Map Surface has a dedicated Journey controller/source/layer set separate from the existing Route overlay. WALK/transit semantics and explicit transit-stop markers do not rely on color alone. The standalone product decides when switching modes should clear Route or Journey presentation; the reusable Map Surface does not silently couple the two states.

## Discovery acquisition and persistence

The v0.4 acquisition path remains:

```text
explicit SpatialResearchQuestion
       |
       v
SpatialResearchPromptService
       |
       v
external manual research / ChatGPT
       |
       v
Spatial Research Bundle 1.0
       |
       v
semantic + strict-shape validation
       |
       v
application ACL / translation
       |
       v
DiscoveryCollection
       |
       v
DiscoveryRepository -> local SQLite
```

The external bundle is not the persistence model. Research evidence, provider-backed Places and Orientation-derived mobility results retain distinct authority/provenance.

SQLite is a local implementation detail. Cross-device delivery is not implied by local persistence and must use the Conveyance decision model if a concrete requirement appears.

## Process/deployment model

A bounded context is not a single-process prescription.

Current standalone development/acceptance can compose:

```text
Browser
  |
  +-- Standalone Orientation App / Reference Host
  |        |
  |        +-- reusable Orientation Map Surface
  |
  +-- Orientation Java backend
             |
             +-- local SQLite discovery database
             +-- Photon-compatible Place provider
             +-- Valhalla direct-routing runtime
             +-- MOTIS public-transit runtime
```

WGT may instead embed the reusable Orientation surface and invoke accepted Orientation boundaries through its own product composition. That does not make the standalone app the WGT UI or make WGT authoritative for Orientation state.

Exact local/remote topology remains capability/platform-specific.

## Browser/runtime acceptance

Two complementary real runtime gates protect v0.5:

1. the existing Valhalla/Photon/Chrome smoke retains direct DRIVING/CYCLING/WALKING, Reference Host and v0.4 standalone import → restart → reopen → direct-route evidence;
2. the MOTIS Journey smoke imports pinned Aachen OSM/GTFS into pinned MOTIS, calls the Journey boundary, then drives the production standalone app in Chrome through discovery destination selection, explicit origin, public-transit planning, Journey rendering, mode switching and clear behavior.

Neither path changes `orientation.host-bridge` 1.0.

## WGT integration gate

Orientation generic capability acceptance remains distinct from WGT product/platform validation. Current Orientation releases do not claim physical-iPhone support merely because the browser/map artifacts exist.

## Legacy consumer migration targets

- Vocation React Leaflet renderer
- Vocation Nominatim geocoder implementation
- historical WGT generic map implementations where not already retired

Consumer migration remains work in the owning consumer repository after the relevant Orientation replacement gates pass.

## Data ownership

No cross-context database access.

Orientation SQLite stores Orientation-owned personal discovery state and required research provenance only. It must not become an authoritative copy of Vocation, Illumination or WGT business state.

Technical provider caches/indexes, if introduced later, remain separate in authority from durable Discovery Collections and require their own lifecycle justification.
