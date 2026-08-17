# Orientation – Read Models

**Status:** v0.4.0 standalone discovery extends the released geospatial read-model baseline.

## SpatialSceneView

Consumer-facing renderer input:

- features;
- optional current-position overlay;
- optional routes;
- viewport intent.

## SpatialFeatureView

Generic map/exploration view:

- opaque feature/source refs;
- geometry/coordinate;
- summary/title/subtitle;
- information sections;
- resources;
- actions;
- visual hints.

The reusable map surface preserves each feature's opaque `ref` and `sourceRef` in selection events. Scene replacement is deterministic and viewport intent remains generic automatic/preserve behavior.

Rich content is provider-neutral presentation data; foreign domain meaning remains with the supplying context.

## DiscoveryCollectionSummaryView

The standalone v0.4 collection list reads a small Orientation-owned summary:

- collection id;
- researched-at timestamp;
- question ref/text;
- area-center label;
- radius;
- candidate count.

This list is derived from Orientation persistence and contains no raw external bundle or SQL/JDBC representation.

## DiscoveryCollectionDetailView

Opening a persisted collection exposes the Orientation read state required for exploration:

- question and area snapshot;
- ordered research criteria and evaluation modes;
- ordered sources/provenance;
- candidates;
- optional strong identity hints;
- researched location and its source refs;
- ordered claims with status/basis, optional typed observed value, note and source refs.

The detail view preserves the distinction between externally researched location evidence and provider-backed Orientation Place results.

The standalone map adapts only candidates with an explicit researched coordinate into generic `SpatialFeatureView` input. Candidates without coordinates remain visible in list/detail views and are not silently geocoded by presentation code.

## RouteView

- decoded route geometry;
- total distance;
- total duration;
- generic travel profile;
- provider/engine facts only where required by diagnostics/attribution.

The standalone app uses the selected mapped Discovery Candidate as destination and an explicitly searched Orientation Place as origin. Route state is transient and does not mutate Discovery Collection state.

## PlaceSearchResultView

Provider-neutral place result used by explicit place search, including the standalone route-origin selector.

## GeocodingCandidateView

Provider-neutral geographic candidate. Consuming domains remain responsible for their own precision/acceptance meaning.

## PositionOverlayView

- coordinate;
- accuracy;
- observed-at;
- optional future heading/speed.

The current implementation accepts a serializable `PositionFix` with coordinate, non-negative accuracy in metres and UTC ISO `observedAt`; heading/speed are not implemented. Current Location is independent from provider features, and no location history is implied.
