# Orientation – MOTIS Journey Provider

**Status:** v0.5.0 Issue #41 first public-transit provider integration.

ADR-0008 accepts MOTIS as the first replaceable provider behind Orientation's provider-neutral `JourneyPort`. MOTIS is infrastructure. `JourneyRequest`, `JourneyPlan`, `Journey`, `JourneyLeg`, stops, timing and stable failures remain Orientation-owned semantics.

## Reviewed provider baseline

The first adapter is reviewed against:

- MOTIS `v2.11.0`;
- MOTIS API `v6`;
- planning endpoint `GET /api/v6/plan`;
- official Linux amd64 release archive `motis-linux-amd64.tar.bz2`;
- archive SHA-256 `508505d3f9cd2e872c763743c459cae4e0539fad14bf490e23251e013d3a6dfa`.

The implementation does not infer compatibility with arbitrary future MOTIS versions merely because the endpoint path remains `/api/v6/plan`.

## Runtime configuration

Orientation configures MOTIS under `orientation.motis`:

```yaml
orientation:
  motis:
    base-url: ${ORIENTATION_MOTIS_BASE_URL:http://localhost:8081}
    connect-timeout: 3s
    read-timeout: 15s
    user-agent: wgt-system-orientation/0.5.0 (+https://github.com/wgt-system/orientation)
```

The default is deliberately local. Orientation does **not** silently route Journey requests through Transitous or another public hosted service.

A provider connection is made only when a Journey request is submitted; the backend can start while MOTIS is unavailable.

## Request mapping

The adapter maps the provider-neutral request to `/api/v6/plan` with:

- `fromPlace` / `toPlace` as `latitude,longitude`;
- explicit offset-aware `time`;
- `arriveBy=true` only for Orientation `ARRIVE_BY`;
- `preTransitModes=WALK`;
- `postTransitModes=WALK`;
- an empty `directModes` set for this public-transit slice;
- `detailedLegs=true`;
- `detailedTransfers=true`;
- `maxItineraries=8`;
- `realtimeMode=REALTIME`;
- an explicit accepted transit-mode subset.

URI values are expanded as encoded template variables. In particular, a timezone offset such as `+01:00`/`+02:00` must reach MOTIS as data, not be interpreted as a query-space character.

### Accepted MOTIS transit modes

The initial request asks MOTIS only for:

- `TRAM`
- `SUBWAY`
- `FERRY`
- `BUS`
- `COACH`
- `HIGHSPEED_RAIL`
- `LONG_DISTANCE`
- `NIGHT_RAIL`
- `REGIONAL_RAIL`
- `SUBURBAN`
- `FUNICULAR`
- `AERIAL_LIFT`

Shared/rental/ODM/ride-sharing modes are not requested by the v0.5 public-transit provider slice.

## Response translation

MOTIS itineraries are translated before they cross `JourneyPort`.

Provider modes normalize to Orientation modes:

- `WALK` -> `WALK`
- `TRAM` -> `TRAM`
- `SUBWAY` -> `SUBWAY`
- `FERRY` -> `FERRY`
- `BUS` -> `BUS`
- `COACH` -> `COACH`
- `SUBURBAN` / `METRO` -> `SUBURBAN_RAIL`
- rail-family modes -> `RAIL`
- funicular/aerial-lift modes -> `OTHER_TRANSIT`

Unsupported modes are rejected rather than silently reclassified as public transport.

Transit service presentation prefers MOTIS `displayName`, then `routeShortName`, then an Orientation generic mode label. Provider route/trip identifiers are not exposed by the Journey HTTP response.

Stops are translated to provider-neutral names and Coordinates. Intermediate stops are bounded by the Journey domain limit.

## Scheduled and realtime information

MOTIS `scheduledStartTime` / `scheduledEndTime` always populate Orientation scheduled timing.

When a MOTIS leg is marked realtime, `startTime` / `endTime` populate the optional realtime-adjusted values. When it is not marked realtime, effective timing remains the scheduled timing.

The adapter therefore supports realtime-aware Journey results without claiming complete realtime coverage for a city, operator or dataset.

## Geometry

Detailed MOTIS leg geometry is accepted as encoded polyline only when the provider reports precision `6`, matching the reviewed API-v6 contract.

The adapter decodes geometry before it crosses the infrastructure boundary and enforces the Orientation maximum of 10,000 Coordinates per leg.

Encoded provider polylines never become Journey-domain state.

## Bounds and failure mapping

Provider responses are bounded to 4 MiB. For unknown content length, the adapter reads at most `MAX + 1` bytes before rejecting the response.

Although MOTIS may return slightly more than the requested `maxItineraries`, Orientation retains at most the accepted eight Journey alternatives.

Stable failure mapping:

- HTTP `429` -> `RATE_LIMITED`
- HTTP `408` / `504` -> `TIMEOUT`
- provider rejection of an otherwise valid Orientation request -> `INVALID_PROVIDER_RESPONSE`
- other non-success provider availability errors -> `PROVIDER_UNAVAILABLE`
- successful response with zero itineraries -> `NO_JOURNEY_FOUND`
- malformed JSON, invalid modes/times/stops/geometry/domain invariants -> `INVALID_PROVIDER_RESPONSE`

Raw MOTIS errors and DTOs are never returned through the Orientation HTTP contract.

## Deterministic self-hosted acceptance

The repository contains `MOTIS Journey Smoke` as a deterministic real-provider gate.

It pins:

- MOTIS `v2.11.0` Linux archive by the SHA-256 above;
- `motis-project/test-data` commit `e2a596045675e12760d77db991b57f1979a998e6`;
- Aachen OSM blob `d4f8a764450637f25a687ba2444914a13b087cab`;
- AVV GTFS blob `8dd7acedd31f961217bf69e4e8bf7d5dae4c8c97`.

The smoke verifies downloaded fixture bytes with Git blob identity, derives an active Aachen-city trip deterministically from the pinned GTFS and configures MOTIS' timetable window to that fixture service date rather than the CI runner's current date.

It then proves the real chain:

```text
pinned OSM + GTFS
      -> MOTIS config/import/server
      -> Orientation backend
      -> POST /api/v1/journeys
      -> provider-neutral Journey alternatives
```

Acceptance requires at least one actual transit leg, retained scheduled timing and decoded provider-neutral geometry. It also rejects leakage of MOTIS/Transitous/provider IDs and sharing semantics through the Orientation result.

This gate is self-hosted and does not call Transitous.

## Transitous boundary

Transitous may be configured deliberately for bounded manual/reference verification because it operates a public MOTIS service and aggregates useful open transit datasets.

It is **not**:

- the Orientation default;
- a deterministic CI/release dependency;
- an implied privacy-equivalent substitute for a local MOTIS process.

Hosted Journey requests leave the local process. Usage policy, identifying User-Agent/contact expectations, attribution and the provider's privacy/logging policy must therefore be respected whenever Transitous is selected explicitly.

## Non-goals of Issue #41

This provider slice does not add:

- GBFS/shared mobility planning;
- rental/ODM/ride-sharing modes;
- fares or ticketing;
- booking/operator actions;
- multimodal sharing + transit;
- a complete realtime-coverage claim;
- Vocation integration;
- Journey map rendering or standalone Journey UI.

Those remain separate product slices.

## Primary provider references

- MOTIS repository: https://github.com/motis-project/motis
- MOTIS v2.11.0: https://github.com/motis-project/motis/releases/tag/v2.11.0
- MOTIS setup/configuration: https://github.com/motis-project/motis/blob/v2.11.0/docs/setup.md
- MOTIS OpenAPI: https://github.com/motis-project/motis/blob/v2.11.0/openapi.yaml
- MOTIS test data: https://github.com/motis-project/test-data
- Transitous API policy: https://transitous.org/api/
- Transitous privacy policy: https://transitous.org/privacy/
