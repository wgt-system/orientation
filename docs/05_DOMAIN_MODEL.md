# Orientation – Domain Model

**Status:** Conceptual bootstrap model; no Published Contract is frozen by this document.

## Value objects

### Coordinate

- longitude in `[-180, 180]`
- latitude in `[-90, 90]`

### PositionFix

- coordinate
- accuracy in metres
- observed-at timestamp
- optional heading
- optional speed

PositionFix is ephemeral by default.

### SpatialFeatureRef

Opaque stable reference within a scene/source boundary.

### SpatialFeature

Conceptually contains:

- feature reference;
- source/provider reference;
- geometry or coordinate;
- display summary;
- optional information sections;
- optional Spatial Resources;
- optional Spatial Actions;
- generic visual hints that do not encode a foreign business domain.

It must not require copying a foreign aggregate/entity object.

### SpatialResource

Conceptually contains:

- resource reference;
- label;
- URI/target descriptor where allowed;
- optional technical/display metadata.

Orientation may validate safe technical shape. The source bounded context remains authoritative for why the resource exists and what it means.

### SpatialAction

Conceptually contains:

- action reference;
- label;
- optional presentation hint.

Activation produces an event. The host/provider adapter owns execution semantics.

### SpatialScene

Conceptually contains:

- features;
- optional current position;
- optional route overlays;
- viewport intent;
- generic map configuration.

The scene is input/read state, not shared domain authority.

### RouteRequest / RouteResult

RouteRequest contains generic geographic routing intent. RouteResult contains generic path output.

The exact versioned transport/API shapes are introduced only with implementation slices.

## Authority

Orientation is authoritative for its own generic geospatial interpretation, provider-adapter behavior and technical caches if introduced.

It is not authoritative for:

- Vocation Work Location precision or job-market decisions;
- another provider's business fields;
- OS permission decisions;
- external map/place datasets;
- user learning/job state.

## Persistence

No general Orientation database is selected at bootstrap. Persistence must be justified by a concrete capability (for example a cache/index/import pipeline) and must not become a copy of foreign domain state.
