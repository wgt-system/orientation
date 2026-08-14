# Orientation – Read Models

**Status:** Conceptual bootstrap set.

## SpatialSceneView

Consumer-facing renderer input:

- scene reference/version as needed;
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

It intentionally permits rich content without imposing Vocation/Illumination semantics.

## RouteView

- route reference;
- geometry;
- total distance;
- total duration;
- optional segment/manoeuvre data;
- provider/engine facts required for diagnostics/attribution.

## PlaceSearchResultView

Introduced only when place discovery is implemented.

## GeocodingCandidateView

Introduced with the geocoding slice; must expose enough provider result information for the consuming domain to make its own acceptance/precision decision.

## PositionOverlayView

- coordinate;
- accuracy;
- observed-at;
- optional heading/speed.

No location history is implied.
