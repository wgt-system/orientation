# ADR-0008 — Journey and MOTIS public-transit boundary

**Status:** Accepted

**Date:** 2026-08-17

## Context

Orientation v0.3.0/v0.4.0 owns provider-neutral direct routing for `DRIVING`, `CYCLING` and `WALKING` through the existing `Route` model and Valhalla adapter.

The next concrete mobility need is public-transport journey planning. Public transport is time-dependent and is naturally composed of access, transit, transfer and egress legs. Treating it as another value in the existing `TravelProfile` enum would collapse materially different semantics into the direct-route contract and make later realtime/shared-mobility composition harder to model correctly.

Current provider evaluation considered MOTIS 2 and OpenTripPlanner 2 as credible open-source multimodal engines.

MOTIS 2 currently provides a versioned REST/JSON API with OpenAPI, supports OSM plus GTFS/NeTEx static transit data, GTFS-RT/SIRI/VDV 454 realtime inputs and GBFS shared-mobility feeds. Its current `v6` planning API already models departure/arrival-time searches, transit modes, detailed legs and independent pre-/post-transit modes. MOTIS v2.11.0 is the reviewed provider baseline for this decision.

OpenTripPlanner 2 remains a credible alternative and supports GTFS/GTFS-RT/GBFS and SIRI, but its supported routing APIs are GraphQL; its former REST API was removed in 2025. That does not make OTP unsuitable, but MOTIS is the smaller first adapter fit for Orientation's existing HTTP/provider-boundary style.

Transitous operates a public MOTIS 2 service and aggregates open transit data internationally. Its Germany feed uses the DELFI dataset and includes Hamburg operators such as S-Bahn Hamburg, AKN, Hochbahn Bus/U-Bahn, VHH and HADAG; realtime sources are configured for the DELFI source. Transitous is nevertheless explicitly best-effort, requests contact before substantial routing load, and logs request metadata including URLs that may contain route start/destination/time for up to two days.

## Decision

### 1. Keep `Route` and `Journey` separate

The released direct-routing model remains unchanged:

```text
RouteRequest -> Route
DRIVING | CYCLING | WALKING
```

Public transport introduces a new provider-neutral Orientation capability:

```text
JourneyRequest -> Journey alternatives -> JourneyLeg[]
```

A `Journey` is not a `Route` with `TRANSIT` added to `TravelProfile`.

The first Journey model must be able to represent, at minimum:

- origin and destination;
- departure-time or arrival-time intent;
- one or more journey alternatives;
- journey start/end and duration;
- transfer count;
- ordered legs;
- access/egress/transfer walking legs;
- transit legs with generic mode, line/service presentation, origin/destination stops and intermediate stop information required by the product slice;
- per-leg geometry where available;
- scheduled timing and optional realtime-adjusted timing/status without claiming realtime when none is available;
- stable Orientation-owned failure outcomes.

Provider IDs and provider DTOs may be retained only behind infrastructure/application correlation boundaries where required for refresh/paging; they are not Orientation domain semantics.

### 2. Use MOTIS as the first Journey provider

Implement a replaceable `JourneyPort` and a MOTIS 2 adapter behind it.

The initial reviewed provider baseline is MOTIS v2.11.0 using `/api/v6/plan`. The adapter must translate MOTIS response data into Orientation-owned Journey types and must not expose MOTIS mode names, response DTOs, cursor structures or provider-specific errors through the domain/application boundary.

The adapter must support the smallest accepted transit slice first. Do not implement every MOTIS option merely because the provider exposes it.

### 3. Transitous is reference/development infrastructure, not an architectural dependency

A configurable Transitous endpoint may be used for bounded manual/reference verification and for evaluating real-world coverage.

Do not make availability of the public Transitous service a deterministic CI/release dependency. Respect its usage policy, send the required identifying User-Agent/contact information, preserve source attribution, and bound request rate/response size/timeouts.

Because route queries sent to Transitous leave the local process and its logs may contain start/destination/time/options, the standalone product must not silently present hosted Transitous use as fully local/private operation.

### 4. Deterministic acceptance uses pinned self-hosted MOTIS

Journey adapter acceptance should use a pinned MOTIS runtime and a retained bounded transit/OSM fixture suitable for deterministic CI and browser smoke evidence.

The production/local-first direction is self-hosted MOTIS fed by appropriately licensed open transit and OSM data. Transitous' processed datasets may be useful where their source licenses and attribution requirements permit, but Orientation must not depend on Transitous-specific data layout as domain semantics.

### 5. Realtime is optional evidence, not a baseline truth claim

The Journey model should distinguish scheduled from realtime-adjusted information when the provider supplies both. Absence of realtime must remain representable.

The first public-transit release may be realtime-aware without claiming complete realtime coverage for Hamburg, Germany or every operator. A realtime acceptance claim requires concrete provider/data evidence for the route under test.

### 6. Shared mobility is deliberately deferred

MOTIS can ingest GBFS and compose sharing modes with transit. However, several sharing-specific planning filters in the current MOTIS OpenAPI are explicitly experimental and may break without API version bumps.

Do not pull bike/scooter/car sharing into the first Journey/public-transit milestone. Shared mobility gets a later concrete product slice and explicit freshness/availability/provider-handoff semantics.

## Consequences

- Valhalla remains the accepted direct `DRIVING`/`CYCLING`/`WALKING` provider; this decision does not replace it.
- Orientation gains a second navigation abstraction, `Journey`, for time-dependent composed mobility.
- Public transit can later be consumed by the standalone Orientation product and by other WGT contexts without leaking MOTIS semantics.
- Realtime support can improve results incrementally without making scheduled journeys invalid.
- A future multimodal slice can compose Journey legs without corrupting the released direct Route contract.
- OTP or another engine can be added/replaced behind `JourneyPort` if a concrete requirement later justifies it.

## Reviewed primary references

- MOTIS repository and OpenAPI: https://github.com/motis-project/motis
- MOTIS v2.11.0 release: https://github.com/motis-project/motis/releases/tag/v2.11.0
- Transitous API usage policy: https://transitous.org/api/
- Transitous source catalog: https://transitous.org/sources/
- Transitous privacy policy: https://transitous.org/privacy/
- Transitous data/import documentation: https://transitous.org/doc/
- OpenTripPlanner current data-source documentation: https://docs.opentripplanner.org/en/latest/Data-Sources/
- OpenTripPlanner API documentation: https://docs.opentripplanner.org/en/latest/apis/Apis/
- GTFS Realtime reference: https://gtfs.org/documentation/realtime/reference/
