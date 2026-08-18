# AGENTS.md

## Purpose

This repository owns the **Orientation** bounded context in the `wgt-system` organization.

Orientation is an independently useful local-first personal spatial exploration and mobility context. It also provides reusable generic geospatial capabilities to other accepted bounded contexts and WGT hosts.

Its current capability space includes Discover, Explore, Navigate and Current Location. The released v0.4 baseline includes Orientation-owned spatial research prompts/imports, persistent personal discovery collections and a first-class standalone discovery-to-direct-route workflow. The released v0.5 baseline adds a separate time-dependent public-transit Journey model, MOTIS-backed planning, reusable Journey rendering and standalone Journey comparison/selection.

## WGT System Architecture

The system-level architecture source of truth is `wgt-system/architecture`.

Before introducing cross-context integration, synchronization/replication, generic relay/storage, shared cross-context infrastructure or another system-wide capability, consult the system Capability Catalog and Integration Policy.

Do not independently implement a generic capability already owned by another accepted bounded context. Generic durable opaque cross-device delivery is owned by **Conveyance**. Generic geospatial/map/geocoding/routing/mobility capability is owned by **Orientation**.

This does not transfer provider-domain semantics to Orientation. Vocation owns Work Location, Opportunity, Company, Posting and other job-market meaning. WGT owns device/platform product integration and presentation.

If an existing system capability does not satisfy a concrete requirement, return that requirement to the System Architecture Control Plane rather than silently creating a competing subsystem. Do not make runtime code depend on the architecture repository.

## Orientation ownership rules

Orientation owns:

- personal spatial discovery/research semantics;
- Orientation-owned prompt/import contracts for external spatial research;
- validated imported spatial research, provenance and persistent personal discovery collections;
- generic spatial geometry and spatial-feature representation;
- map scene/composition and renderer lifecycle;
- generic map layers, clustering, selection and hit testing;
- basemap/tile/style/provider integration;
- generic geocoding, reverse geocoding and place discovery;
- generic direct routing and provider-neutral public-transit Journey semantics/rendering;
- generic current-position and accuracy representation;
- mobility-provider adapters and technical geospatial policy.

Orientation does not own foreign business semantics, another context's persistence, WGT product-shell meaning, OS permission policy, Conveyance delivery, a universal business catalog or a generic prompt/LLM execution platform.

## External research / prompt rules

Orientation may generate prompts intended for an explicit user-controlled external ChatGPT/research workflow and import resulting structured JSON through Orientation-owned versioned contracts.

- Prompt semantics, requested fields, validation, provenance and import translation belong to Orientation when they create Orientation-owned spatial data.
- External generated data is evidence/input, not automatically authoritative truth.
- Heuristics must not silently become asserted sensitive personal characteristics.
- Validate the complete import before mutating persisted state.
- Keep provider-backed `Place` information distinct from researched claims/provenance.
- Do not introduce a paid LLM API, generic AI gateway or generic research microservice without a concrete requirement and system architecture decision.

## Cross-context rules

- No direct reads/writes of another bounded context's database.
- No imports of foreign domain classes into Orientation.
- Cross-context integration uses explicit provider-owned contracts/adapters.
- Rich marker content may carry provider-owned labels/information/resources, but Orientation must not reinterpret business meaning.
- Orientation and Vocation remain separate bounded contexts.
- Do not preserve legacy generic map/geocoding duplication after an Orientation replacement has passed migration gates.

## Architecture and dependency rules

### Backend

Use dependency direction:

`domain <- application <- adapters/infrastructure <- host`

Domain must not depend on Spring, HTTP clients, persistence technology, Valhalla, MOTIS or map rendering. Application defines ports; provider/persistence/import adapters implement those ports.

SQLite persistence is an Orientation infrastructure fact for Orientation-owned discovery state only.

### Local-first provider topology

Default runtime behavior must not silently disclose semantic requests to hosted third parties.

- Orientation backend binds to `127.0.0.1` by default.
- MOTIS defaults to `127.0.0.1:8081` and currently backs both Place Search/Reverse Geocoding and public-transit Journey planning through separate application ports.
- Valhalla defaults to `127.0.0.1:8002` for direct routing.
- There is no automatic Transitous, Photon or other hosted semantic-provider fallback.
- OpenFreeMap is the one intentional external default runtime dependency for basemap style/tile resources.
- A non-loopback provider configuration is an explicit deployment/privacy decision, never an automatic recovery path.

Do not weaken loopback defaults merely to make a phone or another LAN device connect ad hoc. Mobile runtime distribution/pairing requires its own reviewed deployment slice.

### Map surface

The reusable map surface is framework-independent TypeScript. Do not make React, Avalonia or Vocation semantics part of its core API.

MapLibre GL JS is infrastructure. Scene/input/output contracts must not expose MapLibre-specific objects to consumers.

Direct Route and Journey overlays are separate reusable states. Product hosts decide whether presentation is mutually exclusive.

### Routing and mobility

Valhalla is the upstream engine behind the Orientation-owned direct `RoutingPort`. Direct `TravelProfile` remains exactly DRIVING/CYCLING/WALKING.

Public transit uses the separate time-dependent `Journey` model and `JourneyPort`. MOTIS v2.11.0 is the accepted Journey adapter. Provider DTOs/modes/errors stay in infrastructure. Deterministic acceptance uses pinned self-hosted MOTIS plus pinned Aachen OSM/GTFS fixtures.

Do not add `TRANSIT` to direct `TravelProfile` or collapse Journey into Route. Shared mobility/GBFS, fares/ticketing, richer disruption semantics and arbitrary multimodal sharing optimization require explicit later slices.

## Current technology baseline

- Java 25 LTS
- Apache Maven 3.9.x baseline
- Spring Boot 4.1.x baseline
- local SQLite for Orientation discovery persistence
- Node.js 24 LTS
- TypeScript
- MapLibre GL JS 6
- local MOTIS v2.11.0 for place/geocoding and public-transit Journey infrastructure
- Valhalla for direct-routing infrastructure
- OpenFreeMap Liberty as the intentional hosted basemap

Exact dependency versions are implementation facts and may be updated through normal maintenance.

## UI status

Orientation has three intentionally separate browser surfaces:

- **Standalone App** — first-class end-user research/import/discovery/map/navigation workflow, including direct Route and public-transit Journey;
- **Reference Host** — development/acceptance surface for reusable capabilities;
- **Embed Host** — reusable host boundary using `orientation.host-bridge` 1.0.

The Standalone App must keep Research, Collections and Navigate directly reachable. Long desktop workspaces may use independently scrollable columns; narrow/mobile layouts must retain normal document scrolling and touch-reachable navigation.

Do not collapse standalone product semantics into the reusable Map Surface or Host Bridge.

WGT may present Orientation on platform-specific hosts without becoming owner of Orientation semantics. Desktop Orientation hosting is established; physical-iPhone validation is not established and must not be inferred from compile-only or desktop evidence.

## Branch and worker workflow

- `main` — stable accepted milestone state.
- `dev` — active integrated development.
- Feature branches only for genuinely parallel/risky/reviewable work.
- GitHub Milestones, when created, use semantic version names only.
- Issues are durable work packages; do not create ceremony for tiny edits.
- Implementation workers do not invent milestones/issues or expand scope unless the control plane explicitly asks.

Canonical local path: `P:\wgt-system\orientation`

## Git/GitHub execution

The system-wide agent execution and Git/GitHub authentication policy in `wgt-system/architecture/AGENTS.md` applies here.

Do not treat an isolated-sandbox credential failure as proof that the user's normal GitHub authentication is invalid. Use supported direct control-plane GitHub writes where appropriate; for explicitly authorized local operations, use the existing authenticated normal-user session when available.

## Definition of done for a worker slice

Before claiming completion:

- run relevant backend/map tests and build/type checks;
- run the relevant real-provider/browser smoke when the changed path affects that boundary;
- inspect diff hygiene;
- update architecture/docs when semantics changed;
- commit intentionally and push the active branch;
- return commit SHA, checks and remaining blockers.
