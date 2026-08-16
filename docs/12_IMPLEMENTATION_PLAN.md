# Orientation – Implementation Plan

**Status:** Control-plane plan; v0.1.0, v0.1.1 and v0.1.2 released. v0.1.2 was released on 2026-08-16.

Milestone names, when created, use semantic versions only.

## v0.1.0

Focus: Orientation map-surface foundation. Released.

Goal: deliver the first reusable Orientation map-surface capability: a stable
provider-neutral Spatial Scene boundary, rich spatial feature interaction,
host-supplied current-location presentation, and an embeddable host bridge
suited to WGT browser/WebView integration.

The repository bootstrap and CI are complete in the published baseline. v0.1.0
therefore contains these four concrete work packages:

1. **Stabilize the Spatial Scene and Map Surface boundary** — evolve the existing
   SpatialScene/SpatialFeature seed with deterministic scene updates, identity and
   validation, viewport intent/fit behavior, renderer lifecycle/error/ready
   semantics, opaque selection events, and clean destroy/recreate behavior.
2. **Implement rich spatial feature interaction** — add generic information,
   SpatialResource and SpatialAction presentation, host-mediated activation events,
   safe URI/text handling, accessibility, and coherent reference-host details.
   Rich external resources are explicitly allowed; no URL-free restriction applies.
3. **Add host-supplied current-location presentation** — accept PositionFix with
   coordinate, accuracy and observed-at data, render/update/remove the location and
   accuracy independently of provider features, and keep permission/history outside
   Orientation core.
4. **Provide the embeddable Orientation host bridge and harden v0.1.0** — define
   the narrow versioned scene-in/event-out bridge required for WGT WebView hosting,
   validate inbound messages, cover lifecycle/reload, package the provider-neutral
   artifact, and complete browser/performance/regression/readiness checks.

Issues #1, #2, #3 and #4 have implementation and CI evidence on `dev`.

Dependency order:

```text
Issue 1
├── Issue 2
├── Issue 3
└── Issue 4 (after Issues 1, 2 and 3)
```

v0.1.0 does not add routing, Valhalla, place discovery, geocoding migration,
Vocation contract migration, or foreign-domain semantics. It does not introduce
persistence, a broad network API, React/Avalonia/Vocation dependencies, or a new
generic map microservice.

Do not delete legacy Vocation/WGT renderers before replacement gates pass.

## v0.1.1

Focus: Basemap patch. Released on 2026-08-16.

This patch replaces the MapLibre demonstration style with the default OpenFreeMap
Liberty street basemap for the Reference and Embed Hosts. It keeps the accepted
v0.1.0 Spatial Scene, Current Location, viewport and `orientation.host-bridge`
1.0 semantics unchanged, and makes Reference Host basemap failures visible.
The patch includes OpenFreeMap Liberty as the default basemap, real
street/place rendering instead of the MapLibre demonstration style, visible
attribution, Reference Host basemap/renderer failure status, and unchanged
Embed Host bridge/status semantics. Geocoding, Place Discovery, Routing/
Valhalla and standalone UI redesign remain future work.

`orientation.host-bridge` 1.0 and all schemas remain unchanged.

## v0.1.2

Focus: MapLibre GL JS 6 Vite worker runtime patch. Released on 2026-08-16.

Bundle and register the MapLibre worker explicitly with Vite so OpenFreeMap
vector tiles render reliably in the Reference and Embed Hosts. The released
behavior includes the explicit Vite worker asset, `setWorkerUrl(...)` before
Map creation, an `openmaptiles`-loaded renderer readiness gate, and a
15-second guard against silent worker/vector bootstrap failure. Raster-only
relief can appear superficially valid while vector-tile processing is
unavailable, so acceptance requires the `openmaptiles` source to reach its
loaded state in both development and production-build browser runs. The
OpenFreeMap Liberty selection and all v0.1.1 contracts remain unchanged.

`orientation.host-bridge` 1.0, all schemas, Spatial Scene, Current Location,
Viewport and Resource/Action semantics remain unchanged. Geocoding, Place
Search/Discovery, Routing/Valhalla, standalone-app redesign, WGT integration
and physical-iPhone evidence remain unimplemented.

## v0.2.0

Focus: Geocoding and place search.

Orientation-owned generic place capability:

```text
text query -> Orientation Place Search -> provider-backed Place candidates
           -> coordinate/place information -> later map focus/presentation

coordinate -> Orientation Reverse Geocoding -> generic Place result
```

Package #7 delivers the provider-neutral backend boundary, application use
cases, Photon adapter and narrow HTTP endpoints. It remains stateless and does
not modify the map UI. Package #8 is the later Reference Host integration and
is intentionally not started here. Vocation migration is consumer work after
this Orientation capability is reviewed.

The default development/reference provider is the configurable external Photon
endpoint `https://photon.komoot.io`; it is not part of Orientation semantics.

Geocoding and place-search requests leave the local process only when explicitly
submitted. PositionFix and current device location are not forwarded
automatically. Routing/Valhalla, standalone product packaging and persistence
remain later work.

Future consumer work after v0.2.0 review:

- Vocation adapter to Orientation geocoding;
- Vocation rich Published Map Projection successor;
- remove URL-free design as a permanent constraint;
- eliminate per-opportunity external-link fetching for map composition where the successor projection makes it unnecessary;
- migrate Vocation reference UI from Leaflet to Orientation map surface;
- retire Vocation generic Nominatim implementation after parity.

Vocation remains authoritative for Work Location/Precision and all job-market semantics.

## v0.3.0

Focus: WGT map migration.

- integrate Orientation map surface into WGT product map capability;
- preserve WGT shell/navigation/platform ownership;
- pass Windows and physical-iPhone gates;
- retire Mapsui generic renderer once no accepted path requires it.

## v0.4.0

Focus: Navigate.

- Valhalla deployment/adapter;
- generic route request/result;
- route overlay;
- distance/duration;
- current-location -> destination scenario;
- failure/timeout/provider tests.

## v0.5.0

Focus: Discover.

- provider decision for place/POI discovery and geocoding as needed;
- place search/nearby;
- reverse geocoding;
- provider attribution/rate/caching behavior.

## Sequencing rule

Do not create speculative contracts for later milestones in earlier milestones. Stabilize the smallest consumed boundary first.
