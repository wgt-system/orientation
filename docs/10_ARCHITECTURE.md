# Orientation – Architecture

**Status:** v0.4.0 standalone-product work extends the released v0.1-v0.3 geospatial baseline.

## Logical architecture

```text
                         Orientation bounded context
        +----------------------------------------------------------------+
        |                                                                |
        |  Java backend                         TypeScript browser/map     |
        |  +---------------------------+         +----------------------+  |
        |  | domain/application        |         | Standalone App       |  |
        |  | place/routing ports       |         | Reference Host       |  |
        |  | discovery repository port |         | Embed Host           |  |
        |  +------------+--------------+         | reusable Map Surface |  |
        |               |                        +----------+-----------+  |
        |       +-------+--------+                           |              |
        |       | provider       |                           |              |
        |       | adapters       |                           |              |
        |       +-------+--------+                           |              |
        |               |                                    |              |
        |       +-------+--------+                           |              |
        |       | local SQLite   |                           |              |
        |       | discovery DB   |                           |              |
        |       +----------------+                           |              |
        +---------------|------------------------------------|--------------+
                        |                                    |
                external providers/                  browser / WGT host
                Valhalla runtime
```

## Technology baseline

### Backend

- Java 25 LTS
- Maven
- Spring Boot 4.1.x
- framework-independent domain/application layers
- provider interfaces at application boundary
- local SQLite/JDBC adapter for accepted Orientation-owned Discovery Collection persistence

Spring, JDBC, SQLite, Photon and Valhalla remain host/infrastructure concerns rather than domain types.

The place/geocoding and routing provider interactions remain stateless. v0.4 introduces durable local state only for the accepted standalone Discovery Collection capability; see ADR-0007.

### Browser/map runtime

- TypeScript
- MapLibre GL JS 6
- ESM/Vite
- framework-independent reusable map core

The map package intentionally produces three browser entries:

1. `app.html` — first-class standalone Orientation discovery/product surface;
2. `index.html` — Reference/acceptance/development host for isolated generic capabilities;
3. `embed.html` — provider-neutral Embed Host exposing `orientation.host-bridge` 1.0.

The standalone app composes Orientation-owned prompt, discovery, place and routing APIs. It may contain product-specific controls and Discovery Collection presentation, but those semantics do not become part of the reusable Map Surface or Host Bridge.

The Reference Host remains a validation surface for map/place/routing behavior. The Embed Host remains intentionally narrow and contains no standalone-product or provider-domain UI.

OpenFreeMap Liberty remains the default external street basemap. MapLibre is infrastructure; renderer input/output contracts expose Orientation types only.

Do not depend on React/Avalonia/Vocation semantics in the reusable renderer core.

### Place/geocoding

The v0.2 backend exposes provider-neutral place search and reverse geocoding behind Orientation application ports. Photon is the current configurable infrastructure adapter.

Browser hosts call Orientation `/api/v1/places/*` boundaries and never call Photon directly. Current device position is not automatically forwarded to external providers.

### Routing

Valhalla remains the selected upstream routing engine behind the Orientation `RoutingPort` adapter.

The released v0.3 boundary provides two-point DRIVING/CYCLING/WALKING route planning, decoded Orientation route geometry, distance/duration, Map Surface route overlays and explicit route replacement/clear behavior.

v0.4 standalone product composition consumes this boundary unchanged. Public transit, shared mobility and multimodal planning require later explicit domain models rather than new string values in the v0.3 Travel Profile.

### Discovery acquisition and persistence

The v0.4 acquisition path is:

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

The external bundle is not the persistence model. Research evidence, provider-backed Places and Orientation-derived values retain distinct authority/provenance.

SQLite is a local implementation detail. Cross-device delivery is not implied by local persistence and must use the system Conveyance decision model if a real requirement appears.

## Process/deployment model

A bounded context is not a single-process prescription.

Current desktop/browser development and standalone-product topology can be:

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
             +-- place/geocoding provider
             +-- Valhalla runtime
```

WGT may instead embed the reusable Orientation surface and invoke accepted Orientation boundaries through its own product composition. That does not make the standalone app the WGT UI or make WGT authoritative for Orientation state.

Exact local/remote topology remains capability/platform-specific.

## Browser/runtime acceptance

The real Valhalla smoke keeps the generic Reference Host route path and adds the standalone product path.

For standalone acceptance it uses a clean local SQLite database, imports through a real Chrome session, terminates/restarts the backend with the same database, reopens the same collection in a new browser session and requests a real Valhalla route to the imported candidate.

This proves product composition without changing `orientation.host-bridge` 1.0.

## WGT integration gate

Orientation generic map/runtime acceptance for WGT remains distinct from standalone product acceptance:

1. browser/reference/standalone generic runtime evidence;
2. WGT Windows embedded host;
3. physical iPhone embedded host.

The physical iPhone proof is a technology/runtime compatibility gate, not a reason to duplicate renderer ownership permanently.

## Legacy migration targets

- Vocation React Leaflet renderer
- Vocation Nominatim geocoder implementation
- historical WGT Mapsui renderer where not already retired

Consumer migration remains work in the owning consumer repository after the relevant Orientation replacement gates pass.

## Data ownership

No cross-context database access.

Orientation SQLite stores Orientation-owned personal discovery state and required research provenance only. It must not become an authoritative copy of Vocation, Illumination or WGT business state.

Technical provider caches/indexes, if introduced later, remain separate in authority from durable Discovery Collections and require their own lifecycle justification.
