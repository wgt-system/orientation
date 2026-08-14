# Orientation – Implementation Plan

**Status:** Initial control-plane plan

Milestone names, when created, use semantic versions only.

## v0.1.0 — Orientation foundation

Goal: establish the bounded context and prove the reusable map-surface architecture.

Slices:

1. repository bootstrap and CI;
2. generic scene/feature/resource/action model;
3. MapLibre renderer lifecycle;
4. rich marker/reference-host interaction;
5. host-supplied current-position overlay;
6. browser/reference-host acceptance;
7. WGT Windows embedding spike;
8. physical-iPhone embedding spike.

Do not delete legacy Vocation/WGT renderers before replacement gates pass.

## v0.2.0 — Vocation geospatial migration

Candidate scope after v0.1.0 review:

- Orientation geocoding boundary/provider adapter;
- Vocation adapter to Orientation geocoding;
- Vocation rich Published Map Projection successor;
- remove URL-free design as a permanent constraint;
- eliminate per-opportunity external-link fetching for map composition where the successor projection makes it unnecessary;
- migrate Vocation reference UI from Leaflet to Orientation map surface;
- retire Vocation generic Nominatim implementation after parity.

Vocation remains authoritative for Work Location/Precision and all job-market semantics.

## v0.3.0 — WGT map migration

- integrate Orientation map surface into WGT product map capability;
- preserve WGT shell/navigation/platform ownership;
- pass Windows and physical-iPhone gates;
- retire Mapsui generic renderer once no accepted path requires it.

## v0.4.0 — Navigate

- Valhalla deployment/adapter;
- generic route request/result;
- route overlay;
- distance/duration;
- current-location -> destination scenario;
- failure/timeout/provider tests.

## v0.5.0 — Discover

- provider decision for place/POI discovery and geocoding as needed;
- place search/nearby;
- reverse geocoding;
- provider attribution/rate/caching behavior.

## Sequencing rule

Do not create speculative contracts for later milestones in earlier milestones. Stabilize the smallest consumed boundary first.
