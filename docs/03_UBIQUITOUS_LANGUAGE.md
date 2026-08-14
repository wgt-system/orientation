# Orientation – Ubiquitous Language

**Status:** Bootstrap baseline; contract names are not frozen unless explicitly stated.

- **Coordinate** — geographic longitude/latitude pair validated by Orientation.
- **Position Fix** — host-supplied current position with accuracy and timestamp; optionally heading/speed when supplied.
- **Spatial Feature** — generic spatially referenced item presented/explored by Orientation. It carries an opaque reference to its source meaning.
- **Source Reference** — opaque provider/source identity carried with a Spatial Feature and preserved in generic selection events.
- **Spatial Scene** — the generic input state for a map surface: features, optional route/location overlays and viewport intent.
- **Viewport Intent** — minimal generic instruction for automatic feature focus/fit or preserving the current viewport; it is not a broad camera contract.
- **Spatial Resource** — an externally addressable resource attached to a spatial feature, with provider-supplied label/metadata. Activation is emitted to the host.
- **Spatial Action** — a host/provider-defined action descriptor attached to a spatial feature. Orientation presents/raises it without owning its business meaning.
- **Place** — a provider-backed generic geographic place/POI result.
- **Geocoding** — resolving textual place/address input to geographic candidates.
- **Reverse Geocoding** — resolving a coordinate to provider-backed place/address candidates.
- **Route Request** — origin/destination/waypoints plus a generic travel profile and routing options.
- **Route Result** — route geometry plus generic distance/duration and directions data.
- **Travel Profile** — generic routing mode such as pedestrian, bicycle or car where supported.
- **Map Surface** — reusable renderer runtime owned by Orientation.
- **Map Surface Lifecycle** — initializing, ready, error and destroyed states of a renderer instance; lifecycle events remain generic and do not expose MapLibre or DOM event objects.
- **Reference Host** — standalone development/debug UI for Orientation; not the WGT product UI.
- **Provider Adapter** — Orientation infrastructure that translates a third-party geospatial API/engine into Orientation application semantics.
- **Host** — a product or reference runtime embedding an Orientation capability.
- **Opaque Provider Reference** — identifier Orientation can round-trip without interpreting foreign business semantics.
