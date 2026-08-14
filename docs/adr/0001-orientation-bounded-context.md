# ADR-0001: Orientation is the geospatial bounded context

- **Status:** Accepted
- **Date:** 2026-08-14

## Context

WGT-system has duplicate generic map/geocoding implementation and concrete requirements spanning map exploration, geocoding/current location and routing.

These capabilities form a coherent spatial language and have legitimate consumers outside any single business domain.

## Decision

Create Orientation as the bounded context for generic geospatial capability.

The system-level ownership decision is mirrored by `wgt-system/architecture` ADR-0003.

Orientation is organized around Discover, Explore and Navigate, with Current Location as a supporting geospatial capability.

Orientation does not own foreign business semantics merely because it renders or routes to foreign spatial objects.

## Consequences

- Vocation may consume Orientation geocoding while retaining Work Location/Precision authority.
- WGT may compose provider-owned data with Orientation for product presentation.
- generic map/geocoding/routing duplication should be retired after migration gates pass.
- one Orientation bounded context may contain multiple runtime artifacts.
