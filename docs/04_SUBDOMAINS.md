# Orientation – Subdomains

**Status:** Bootstrap baseline

## Discover

Generic discovery of spatial objects:

- geocoding;
- reverse geocoding;
- place/POI search;
- nearby/spatial search when concrete scenarios justify them.

## Explore

Generic spatial presentation and interaction:

- map scenes;
- geometry/features;
- markers, layers and clustering;
- selection/hit testing;
- rich information/resource/action presentation primitives;
- current-position visualization;
- viewport/camera behavior.

## Navigate

Generic path determination and route presentation:

- route requests;
- travel profiles;
- waypoints;
- distance/duration;
- route geometry;
- directions/manoeuvre data where supported;
- rendering route overlays.

## Supporting technical capability

Provider adapters, caching, attribution, limits, failure isolation, observability and performance behavior support the three subdomains but do not become separate business bounded contexts by default.
