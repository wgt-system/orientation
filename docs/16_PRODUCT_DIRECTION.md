# Orientation – Product Direction after v0.5.0

**Status:** v0.5.0 is released and accepted on 2026-08-18.

## Why this document exists

Orientation v0.1.0 through v0.3.0 deliberately established reusable geospatial foundations first:

- provider-neutral map surface and host bridge;
- current-position presentation;
- place search and reverse geocoding;
- provider-neutral direct routing;
- Valhalla-backed DRIVING/CYCLING/WALKING;
- route rendering and a complete reference route-planning workflow.

Orientation is not only reusable infrastructure for Vocation/WGT. It is an independently useful personal spatial exploration and mobility application/context. Reuse by Vocation, WGT and future contexts is an additional system role.

## v0.4.0 baseline

**Focus:** first standalone spatial-research and persistent-discovery product baseline. Released and accepted on 2026-08-17.

The released Orientation loop is:

```text
question -> research prompt -> external structured result -> validation/import
        -> local persistent spatial collection -> reopen -> explore -> select -> direct route
```

Completed packages #20–#24 established the Orientation-owned Spatial Research Bundle 1.0, deterministic external-research prompt generation, strict import, local SQLite discovery persistence, the first standalone discovery application and the integrated v0.4 hardening/release gate.

## v0.5.0 public-transit baseline

**Focus:** make time-dependent public-transit Journey planning a first-class standalone Orientation Navigate flow without weakening the released direct Route boundary.

The released baseline is:

```text
selected destination + explicit origin + depart/arrive time
        ↓
provider-neutral JourneyRequest
        ↓
Orientation JourneyPort
        ↓
MOTIS v2.11.0 adapter
        ↓
Journey alternatives with ordered WALK/transit legs
        ↓
compare timing/transfers/legs
        ↓
select Journey
        ↓
Journey Map Surface overlay
```

### Completed work packages

1. **#40 — Public-transit Journey boundary**
   - separate `Journey` abstraction; `TRANSIT` is not added to direct `TravelProfile`;
   - offset-aware `DEPART_AT` / `ARRIVE_BY` requests;
   - bounded alternatives, legs, stops and decoded geometry;
   - scheduled timing plus optional realtime-adjusted timing;
   - stable provider-neutral failures and `POST /api/v1/journeys`.

2. **#41 — MOTIS provider**
   - MOTIS v2.11.0 `/api/v6/plan` behind `JourneyPort`;
   - provider DTO/mode/error isolation;
   - explicit transit-mode request boundary excluding shared/rental/ODM/ride-sharing modes;
   - local MOTIS as default rather than a public Transitous endpoint;
   - deterministic self-hosted acceptance using pinned MOTIS plus pinned Aachen OSM/GTFS data.

3. **#42 — Journey Map Surface**
   - Journey state/source/layers separate from direct Route state;
   - ordered WALK/transit geometry;
   - dashed WALK vs solid transit plus explicit transit-stop markers;
   - replace/clear/lifecycle behavior and antimeridian-safe viewport fitting.

4. **#43 — Standalone Journey flow**
   - Public transit beside DRIVING/CYCLING/WALKING;
   - explicit depart-at/arrive-by and local date/time input converted to offset-aware requests;
   - alternative summaries with duration, transfers, ordered legs and scheduled/realtime timing state;
   - selected Journey rendering;
   - stale-request protection and explicit Route/Journey switching;
   - discovery state retained across Journey replacement/clear and mode changes;
   - real production-browser acceptance against self-hosted MOTIS.

5. **#44 — Integrated hardening/release**
   - final regression/security/repository consistency and release evidence completed;
   - explicit Control Plane approval authorized `main` promotion, tag/GitHub Release and milestone closure.

### v0.5.0 boundaries

v0.5.0 does not claim:

- shared mobility/GBFS vehicle or station availability;
- bike/scooter/car-sharing booking;
- fares, ticket sales or ticket validity;
- complete realtime coverage across all operators;
- arbitrary multimodal optimization across independent sharing providers;
- turn-by-turn live GPS navigation/rerouting;
- automatic background location forwarding;
- Vocation migration completion;
- a new `orientation.host-bridge` version.

Hosted Transitous may be used only as an explicitly configured reference/manual endpoint. Deterministic acceptance and release evidence do not depend on that public service.

## Vocation boundary

Vocation and Orientation remain separate bounded contexts.

- Vocation owns why a job/work location matters and how precise/trustworthy that job-domain location is.
- Orientation owns generic place resolution, spatial exploration and mobility.
- Orientation owns its own unrelated personal spatial-research collections.

A Vocation "route to workplace" action may consume Orientation routing/Journey capabilities without moving job-market state into Orientation.

## Prompt/research service decision

Do **not** extract Vocation, Illumination and Orientation prompt workflows into a separate bounded context merely because all three can generate text for ChatGPT and consume JSON.

Their domain semantics remain different. Shared mechanical helpers may later become libraries if real duplication justifies it. A system-wide LLM/research execution capability should be reconsidered only when there is a concrete cross-context operational requirement such as centrally managed execution, credentials, quotas, queues or observability.

## What may come after v0.5.0

Do not create another version ladder merely because v0.5.0 is released. Shared mobility, richer realtime/disruption semantics, multimodal planning and consumer migrations remain candidate capability families only; each requires a concrete product slice and explicit ownership/provider review before milestone creation.
