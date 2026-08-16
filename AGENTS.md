# AGENTS.md

## Purpose

This repository owns the **Orientation** bounded context in the `wgt-system` organization.

Orientation provides reusable generic geospatial capabilities: discovering spatial objects, exploring them on maps, determining routes, and representing host-supplied current location.

## WGT System Architecture

This repository is one bounded context within the `wgt-system` organization.

The system-level architecture source of truth is:

`wgt-system/architecture`

Before introducing any of the following, consult the system Capability Catalog and Integration Policy:

- cross-context integration;
- synchronization or replication;
- generic relay or storage infrastructure;
- service discovery/registry infrastructure;
- shared cross-context infrastructure;
- another system-wide capability.

Do not independently implement a generic capability already owned by another accepted bounded context.

Generic durable opaque cross-device delivery is owned by **Conveyance**.

Generic geospatial/map/geocoding/routing capability is owned by **Orientation**, as accepted by the System Architecture Control Plane in [ADR-0003](https://github.com/wgt-system/architecture/blob/dev/adr/0003-orientation-geospatial-capability-ownership.md).

This does not transfer provider-domain semantics to Orientation. Vocation continues to own Work Location, Opportunity, Company, Posting, External Link and other job-market meaning. WGT continues to own device/platform product integration and presentation.

If an existing system capability does not satisfy a concrete requirement, return the requirement to the System Architecture Control Plane rather than silently creating a competing subsystem.

Do not make runtime code depend on the architecture repository.

## Orientation ownership rules

Orientation owns:

- generic spatial geometry and spatial-feature representation;
- map scene/composition and renderer lifecycle;
- generic map layers, clustering, selection and hit testing;
- basemap/tile/style/provider integration;
- generic geocoding and reverse geocoding;
- generic place/POI discovery when a concrete accepted slice introduces it;
- generic routing, route geometry, distance/duration and directions;
- generic current-position and accuracy representation;
- geospatial provider adapters, technical caching, rate/failure handling and performance policy.

Orientation does not own:

- foreign business semantics or authoritative state;
- another bounded context's persistence;
- WGT product navigation or domain-screen meaning;
- operating-system permission policy;
- installation trust;
- generic durable cross-device delivery.

## Cross-context rules

- No direct reads/writes of another bounded context's database.
- No imports of foreign domain classes into Orientation.
- Cross-context integration uses explicit provider-owned Application/Published Contracts or adapters.
- Rich marker content may carry provider-owned labels, information and external resources, but Orientation must not reinterpret their business meaning.
- A provider may call Orientation (for example Vocation -> geocoding -> Vocation) when the provider must interpret the generic result.
- WGT may compose provider data with Orientation when the operation is product presentation/orchestration rather than provider-domain interpretation.
- Do not preserve legacy generic map/geocoding duplication merely for compatibility after an Orientation replacement has passed its migration gates.

## Architecture and dependency rules

### Backend

Use dependency direction:

`domain <- application <- adapters/infrastructure <- host`

Domain must not depend on Spring, HTTP clients, persistence, Valhalla, providers or map rendering.

Application defines ports. Provider adapters implement those ports.

Do not add persistence until a concrete stateful requirement justifies it.

### Map surface

The reusable map surface is framework-independent TypeScript. Do not make React, Avalonia or Vocation semantics part of its core API.

MapLibre GL JS is infrastructure. Scene/input/output contracts must not expose MapLibre-specific objects to consumers.

Map interaction should emit generic events/references. The host decides product/domain navigation and external-resource execution.

### Routing

Valhalla is an external engine behind an Orientation-owned adapter. Do not fork/vendor Valhalla source into this repository without a separate explicit decision.

## Current technology baseline

- Java 25 LTS
- Apache Maven 3.9.x baseline, wrapper pinned by repository bootstrap
- Spring Boot 4.1.x baseline
- Node.js 24 LTS for map tooling
- TypeScript
- MapLibre GL JS 6
- Valhalla as upstream routing engine

Exact dependency versions are repository implementation facts and may be updated through normal maintenance.

## UI status

Any Orientation standalone/browser UI is a **reference/development/debug host**, not the primary WGT product UI.

It should still be coherent and visually useful because it is the primary independent validation surface for Orientation capabilities.

## Branch and worker workflow

- `main` — stable accepted milestone state.
- `dev` — active integrated development.
- Feature branches only for genuinely parallel/risky/reviewable work.
- GitHub Milestones, when created, use semantic version names only: `v0.1.0`, `v0.2.0`, ...
- Issues are durable work packages; do not create ceremony for tiny edits.
- Implementation workers do not invent milestones/issues or expand scope unless the control plane explicitly asks.

Canonical local path:

`P:\wgt-system\orientation`

## Git/GitHub execution

The system-wide agent execution and Git/GitHub authentication policy in `wgt-system/architecture/AGENTS.md` applies here.

Do not treat an isolated-sandbox credential failure as proof that the user's normal GitHub authentication is invalid, and do not start a re-login/device-flow/credential-manager change for that reason. Use supported direct control-plane GitHub writes where appropriate; for explicitly authorized local operations, use the existing authenticated normal-user `git`/`gh` session when the sandbox cannot access it.

Do not repeat the system-wide policy in ordinary worker prompts.

## Definition of done for a worker slice

Before claiming completion:

- run the relevant backend/map tests;
- run the relevant build/type checks;
- inspect `git diff --check`;
- confirm no generated build output or bootstrap ZIP is staged;
- update architecture/docs when semantics changed;
- commit intentionally;
- push the active branch;
- return commit SHA, checks and remaining blockers.
