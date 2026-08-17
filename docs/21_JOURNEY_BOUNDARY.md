# Orientation – Public-Transit Journey Boundary

**Status:** v0.5.0 Issue #40 provider-neutral boundary.

ADR-0008 defines the accepted architectural split: the released direct `Route` model remains the owner of `DRIVING`, `CYCLING` and `WALKING`; public transport is represented independently as a time-dependent `Journey`.

## Request

`JourneyRequest` contains:

- origin `Coordinate`;
- destination `Coordinate`;
- `JourneyTimeMode`: `DEPART_AT` or `ARRIVE_BY`;
- an offset-aware ISO-8601 time (`OffsetDateTime`).

The HTTP boundary is:

```text
POST /api/v1/journeys
```

Example request:

```json
{
  "origin": { "longitude": 10.0, "latitude": 53.5 },
  "destination": { "longitude": 10.2, "latitude": 53.6 },
  "timeMode": "DEPART_AT",
  "time": "2026-08-17T22:00:00+02:00"
}
```

A local wall-clock value without an offset is rejected. The host/product UI is responsible for translating local user input into an explicit offset-aware instant.

## Result model

`JourneyPlan` contains one to eight Journey alternatives.

A `Journey` contains:

- one to 64 ordered legs;
- at least one transit leg;
- a non-negative transfer count consistent with its transit legs;
- departure/arrival/duration derived from the effective timing of its first/last legs.

A `JourneyLeg` contains:

- provider-neutral mode;
- origin/destination `JourneyStop`;
- departure/arrival `JourneyEventTime`;
- optional transit-service presentation for transit legs;
- optional bounded `JourneyLegGeometry`;
- bounded intermediate transit stops.

Initial provider-neutral modes are:

- `WALK`
- `RAIL`
- `SUBURBAN_RAIL`
- `SUBWAY`
- `TRAM`
- `BUS`
- `COACH`
- `FERRY`
- `OTHER_TRANSIT`

They are Orientation semantics, not aliases that expose one provider's enum.

## Scheduled and realtime timing

`JourneyEventTime` always has a scheduled time and may additionally contain a realtime-adjusted time.

- if no realtime-adjusted time exists, the scheduled value is effective;
- if a realtime-adjusted value exists, it is effective while the scheduled value remains available;
- absence of realtime information is normal and must not be presented as an error or as evidence that a service is exactly on time.

The first boundary does not define universal disruption/cancellation semantics. Provider-specific realtime details remain infrastructure concerns until a concrete product requirement justifies a provider-neutral extension.

## Bounds

The initial defensive bounds are:

- at most 8 Journey alternatives per response;
- at most 64 legs per Journey;
- at most 128 intermediate stops per leg;
- at most 10,000 decoded coordinates per leg geometry.

Collections are immutable/copy-safe after construction.

## Application boundary

```text
JourneyRequest
    -> JourneyService
    -> JourneyPort
    -> JourneyPlan
```

Stable Journey provider failures are:

- `PROVIDER_UNAVAILABLE`
- `TIMEOUT`
- `RATE_LIMITED`
- `INVALID_PROVIDER_RESPONSE`
- `NO_JOURNEY_FOUND`

The HTTP host maps these to stable Orientation error codes and never returns provider error bodies.

Issue #40 deliberately supplies no production `JourneyPort` bean. `JourneyController` is activated only when a port exists. Issue #41 supplies the first real adapter (MOTIS 2), keeping #40 independently provider-neutral and keeping application bootstrap valid before the provider slice lands.

## Non-goals

This boundary does not add:

- `TRANSIT` to `TravelProfile`;
- MOTIS or OpenTripPlanner DTOs/types;
- shared mobility / GBFS;
- fares or ticketing;
- booking/operator actions;
- turn-by-turn navigation;
- Vocation semantics;
- a new `orientation.host-bridge` version.
