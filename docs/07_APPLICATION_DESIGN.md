# Orientation – Application Design

**Status:** v0.4.0 standalone discovery work extends the released v0.1-v0.3 application baseline.

## Backend dependency direction

```text
domain
  ^
  |
application
  ^
  |
adapters / infrastructure
  ^
  |
host / HTTP
```

Domain code is framework-independent.

Application services express use cases and define ports for:

- geocoding;
- reverse geocoding;
- place search;
- routing;
- discovery persistence;
- technical cache/index providers where later justified.

The v0.2.0 backend slice adds `PlaceSearchPort` and
`ReverseGeocodingPort`. `PlaceSearchService` and
`ReverseGeocodingService` expose generic Place semantics while the Photon
adapter remains infrastructure. Place-provider interaction itself remains stateless;
Photon base URL and timeouts are trusted configuration. The Reference Host consumes
this API only through relative same-origin `/api` URLs; Vite dev and preview proxies
target the local backend and are host configuration, not browser provider settings.
Search is explicit-submit only. Reverse lookup is an explicit map-center
action and exposes only the generic `Coordinate` value from the map surface.

The narrow place host API is:

- `GET /api/v1/places/search?q=...&limit=...&lang=...` with optional explicit
  `biasLat`, `biasLon` and `biasZoom`;
- `GET /api/v1/places/reverse?lat=...&lon=...&lang=...`.

Responses contain Orientation DTOs, never Photon GeoJSON or provider DTOs.
Provider failures map to stable HTTP outcomes: invalid input `400`, rate limit
`429`, unavailable/timeout `503`, invalid upstream response `502`.

Provider adapters implement those ports.

## Routing boundary (v0.3.0)

`RoutingPort` and `RoutingService` keep Valhalla/provider details outside Orientation domain/application output. The host endpoint is `POST /api/v1/routes` and accepts only explicit origin, destination and `DRIVING`/`CYCLING`/`WALKING`.

v0.3.0 is route planning/routing, not full live navigation. No current
PositionFix is automatically read or forwarded, and no route is persisted.

The Reference Host does not call Photon or Valhalla directly. It validates Orientation HTTP DTOs and protects request lifecycle state from stale responses.

## Spatial research prompt boundary (v0.4.0)

`SpatialResearchQuestion` represents only explicit user research input required by Spatial Research Bundle 1.0. `SpatialResearchPromptService` deterministically turns that question into an exportable external-research prompt.

`POST /api/v1/research/prompts` returns contract/version/schema identity plus the generated prompt string. It performs no LLM call, crawling, clipboard access or persistence.

## Discovery import and persistence boundary (v0.4.0)

The accepted stateful flow is:

```text
external JSON
  -> SpatialResearchBundleValidator + strict shape guard
  -> SpatialResearchBundleTranslator
  -> DiscoveryCollection
  -> DiscoveryRepository
  -> SQLite adapter
```

`DiscoveryImportService` never writes before complete contract/semantic validation. The translator is the acquisition ACL: external JSON nodes do not become persisted entities directly.

`DiscoveryRepository.storeIfAbsent` is the atomic persistence boundary. The SQLite adapter writes one complete new collection in one transaction. The canonical bundle fingerprint is unique; an already-imported bundle returns the existing collection as `UNCHANGED`.

The first read/import host API is:

- `POST /api/v1/discovery/imports`;
- `GET /api/v1/discovery/collections`;
- `GET /api/v1/discovery/collections/{collectionId}`.

Host DTOs expose Orientation read semantics, not SQL rows, JDBC types or raw external Research Bundle storage.

SQLite is an infrastructure detail selected by ADR-0007. Schema changes use explicit Orientation-owned SQL migrations. Cross-device synchronization is not implied by local persistence.

## Map surface

The TypeScript map surface is a separate runtime artifact inside the same bounded context.

The default reference/embed basemap is OpenFreeMap Liberty
(`https://tiles.openfreemap.org/styles/liberty`), an external
OpenStreetMap/OpenMapTiles-based style. Availability is best-effort and does not
change the provider-neutral renderer or bridge contract.

Conceptual boundary:

```text
SpatialScene in
    |
    v
Orientation Map Surface
    |
    +--> rendered map
    |
    +--> generic interaction events out
```

The public surface must not expose MapLibre classes as contract types.

The renderer uses immutable/read-oriented scene snapshots internally. Replacing a scene validates and replaces the complete feature set; it does not merge hidden foreign state. Generic selection emits opaque `featureRef` and `sourceRef` values, while renderer lifecycle emits only generic initializing/ready/error/destroyed status values.

Viewport handling is deliberately limited to automatic empty/focus/fit behavior or preserving the current viewport. This is a reusable renderer boundary, not a frozen network, Published Contract, or WGT WebView bridge protocol.

Generic events may include:

- feature selected;
- spatial resource activated;
- spatial action activated;
- viewport changed;
- map error/ready.

## Rich information

Orientation may render generic information/resource/action structures because exploration is part of its geospatial capability.

The source of provider-supplied structures retains semantic authority. For example, Vocation can say "this resource is the preferred job posting." Orientation can render a button/entry, but it does not decide which posting is preferred.

Orientation's own Discovery Collection candidate details are different: they are Orientation-owned read state whose individual claims retain external-research provenance and claim basis.

## External-resource execution

Prefer emitting activation events to the host rather than unconditionally navigating from core renderer code.

The reusable surface does not call browser navigation APIs or fetch resource URIs. The reference host demonstrates activation by displaying the received opaque identity.

Automatic multi-feature viewport fitting uses a minimal-span longitude interval; its internal resolved bounds may use an unwrapped longitude above 180° across the antimeridian, while public Coordinate input remains canonical in `[-180, 180]`.

## Current location

Platform/browser permission and acquisition are host concerns.

The host supplies PositionFix data to Orientation. Orientation owns generic validation/use/visualization of the supplied fix.

The reusable map surface exposes `setCurrentPosition`, `clearCurrentPosition` and `currentPosition` independently from `setScene`. It renders a point plus a geographic accuracy polygon and preserves the user's viewport on updates; it does not follow or recenter automatically. No history is retained.

## Failure model

Provider unavailability, invalid provider payloads, rate limiting and timeout are explicit application outcomes. Do not convert them into foreign-domain states such as "job unavailable."

Research import rejection is likewise explicit and occurs before persistence. Database/infrastructure failure is not converted into a false `REJECTED` research result; it remains an infrastructure failure so the caller does not confuse invalid external data with local storage failure.

## Persistence rule

Persistence is introduced only for a concrete Orientation-owned stateful capability. The accepted v0.4 Discovery Collection workflow satisfies that gate.

This does not authorize generic storage of foreign-domain state, provider caches, location history or arbitrary external JSON. Each later persistent concern still requires its own authority/lifecycle justification.
