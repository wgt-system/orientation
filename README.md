# Orientation

Orientation is the `wgt-system` **local-first personal spatial exploration and mobility bounded context** and the system owner of reusable generic geospatial capabilities.

Its product and capability space answers three connected questions:

- **Discover** — What is where, and which spatial candidates satisfy the question I am asking?
- **Explore** — What is this place or spatial object, what evidence/information do I have about it, and what can I do with it?
- **Navigate** — How do I get there?

Current Location supports the same space: Orientation can consume and visualize a position supplied by a host while OS/browser permission ownership stays with that host.

## Independent product direction

Orientation is not only a map/geocoder/router used by other bounded contexts. It is intended to be independently useful in the same architectural sense as Vocation and Illumination.

A first-class Orientation workflow may be:

```text
spatial question / criteria
        ↓
Orientation-generated research prompt
        ↓
external ChatGPT / research interaction
        ↓
versioned structured JSON
        ↓
Orientation validation + import
        ↓
local Orientation-owned spatial collection + provenance
        ↓
map/list exploration, filtering and comparison
        ↓
route / mobility planning from an explicit or current position
```

The prompt may request facts that ordinary map providers do not expose reliably. Imported claims remain evidence-backed research data; Orientation must not silently turn heuristics into asserted sensitive personal characteristics.

A paid LLM API is not required for this workflow. Domain-specific prompt templates and import contracts remain Orientation-owned. Their existence does not by itself justify a generic prompt/LLM microservice.

## Ownership

Orientation owns:

- personal spatial discovery/research semantics and Orientation-owned imported spatial collections when introduced by a concrete slice;
- provenance/evidence needed to understand imported spatial research;
- map scenes, geometry, features, layers, clustering and hit testing;
- framework-independent map rendering;
- map style/tile/provider integration;
- place/POI discovery;
- geocoding and reverse geocoding;
- routing, route geometry, distance/duration and generic directions;
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

## Independence and product integration

Orientation and Vocation intentionally remain separate bounded contexts. Vocation asks job-market questions and remains authoritative for job-market meaning; Orientation asks spatial-discovery and mobility questions. Vocation may consume Orientation geocoding, rendering or routing without transferring job-domain ownership.

`Wiiii Got This` may provide cross-platform composition/presentation of Orientation capabilities, but Orientation is independently authoritative and may have its own end-user application surface. The current Reference Host is still a development/acceptance surface; that does not prohibit a future standalone Orientation product UI.

Provider-owned rich spatial projections are valid. A marker may expose provider-owned information and external resources without transferring their business meaning to Orientation.

## Repository / runtime shape

One bounded context does not imply one process or one language.

```text
orientation/
├── backend/       Java 25 + Maven + Spring Boot
├── map/           TypeScript + MapLibre GL JS
├── contracts/     explicit versioned boundary/import artifacts when accepted
├── deployment/    external runtime integration such as Valhalla
├── docs/
└── scripts/
```

A future standalone application host and local persistence may be added inside the same bounded context when the concrete product slice requires them. They do not require a new bounded context.

Valhalla is treated as an upstream C++ routing engine behind an Orientation adapter, not as a WGT-owned C++ codebase.

## Released foundation

- **v0.1.0** — provider-neutral Spatial Scene and Map Surface, rich feature interaction, host-supplied Current Location, `orientation.host-bridge` 1.0, Reference Host and embeddable Host.
- **v0.1.1** — usable OpenFreeMap Liberty street/place basemap.
- **v0.1.2** — explicit MapLibre GL JS 6 Vite worker packaging and vector-source readiness gate.
- **v0.2.0** — Orientation-owned Place model, forward place search and reverse geocoding through a replaceable Photon adapter and narrow HTTP endpoints.
- **v0.3.0** — provider-neutral two-point routing, Valhalla 3.8.3 integration, DRIVING/CYCLING/WALKING, generic route rendering and Reference Host route planning.

v0.3.0 is routing foundation, not full mobility navigation. It does not yet include public transit, shared-mobility providers, multimodal/time-aware routing, local Orientation research persistence or the standalone research/import product workflow.

The post-v0.3 product direction is documented in [`docs/16_PRODUCT_DIRECTION.md`](docs/16_PRODUCT_DIRECTION.md).

See [`docs/INDEX.md`](docs/INDEX.md) for the complete specification set.
