# Orientation – Product Direction after v0.3.0

**Status:** v0.4.0 standalone spatial-research and persistent-discovery baseline implemented; release promotion follows the final #24 hardening gate.

## Why this document exists

Orientation v0.1.0 through v0.3.0 deliberately established reusable geospatial foundations first:

- provider-neutral map surface and host bridge;
- current-position presentation;
- place search and reverse geocoding;
- provider-neutral routing;
- Valhalla-backed DRIVING/CYCLING/WALKING;
- route rendering and a complete reference route-planning workflow.

Orientation is not only reusable infrastructure for Vocation/WGT. It is an independently useful personal spatial exploration and mobility application/context. Reuse by Vocation, WGT and future contexts is an additional system role.

## v0.4.0 baseline

**Focus:** first standalone spatial-research and persistent-discovery product baseline.

The implemented Orientation loop is:

```text
question -> research prompt -> external structured result -> validation/import
        -> local persistent spatial collection -> reopen -> explore -> select -> route
```

### Completed work packages

1. **#20 — Spatial research/import contract**
   - explicit user-supplied spatial research criteria;
   - versioned `orientation.spatial-research` 1.0 JSON contract;
   - source/provenance representation;
   - strict semantic validation;
   - no silent sensitive-trait inference from heuristics.

2. **#21 — External research prompt generation**
   - deterministic prompt generation from explicit question/criteria input;
   - contract/version guidance embedded in the prompt;
   - copy/export-friendly text boundary;
   - no paid LLM API and no generic prompt service.

3. **#22 — Import and persistence**
   - validate before mutation;
   - explicit anti-corruption translation into Orientation discovery state;
   - local SQLite persistence for Orientation-owned collections;
   - retained provenance/evidence;
   - deterministic unchanged re-import handling;
   - restart/reopen persistence proof.

4. **#23 — Standalone discovery application**
   - separate first-class standalone browser entrypoint;
   - question/prompt/import workflow;
   - persisted collection list/reopen;
   - candidate list/map composition and evidence inspection;
   - filtering/sorting required by the accepted contract;
   - destination selection and DRIVING/CYCLING/WALKING routing;
   - Reference Host and Embed Host remain separate surfaces.

5. **#24 — Integrated hardening/release**
   - complete backend/map regression;
   - real Valhalla + deterministic Photon + Chrome product smoke;
   - import -> backend restart -> reopen -> route proof;
   - dependency-vulnerability gate;
   - documentation/repository consistency;
   - unchanged `orientation.host-bridge` 1.0 compatibility boundary.

### v0.4.0 non-goals

The release does not claim:

- public-transit routing;
- realtime transit;
- bike/scooter/car-sharing integration;
- multimodal journey planning;
- turn-by-turn live GPS rerouting;
- automatic background crawling;
- paid LLM API/model execution;
- generic prompt/research microservice;
- Vocation migration completion;
- cross-device synchronization;
- broad arbitrary-schema data-lake behavior.

## Mobility after the standalone baseline

Deeper Navigate slices should now be prioritized by real use rather than provider availability alone.

Likely future capability families include public transit, shared mobility and multimodal planning. These require explicit new domain models and provider/data boundaries; they must not be represented as extra string values in the v0.3/v0.4 `TravelProfile` contract.

## Vocation boundary

Vocation and Orientation remain separate bounded contexts.

- Vocation owns why a job/work location matters and how precise/trustworthy that job-domain location is.
- Orientation owns generic place resolution, spatial exploration and mobility.
- Orientation owns its own unrelated personal spatial-research collections.

A Vocation "route to workplace" action may consume Orientation routing without moving job-market state into Orientation.

## Prompt/research service decision

Do **not** extract Vocation, Illumination and Orientation prompt workflows into a separate bounded context merely because all three can generate text for ChatGPT and consume JSON.

Their domain semantics remain different. Shared mechanical helpers may later become libraries if real duplication justifies it. A system-wide LLM/research execution capability should be reconsidered only when there is a concrete cross-context operational requirement such as centrally managed execution, credentials, quotas, queues or observability.

## Sequencing principle

With the standalone research/import loop established, later provider-heavy mobility capabilities and consumer migrations may compete for priority as concrete slices. Do not pre-create speculative milestone ladders or contracts.
