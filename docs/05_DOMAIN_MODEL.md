# Orientation – Domain Model

**Status:** v0.4.0 discovery model extends the released v0.1-v0.3 geospatial baseline; no Published Contract is frozen by this document.

## Value objects

### Coordinate

- longitude in `[-180, 180]`
- latitude in `[-90, 90]`

### Place

A provider-backed generic geographic place result contains:

- opaque provider reference;
- deterministic display label;
- validated Coordinate;
- optional bounding extent;
- optional normalized generic kind;
- optional address components such as name, street, house number, postcode,
  city, county, state, country and country code.

Missing provider fields remain absent. Orientation does not define a universal
postal-address ontology or claim that a reverse result proves building-level
address accuracy.

### PlaceSearchQuery and ReverseGeocodeQuery

Place search trims nonblank text and uses a bounded result limit (default 5,
hard maximum 10), optional language and explicit location bias. Reverse
geocoding accepts a validated Coordinate and optional language. Neither query
automatically includes PositionFix or current device location.

### PositionFix

- coordinate
- non-negative horizontal accuracy in metres
- `observedAt` as a serialization-friendly UTC ISO timestamp

PositionFix is validated, immutable when accepted by the map surface and ephemeral by default. Host acquisition and permission remain outside Orientation. Heading/speed are not part of this slice.

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

Current Location is not a Spatial Feature and is supplied/cleared through an independent map-surface API. Replacing or clearing a Spatial Scene does not replace or clear the current PositionFix.

### RouteRequest / Route / RouteGeometry

`RouteRequest` is deliberately small in v0.3.0: origin Coordinate, destination
Coordinate and Travel Profile. It is two-point routing only; it has no
waypoints, optimization, traffic, toll, transit, date/time or provider costing
options.

`Route` contains the request endpoints and profile, an Orientation-owned
decoded `RouteGeometry`, finite non-negative distance metres and finite
non-negative duration seconds. `RouteGeometry` is an immutable ordered list
of validated Coordinates with at least two and at most 10,000 points. An
encoded provider polyline and provider identifiers are not domain requirements.

The released v0.3.0 implementation provides the provider-neutral model plus Valhalla-backed DRIVING/CYCLING/WALKING route planning and rendering.

### SpatialResearchQuestion

The v0.4 standalone discovery flow introduces an explicit spatial research question:

- local question reference;
- user-supplied question text;
- explicit radial area center and optional supplied Coordinate;
- radius;
- ordered Criteria with `EVIDENCE_REQUIRED` or `HEURISTIC` evaluation mode.

The question is Orientation-owned input. Device position is not silently acquired or inserted.

### DiscoveryCollection

A Discovery Collection is durable Orientation-owned state created from one accepted Spatial Research Bundle import.

It contains:

- Orientation collection identity and deterministic import fingerprint;
- research timestamp and question/area snapshot;
- ordered Criteria;
- external research Sources with retrieval timestamps;
- researched Candidates;
- optional strong identity hints (canonical HTTPS URI and/or explicit provider identifier);
- Researched Location evidence;
- one Claim per Criterion, including status, basis, optional typed observed value, note and source references.

A Discovery Collection is not raw imported JSON. The external bundle is validated and translated before the collection exists.

`ResearchedLocation` is not the same type or authority as provider-backed `Place`. Research evidence may later be reconciled/geocoded explicitly without erasing that distinction.

A `HEURISTIC` claim records only a match against the user-defined heuristic. It does not create an asserted sensitive-trait fact from a proxy such as a name or language pattern.

Re-import of the same canonical bundle reuses the existing collection. Changed research creates a new collection in the initial v0.4 baseline; no cross-collection fuzzy merge is implied.

## Authority

Orientation is authoritative for its own generic geospatial interpretation, provider-adapter behavior and Orientation-owned personal discovery state.

It is not authoritative for:

- Vocation Work Location precision or job-market decisions;
- another provider's business fields;
- OS permission decisions;
- external map/place datasets;
- user learning/job state;
- truth of an external researched claim merely because the claim was imported.

## Persistence

The accepted v0.4 standalone discovery requirement justifies the first general Orientation database.

Discovery Collections and their provenance are stored locally in SQLite behind `DiscoveryRepository`; see ADR-0007. The database stores Orientation-owned state only and must not become a copy of another bounded context's authoritative data.

Technical provider caches, if introduced later, remain separate in authority from imported research and durable personal discovery state.
