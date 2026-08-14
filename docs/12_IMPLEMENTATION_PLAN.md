# Orientation – Implementation Plan

**Status:** Control-plane plan; four v0.1.0 implementation packages completed in the repository. Milestone/release approval remains pending.

Milestone names, when created, use semantic versions only.

## v0.1.0 — Orientation map-surface foundation

Goal: deliver the first reusable Orientation map-surface capability: a stable
provider-neutral Spatial Scene boundary, rich spatial feature interaction,
host-supplied current-location presentation, and an embeddable host bridge
suited to WGT browser/WebView integration.

The repository bootstrap and CI are complete in the published baseline. v0.1.0
therefore contains these four concrete work packages:

1. **Stabilize the Spatial Scene and Map Surface boundary** — evolve the existing
   SpatialScene/SpatialFeature seed with deterministic scene updates, identity and
   validation, viewport intent/fit behavior, renderer lifecycle/error/ready
   semantics, opaque selection events, and clean destroy/recreate behavior.
2. **Implement rich spatial feature interaction** — add generic information,
   SpatialResource and SpatialAction presentation, host-mediated activation events,
   safe URI/text handling, accessibility, and coherent reference-host details.
   Rich external resources are explicitly allowed; no URL-free restriction applies.
3. **Add host-supplied current-location presentation** — accept PositionFix with
   coordinate, accuracy and observed-at data, render/update/remove the location and
   accuracy independently of provider features, and keep permission/history outside
   Orientation core.
4. **Provide the embeddable Orientation host bridge and harden v0.1.0** — define
   the narrow versioned scene-in/event-out bridge required for WGT WebView hosting,
   validate inbound messages, cover lifecycle/reload, package the provider-neutral
   artifact, and complete browser/performance/regression/readiness checks.

Issues #1, #2, #3 and #4 now have implementation and CI evidence on `dev`.
This does not mark milestone `v0.1.0` released or approved.

Dependency order:

```text
Issue 1
├── Issue 2
├── Issue 3
└── Issue 4 (after Issues 1, 2 and 3)
```

v0.1.0 does not add routing, Valhalla, place discovery, geocoding migration,
Vocation contract migration, or foreign-domain semantics. It does not introduce
persistence, a broad network API, React/Avalonia/Vocation dependencies, or a new
generic map microservice.

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
