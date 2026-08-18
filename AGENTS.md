# AGENTS.md

## Purpose

This repository owns the **Orientation** bounded context in the `wgt-system` organization.

Orientation is an independently useful local-first personal spatial exploration and mobility context. It also provides reusable generic geospatial capabilities to other accepted bounded contexts and WGT hosts.

Its current capability space includes Discover, Explore, Navigate and Current Location. The released v0.4 baseline includes Orientation-owned spatial research prompts/imports, persistent personal discovery collections and a first-class standalone discovery-to-direct-route workflow. The released v0.5 baseline adds a separate time-dependent public-transit Journey model, MOTIS-backed planning, reusable Journey rendering and standalone Journey comparison/selection.

## WGT System Architecture

This repository is one bounded context within the `wgt-system` organization.

The system-level architecture source of truth is:

`wgt-system/architecture`

Before introducing cross-context integration, synchronization/replication, generic relay/storage, shared cross-context infrastructure or another system-wide capability, consult the system Capability Catalog and Integration Policy.

Do not independently implement a generic capability already owned by another accepted bounded context.

Generic durable opaque cross-device delivery is owned by **Conveyance**.

Generic geospatial/map/geocoding/routing/mobility capability is owned by **Orientation**. Orientation also owns its domain-specific external spatial-research prompt/import workflow and the resulting Orientation-owned discovery state.

This does not transfer provider-domain semantics to Orientation. Vocation continues to own Work Location, Opportunity, Company, Posting, External Link and other job-market meaning. WGT continues to own device/platform product integration and presentation.

If an existing system capability does not satisfy a concrete requirement, return that requirement to the System Architecture Control Plane rather than silently creating a competing subsystem.

Do not make runtime code depend on the architecture repository.

## Orientation ownership rules

Orientation owns:

- personal spatial discovery/research semantics;
- Orientation-owned prompt/import contracts for external spatial research;
- validated imported spatial research, provenance and persistent personal discovery collections;
- generic spatial geometry and spatial-feature representation;
- map scene/composition and renderer lifecycle;
- generic map layers, clustering, selection and hit testing;
- basemap/tile/style/provider integration;
- generic geocoding and reverse geocoding;
- generic place/POI discovery;
- generic direct routing, Route geometry, distance/duration and directions;
- provider-neutral public-transit Journey semantics and Journey rendering;
- generic current-position and accuracy representation;
- generic mobility-planning semantics and mobility-provider adapters introduced by accepted slices;
- geospatial provider adapters, technical caching, rate/failure handling and performance policy.

Orientation does not own:

- foreign business semantics or authoritative state;
- another bounded context's persistence;
- WGT product-shell/navigation meaning;
- operating-system permission policy;
- installation trust;
- generic durable cross-device delivery;
- a universal business-entity catalog;
- a generic prompt/LLM execution platform merely because Orientation generates domain-specific prompts.

## External research / prompt rules

Orientation may generate prompts intended for external ChatGPT/research workflows and import the resulting structured JSON through Orientation-owned versioned contracts.

- Prompt semantics, requested fields, validation, provenance and import translation belong to Orientation when they create Orientation-owned spatial data.
- External generated data is evidence/input, not automatically authoritative truth.
- User-supplied research criteria may be represented explicitly, but heuristics must not be silently converted into asserted sensitive personal characteristics.
- Validate the complete import before mutating persisted Orientation state.
- Keep provider-backed `Place` information distinct from researched claims/provenance.
- Do not introduce a paid LLM API, generic AI gateway or generic research microservice unless a concrete requirement and System Architecture decision justify one.
- Shared non-semantic mechanics may later be extracted as utilities/libraries; domain prompt/import contracts remain with their owning bounded context.

## Cross-context rules

- No direct reads/writes of another bounded context's database.
- No imports of foreign domain classes into Orientation.
- Cross-context integration uses explicit provider-owned Application/Published Contracts or adapters.
- Rich marker content may carry provider-owned labels, information and external resources, but Orientation must not reinterpret their business meaning.
- A provider may call Orientation when the provider must interpret a generic result.
- WGT may compose provider data with Orientation when the operation is product presentation/orchestration rather than provider-domain interpretation.
- Orientation and Vocation remain separate bounded contexts; shared geographic use cases are integration, not evidence that their domain models should merge.
- Do not preserve legacy generic map/geocoding duplication merely for compatibility after an Orientation replacement has passed its migration gates.

## Architecture and dependency rules

### Backend

Use dependency direction:

`domain <- application <- adapters/infrastructure <- host`

Domain must not depend on Spring, HTTP clients, persistence technology, Valhalla, MOTIS, third-party providers or map rendering.

Application defines ports. Provider/persistence/import adapters implement those ports.

SQLite persistence is currently an Orientation infrastructure fact for Orientation-owned discovery state. It must not become a copied database of foreign-domain state.

### Map surface

The reusable map surface is framework-independent TypeScript. Do not make React, Avalonia or Vocation semantics part of its core API.

MapLibre GL JS is infrastructure. Scene/input/output contracts must not expose MapLibre-specific objects to consumers.

Map interaction should emit generic events/references. A host decides product/domain navigation and external-resource execution where that meaning is host/provider-owned.

Direct Route and Journey overlays are separate reusable states. Do not couple them implicitly in the Map Surface; product hosts decide whether their presentation is mutually exclusive.

### Routing and mobility

Valhalla is the external engine behind the Orientation-owned direct `RoutingPort` adapter. Do not fork/vendor Valhalla source into this repository without a separate explicit decision.

Direct `TravelProfile` remains exactly DRIVING/CYCLING/WALKING.

Public transit uses the separate time-dependent `Journey` model and `JourneyPort`. MOTIS v2.11.0 is the first accepted Journey adapter. Provider DTOs/mode enums/errors stay in infrastructure. Default configuration targets local MOTIS; deterministic acceptance uses a pinned self-hosted MOTIS runtime and pinned Aachen OSM/GTFS fixture rather than public Transitous.

Do not add `TRANSIT` to direct `TravelProfile` or collapse Journey into Route. Shared mobility/GBFS, fares/ticketing, richer disruption semantics and arbitrary multimodal sharing optimization require explicit later slices rather than being inferred from MOTIS capabilities.

## Current technology baseline

- Java 25 LTS
- Apache Maven 3.9.x baseline, wrapper pinned by repository bootstrap
- Spring Boot 4.1.x baseline
- local SQLite for Orientation discovery persistence
- Node.js 24 LTS for map tooling
- TypeScript
- MapLibre GL JS 6
- Photon-compatible place/geocoding provider boundary
- Valhalla as upstream direct-routing engine
- MOTIS v2.11.0 as the first upstream public-transit Journey engine

Exact dependency versions are repository implementation facts and may be updated through normal maintenance.

## UI status

Orientation currently has three intentionally separate browser surfaces:

- **Standalone App** — first-class Orientation end-user research/import/discovery/map/navigation workflow, including direct Route and the released v0.5 public-transit Journey flow;
- **Reference Host** — development/acceptance surface for reusable geospatial/direct-routing capabilities;
- **Embed Host** — reusable host boundary using `orientation.host-bridge` 1.0.

Do not collapse standalone product semantics into the reusable Map Surface or Host Bridge merely for convenience.

WGT may present Orientation on platform-specific hosts without becoming owner of Orientation semantics. Desktop Orientation hosting is established; physical-iPhone validation is not established and must not be inferred from compile-only or desktop evidence.

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
- run the relevant real-provider/browser smoke when the changed path affects that boundary;
- inspect `git diff --check`;
- confirm no generated build output or bootstrap ZIP is staged;
- update architecture/docs when semantics changed;
- commit intentionally;
- push the active branch;
- return commit SHA, checks and remaining blockers.
