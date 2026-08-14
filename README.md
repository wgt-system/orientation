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

Repository bootstrap baseline. Exact public contracts are intentionally not frozen merely to reserve future integration. Concrete slices must justify them first.

See [`docs/INDEX.md`](docs/INDEX.md).
