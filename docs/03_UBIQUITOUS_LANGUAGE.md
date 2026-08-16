# Orientation – Ubiquitous Language

**Status:** Bootstrap baseline; contract names are not frozen unless explicitly stated.

- **Coordinate** — geographic longitude/latitude pair validated by Orientation.
- **Position Fix** — host-supplied current position with coordinate, non-negative horizontal accuracy in metres and a UTC ISO timestamp; it is independent of Spatial Features.
- **Spatial Feature** — generic spatially referenced item presented/explored by Orientation. It carries an opaque reference to its source meaning.
- **Source Reference** — opaque provider/source identity carried with a Spatial Feature and preserved in generic selection events.
- **Spatial Scene** — the generic input state for a map surface: features, optional route/location overlays and viewport intent.
- **Viewport Intent** — minimal generic instruction for automatic feature focus/fit or preserving the current viewport; it is not a broad camera contract.
- **Spatial Resource** — an externally addressable resource attached to a spatial feature, with provider-supplied label/metadata. Activation is emitted to the host.
- **Spatial Action** — a host/provider-defined action descriptor attached to a spatial feature. Orientation presents/raises it without owning its business meaning.
- **Spatial Information Section** — a provider-neutral titled group of labelled text rows rendered as feature detail; it is presentation data, not foreign-domain semantics.
- **Resource Activation** — a generic event carrying feature/source/resource references; the host decides whether and how to navigate or execute it.
- **Action Activation** — a generic event carrying feature/source/action references; the host decides its execution semantics.
- **Place** — a provider-backed generic geographic place/POI result.
- **Geocoding** — resolving textual place/address input to geographic candidates.
- **Reverse Geocoding** — resolving a coordinate to provider-backed place/address candidates.
- **Place Search Query** — bounded, explicit text input used to request ordered generic Place candidates.
- **Forward Geocoding** — resolving text input to generic Place candidates; in v0.2.0 this is exposed as Place Search.
- **Place Candidate** — an ordered generic result with coordinate, display label and optional address/category information.
- **Route Request** — an explicitly supplied origin, destination and generic Travel Profile for two-point routing; it has no implicit current-position input.
- **Route** — provider-neutral route output containing origin, destination, profile, bounded decoded Route Geometry, distance in metres and duration in seconds.
- **Route Geometry** — an immutable ordered list of at least two validated Coordinates, bounded to 10,000 points in the initial routing slice.
- **Travel Profile** — one of the generic Orientation modes `DRIVING`, `CYCLING` or `WALKING`; provider-specific costing names are not domain language.
- **Routing Port** — application boundary translating a Route Request to a provider-neutral Route or stable routing failure.
- **No Route Found** — a valid routing outcome distinct from provider unavailability or invalid provider data.
- **Map Surface** — reusable renderer runtime owned by Orientation.
- **Map Surface Lifecycle** — initializing, ready, error and destroyed states of a renderer instance; lifecycle events remain generic and do not expose MapLibre or DOM event objects.
- **Reference Host** — standalone development/debug UI for Orientation; not the WGT product UI.
- **Provider Adapter** — Orientation infrastructure that translates a third-party geospatial API/engine into Orientation application semantics.
- **Host** — a product or reference runtime embedding an Orientation capability.
- **Opaque Provider Reference** — identifier Orientation can round-trip without interpreting foreign business semantics.

Photon field names such as `osm_key` and `osm_value` are provider details, not
Orientation language. The v0.2.0 adapter maps only normalized generic Place
semantics across the application boundary.
