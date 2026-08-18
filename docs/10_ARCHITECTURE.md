# Orientation – Architecture

**Status:** v0.5.0 released; post-v0.5 local-first/runtime-usability hardening active on `dev`.

## Logical architecture

```text
                         Orientation bounded context
        +----------------------------------------------------------------+
        |                                                                |
        |  Java backend                         TypeScript browser/map     |
        |  +---------------------------+         +----------------------+  |
        |  | domain/application        |         | Standalone App       |  |
        |  | Place / Route / Journey   |         | Reference Host       |  |
        |  | discovery repository port |         | Embed Host           |  |
        |  +------------+--------------+         | reusable Map Surface |  |
        |               |                        +----------+-----------+  |
        |       +-------+----------------+                  |              |
        |       | provider adapters      |                  |              |
        |       | MOTIS / Valhalla       |                  |              |
        |       +-------+----------------+                  |              |
        |               |                                   |              |
        |       +-------+--------+                          |              |
        |       | local SQLite   |                          |              |
        |       +----------------+                          |              |
        +---------------|-----------------------------------|--------------+
                        |                                   |
             local mobility/geodata                  browser / WGT host
                   runtimes                                |
                                                         external
                                                      OpenFreeMap only
```

## Backend

- Java 25 LTS, Maven, Spring Boot 4.1.x;
- framework-independent domain/application layers;
- provider interfaces at application boundaries;
- local SQLite/JDBC persistence for Orientation-owned Discovery Collections.

The default backend bind address is `127.0.0.1`. Provider URLs are trusted application configuration, not request parameters.

### Place search and reverse geocoding

The provider-neutral v0.2 Place boundaries remain unchanged. The current infrastructure adapter uses the same local MOTIS runtime as Journey planning:

- `GET /api/v1/geocode` for forward search;
- `GET /api/v1/reverse-geocode` for reverse geocoding.

Photon is historical v0.2 infrastructure and is no longer part of the current runtime. There is no automatic hosted fallback. Browser hosts call Orientation `/api/v1/places/*` only.

### Direct Route

Valhalla remains behind `RoutingPort` for exactly:

- DRIVING;
- CYCLING;
- WALKING.

The default Valhalla endpoint is local `127.0.0.1:8002`.

### Public-transit Journey

ADR-0008 defines a separate time-dependent Journey boundary:

```text
JourneyRequest
  origin + destination
  DEPART_AT | ARRIVE_BY
  offset-aware time
        |
        v
JourneyService -> JourneyPort -> local MOTIS
        |
        v
JourneyPlan
  alternatives
  ordered WALK/transit legs
  scheduled + optional realtime timing
  bounded decoded geometry
```

MOTIS provider DTOs/modes/errors remain infrastructure details. Shared/rental/ODM/ride-sharing modes are outside the accepted v0.5 Journey slice.

The default MOTIS endpoint is local `127.0.0.1:8081`. No public Transitous or other hosted service is used as an automatic fallback.

## Browser/map runtime

- TypeScript;
- MapLibre GL JS 6;
- ESM/Vite;
- framework-independent reusable map core.

Entries:

1. `app.html` — standalone Orientation product;
2. `index.html` — Reference/acceptance host;
3. `embed.html` — Embed Host with `orientation.host-bridge` 1.0.

The standalone app composes prompt, discovery, place, direct Route and Journey APIs. Product controls remain outside the reusable Map Surface and Host Bridge.

OpenFreeMap Liberty is the intentional external basemap. MapLibre requests hosted style/tile resources for the visible map; no other semantic provider should be contacted silently by the default runtime.

Desktop uses independently scrollable Research, Collections and map/navigation workspace columns. Narrow/mobile layouts return to normal document scrolling with direct jump navigation to Research, Collections and Navigate.

## Local-first runtime topology

```text
Browser
  +-- OpenFreeMap (external basemap)
  |
  +-- Orientation @ 127.0.0.1
          +-- SQLite
          +-- MOTIS @ 127.0.0.1:8081
          |      +-- geocode/reverse
          |      +-- public transit
          +-- Valhalla @ 127.0.0.1:8002
                 +-- direct Route
```

Search text, route origin/destination and Journey time stay on the local semantic-provider path by default. A missing local runtime produces an explicit provider failure; it does not trigger external forwarding.

This topology does **not** prescribe putting full MOTIS/Valhalla datasets on a phone. Mobile runtime distribution, regional extracts and offline capability require separate deployment decisions.

## Discovery acquisition and persistence

```text
SpatialResearchQuestion
       -> Orientation prompt
       -> explicit external user-controlled research
       -> Spatial Research Bundle 1.0
       -> strict validation / ACL translation
       -> DiscoveryCollection
       -> local SQLite
```

The external research interaction is initiated by the user; imported research remains evidence/provenance, not authoritative provider truth. SQLite never becomes a copy of another bounded context's database.

## Runtime acceptance

Two complementary gates protect current behavior:

1. Valhalla/Chrome: direct DRIVING/CYCLING/WALKING plus v0.4 import → restart → reopen → direct-route regression, using a local deterministic MOTIS-compatible geocode stub;
2. MOTIS/Chrome: pinned self-hosted MOTIS + pinned Aachen OSM/GTFS, real MOTIS-backed place search, real Journey planning and production standalone browser flow.

Neither path changes `orientation.host-bridge` 1.0.

## Data ownership

No cross-context database access. Orientation persists Orientation-owned discovery state and research provenance only. Provider indexes/caches remain technical infrastructure and never become authoritative foreign-domain state.
