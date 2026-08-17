# Orientation – Standalone Discovery Application

**Status:** v0.4.0 standalone product slice for Issue #23.

## Purpose

`app.html` is the first first-class end-user Orientation surface. It demonstrates that Orientation is useful independently of Vocation, Illumination or WGT composition.

The complete local-first workflow is:

```text
spatial question + explicit criteria
  -> Orientation research prompt
  -> external ChatGPT/research interaction
  -> Spatial Research Bundle 1.0
  -> validate/import
  -> persisted Discovery Collection
  -> list/map/evidence exploration
  -> candidate selection
  -> existing Orientation route planning
```

The external research interaction remains manual in v0.4. Orientation does not require a paid LLM API.

## Browser surfaces

The map package now intentionally has three different browser entries:

- `app.html` — first-class standalone Orientation product surface;
- `index.html` — Reference/acceptance/development host for generic capability validation;
- `embed.html` — provider-neutral reusable Embed Host exposing `orientation.host-bridge` 1.0.

Standalone product behavior must not leak into the Embed Host or Host Bridge protocol. The Reference Host remains useful for isolated place/routing/map acceptance even though it is no longer the only independent browser surface.

## Research workflow

The standalone app captures only the accepted Spatial Research Question fields:

- free research question;
- explicit area-center label;
- radius;
- one or more criteria;
- per-criterion `EVIDENCE_REQUIRED` or `HEURISTIC` mode.

Technical local refs are generated deterministically from form/order rather than exposed as user-facing editing concerns.

`POST /api/v1/research/prompts` generates the version-bound prompt. The app can copy the returned prompt through the browser clipboard API with a selection/copy fallback; clipboard mechanics remain presentation code.

## Import workflow

The user pastes a returned JSON bundle and explicitly requests validation/import.

The app displays:

- `CREATED` with candidate count;
- `UNCHANGED` when the same canonical bundle already exists;
- `REJECTED` plus deterministic validation errors;
- backend-unavailable state separately from research rejection.

A rejected import never removes or replaces an existing collection.

## Collections and candidates

The app lists persisted collections from `/api/v1/discovery/collections`. Opening a collection loads its detail read model and presents:

- question and radial area;
- candidate count and mapped-candidate count;
- simple name/location filtering;
- research-order or name sorting;
- candidate evidence details;
- explicit source links;
- a map scene for candidates whose researched location contains a coordinate.

A candidate without a coordinate remains explorable as research data but cannot be routed until a later explicit location-resolution capability supplies a usable destination. No coordinate is invented from its label inside the UI.

Selecting a candidate from the map or list exposes the same persisted evidence. List selection may focus the map to that feature; `Fit mapped candidates` restores the collection scene.

## Routing

The destination is the selected mapped Discovery Candidate. The origin is selected explicitly through existing Orientation Place Search.

The app then reuses the released v0.3 routing boundary unchanged:

- DRIVING;
- CYCLING;
- WALKING.

Route rendering uses the reusable Map Surface overlay. Changing destination, start or profile invalidates an active route. Clearing a route only clears route state; Discovery Collection state is untouched.

Provider failures are shown locally and do not mutate the persisted collection.

## Persistent runtime acceptance

The `Valhalla Smoke` workflow now validates both generic Reference Host routing and the standalone product flow with real browser/runtime boundaries.

The standalone smoke:

1. starts with a clean local SQLite database;
2. opens `app.html` in real headless Chrome;
3. generates a contract-bound research prompt;
4. proves an incompatible bundle is rejected without creating a collection;
5. imports the canonical valid bundle through the UI;
6. records the persisted Orientation collection id;
7. terminates and restarts the Orientation backend using the same SQLite database;
8. opens a new Chrome session and verifies the same collection id is available;
9. opens the candidate and provenance details;
10. searches an explicit start through the deterministic Photon stub;
11. requests and renders a real Valhalla-backed route to the imported candidate;
12. clears the route and verifies discovery state remains present.

This runtime smoke complements, rather than duplicates, the focused repository restart/re-import tests from Issue #22.

## Explicit non-goals

- no automatic ChatGPT/LLM execution;
- no background crawling;
- no public-transit routing;
- no realtime transit;
- no bike/scooter/car-sharing integration;
- no multimodal planning;
- no cross-device synchronization;
- no Vocation dependency;
- no admin/settings/dashboard expansion;
- no Host Bridge 1.0 redesign.
