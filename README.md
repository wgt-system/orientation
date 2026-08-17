# Orientation

Orientation is the `wgt-system` bounded context for reusable geospatial capabilities.

Its purpose is to answer three connected classes of questions:

- **Discover** — What is where?
- **Explore** — What is this spatial object and what can I do with it?
- **Navigate** — How do I get there?

Current-location handling is part of the same capability space: Orientation can consume and visualize a position supplied by a host, while OS/browser permission ownership stays with that host.

## Ownership

Orientation owns generic geospatial capability and technical geospatial behavior:

- map scenes, geometry, features, layers, clustering and hit testing;
- framework-independent map rendering;
- map style/tile/provider integration;
- place/POI discovery when introduced by a concrete slice;
- geocoding and reverse geocoding;
- routing, route geometry, distance/duration and generic directions;
- generic current-position/accuracy representation;
- provider adapters, caching, failure handling and technical geospatial policy.

Orientation does **not** own:

- Vocation Work Location, Opportunity, Company, Posting or External Link semantics;
- Illumination learning semantics;
- Wiiii Got This product-shell/navigation/domain-screen semantics;
- OS permission or device-trust semantics;
- Conveyance durable cross-device delivery;
- foreign authoritative persistence.

## Product integration

`Wiiii Got This` remains the primary product composition/presentation context. A domain such as Vocation may consume Orientation when it needs a generic geospatial result that it interprets domain-specifically. WGT may also compose provider-owned domain data with Orientation directly when only product presentation/orchestration is involved.

Provider-owned rich spatial projections are valid. A marker may expose provider-owned information and external resources without transferring their business meaning to Orientation.

## Repository / runtime shape

One bounded context does not imply one process or one language.

```text
orientation/
├── backend/       Java 25 + Maven + Spring Boot
├── map/           TypeScript + MapLibre GL JS
├── contracts/     explicit versioned boundary artifacts when accepted
├── deployment/    external runtime integration such as Valhalla
├── docs/
└── scripts/
```

Valhalla is treated as an upstream C++ routing engine behind an Orientation adapter, not as a WGT-owned C++ codebase.

## Status

Orientation v0.1.0 is the first published Map-Surface baseline: provider-neutral
Spatial Scene and Map Surface, Rich Spatial Feature Interaction, host-supplied
Current Location, `orientation.host-bridge` 1.0, the Reference Host, and the
embeddable browser/WebView Host.

It does not include WGT Windows integration, physical-iPhone proof, Vocation
migration, geocoding, Place Discovery, or Routing/Valhalla.

Orientation v0.1.1 is the released basemap patch baseline dated 2026-08-16.
It replaces the demonstration style with a usable street/place basemap,
preserves visible attribution, and makes Reference Host renderer/basemap
failures visible. Embed bridge/status semantics remain unchanged.

Orientation v0.1.2 is the released runtime patch dated 2026-08-16. It explicitly
bundles and registers the MapLibre GL JS 6 worker for Vite; the `openmaptiles`
vector source must load before renderer readiness, so raster-only relief is not
accepted as a valid map result. The 15-second readiness guard makes silent
worker/vector bootstrap failures diagnosable in Reference and Embed Hosts.

The v0.1.1 basemap patch uses OpenFreeMap Liberty
(`https://tiles.openfreemap.org/styles/liberty`) as the default MapLibre style.
It provides OpenStreetMap/OpenMapTiles-based street and place detail without an
API key. Remote basemap availability is external and best-effort; renderer and
bridge semantics do not depend contractually on this provider.

## v0.2.0

Focus: Geocoding and place search.

Released and accepted on 2026-08-16.

The release adds the Orientation-owned Place model, forward place search,
reverse geocoding, provider-neutral application ports, the configurable Photon
infrastructure adapter, bounded provider responses, stable provider error
mapping and stateless HTTP endpoints:

- `GET /api/v1/places/search`
- `GET /api/v1/places/reverse`

Photon is replaceable backend infrastructure. Jackson 3 is used through the
Spring Boot 4.1 stack; no Jackson-2 Photon parser or raw Photon taxonomy leaks
into Orientation semantics. Provider responses are limited to 1 MiB, with
unknown-length bodies consuming at most one additional byte for oversize
detection. The Reference Host uses only relative `/api` calls through the Vite
dev/preview proxy, supports explicit-submit keyboard-accessible search and
explicit map-center reverse lookup, and never forwards PositionFix
automatically. The release does not include routing, Valhalla, persistence,
Vocation/WGT migration or standalone-product packaging. The
`orientation.host-bridge` 1.0 contract and schemas are unchanged.

See [`docs/INDEX.md`](docs/INDEX.md).

## v0.3.0

Focus: Routing. Released and accepted on 2026-08-17.

The release completes provider-neutral route planning with two-point
`RouteRequest` semantics, DRIVING/CYCLING/WALKING travel profiles, bounded
decoded route geometry, distance/duration, `RoutingPort`, `RoutingService` and
the narrow `POST /api/v1/routes` host endpoint.

Valhalla 3.8.3 is the first routing provider behind the Orientation boundary.
The development/smoke runtime uses the reviewed upstream
`ghcr.io/valhalla/valhalla-scripted:3.8.3` image and a retained Hamburg monthly
OpenStreetMap snapshot. Valhalla costing names, error codes, JSON and encoded
polyline6 geometry remain infrastructure details; decoded Orientation geometry
crosses `RoutingPort`.

The Map Surface renders generic route lines plus origin/destination points with
replace, clear, lifecycle and antimeridian-safe viewport behavior. The Reference
Host provides explicit Start and Destination searches and selection, explicit
profile choice, distance/duration/profile summaries, stale-request protection,
controlled routing failure states and explicit route clearing. Existing Place
Search, Reverse Geocoding, Spatial Scene interaction and host-supplied Current
Position remain available.

Acceptance includes deterministic backend/map CI, real Valhalla routes for all
three profiles, production Reference Host execution in Chrome, and visible
route-render/replacement/clear evidence. The post-merge `dev` CI and complete
Valhalla + browser smoke both pass on the accepted routing commit.

v0.3.0 is route planning/routing, not full live navigation. It does not add
turn-by-turn guidance, live GPS rerouting, persistence, OS/device permission
ownership or foreign-domain semantics. `orientation.host-bridge` remains 1.0.
