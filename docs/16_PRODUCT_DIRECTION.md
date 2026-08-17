# Orientation – Product Direction after v0.3.0

**Status:** Accepted direction after v0.3.0 review; exact v0.4.0 contracts and issue slices remain to be defined before implementation.

## Why this document exists

Orientation v0.1.0 through v0.3.0 deliberately established reusable geospatial foundations first:

- provider-neutral map surface and host bridge;
- current-position presentation;
- place search and reverse geocoding;
- provider-neutral routing;
- Valhalla-backed DRIVING/CYCLING/WALKING;
- route rendering and a complete reference route-planning workflow.

Those releases are valid foundations, but they can be misread as if Orientation existed only to serve Vocation/WGT. That is not the intended product boundary.

Orientation is an independently useful personal spatial exploration and mobility application/context. Reuse by Vocation, WGT and future contexts is an additional system role.

## Next planned release direction: v0.4.0

**Focus:** first standalone spatial-research and persistent-discovery product baseline.

The goal is to prove the independent Orientation loop:

```text
question -> research prompt -> external structured result -> validation/import
        -> local spatial collection -> explore -> select -> route
```

### Candidate v0.4.0 work packages

The exact issue decomposition must be reviewed before implementation, but v0.4.0 should stay within this coherent scope:

1. **Define Orientation spatial-research/import semantics**
   - user-supplied spatial research question/criteria;
   - explicit provenance/source representation;
   - versioned Orientation-owned JSON import contract;
   - compatibility/validation/error semantics;
   - no silent sensitive-trait inference from heuristics.

2. **Generate external-research prompts**
   - initial research prompt from explicit user input;
   - schema/version guidance embedded in the prompt;
   - copy/export workflow suitable for external ChatGPT;
   - no paid LLM API requirement;
   - no generic system prompt service.

3. **Import and persist Orientation-owned discovery data**
   - validate before mutation;
   - translate imported artifacts through an explicit boundary;
   - retain provenance required to understand researched claims;
   - introduce local persistence for Orientation-owned state;
   - never copy another bounded context's authoritative database.

4. **Provide a first-class standalone discovery workflow**
   - create/open a discovery collection;
   - import researched candidates;
   - list/map exploration;
   - inspect relevant evidence/details;
   - basic filtering/sorting required by the accepted schema;
   - select a destination and invoke the existing routing foundation.

5. **Harden the independent product baseline**
   - persistent restart/reload proof;
   - invalid/incompatible import states;
   - map/routing regression;
   - coherent standalone UX;
   - documentation and release consistency.

### Explicit v0.4.0 non-goals

Do not pull these into the first persistent product baseline unless a concrete blocker makes them necessary:

- public-transit routing;
- realtime transit;
- bike/scooter/car-sharing integration;
- multimodal journey planning;
- turn-by-turn live GPS rerouting;
- automatic background crawling;
- paid LLM API/model execution;
- generic prompt/research microservice;
- Vocation migration work;
- cross-device synchronization;
- broad arbitrary-schema data lake behavior.

## Mobility after the standalone baseline

After the standalone research/import loop exists, deeper Navigate slices can be prioritized by real use rather than provider availability alone.

Likely capability families include:

### Public transit

- departure/arrival time;
- transit legs and transfers;
- stops/platforms where the provider can support them;
- service calendars;
- delays/disruptions/realtime where available;
- route alternatives.

### Shared mobility

- nearby shared vehicles/stations;
- bike, scooter and car sharing where suitable provider data exists;
- availability/freshness semantics;
- provider/operator information;
- explicit hand-off when booking/unlocking remains operator-owned.

### Multimodal planning

- walking + transit;
- walking/cycling + transit;
- shared mobility + transit;
- sensible transfer/waiting/availability costs;
- comparison by time, transfers, walking distance or other Orientation-owned generic mobility criteria.

These future capabilities require explicit new domain models. They must not be represented as extra string values in the v0.3.0 `TravelProfile` contract.

## Vocation boundary

Vocation and Orientation should not be merged.

Their overlap is intentional integration:

- Vocation owns why a job/work location matters and how precise/trustworthy that job-domain location is.
- Orientation owns generic place resolution, spatial exploration and mobility.
- Orientation also owns its own unrelated personal spatial-research collections.

A future Vocation "route to workplace" action can consume Orientation routing without moving job-market state into Orientation.

## Prompt/research service decision

Do **not** extract Vocation, Illumination and Orientation prompt workflows into a separate bounded context or microservice merely because all three can generate text for ChatGPT and consume JSON.

The business semantics differ:

- Vocation prompts create/update job-market knowledge governed by Vocation contracts.
- Illumination prompts create learning content governed by Illumination contracts.
- Orientation prompts create spatial-research data governed by Orientation contracts.

The shared portion today is mostly mechanical: template rendering, copy/export, version/schema display and possibly generic JSON-validation helpers. Such mechanics may be shared later as libraries if duplication becomes meaningful, without moving domain ownership.

A system-wide LLM/research execution capability should be reconsidered only when a concrete cross-context operational problem exists, for example centrally managed model/API execution, credentials, rate limits, queued jobs, observability or reusable web-research tooling. That would require a separate System Architecture decision and still would not own each domain's prompt or result schema.

## Sequencing principle

Orientation should first become demonstrably useful on its own using the already released map/place/routing foundations.

Only then should later provider-heavy mobility capabilities or consumer migrations compete for priority.
