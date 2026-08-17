# Orientation – Standalone Public-Transit Journey Flow

**Status:** v0.5.0 Issue #43 standalone product slice.

The standalone Orientation application composes the provider-neutral Journey boundary from Issue #40, the MOTIS adapter from Issue #41 and the reusable Journey Map Surface from Issue #42 into an end-user public-transit workflow. Direct routing remains available beside it.

## Product flow

The Navigate card reuses the same explicit origin and selected mapped discovery candidate used by direct routing.

For Public transit the user chooses:

- `DEPART_AT` or `ARRIVE_BY`;
- a local date and time.

The browser converts that local value into an offset-aware ISO-8601 timestamp before calling:

```text
POST /api/v1/journeys
```

The application does not infer a server timezone.

## Journey alternatives

Returned alternatives show:

- effective departure and arrival;
- total duration;
- transfer count;
- ordered WALK/transit legs;
- stop names;
- transit service label/headsign when present;
- scheduled timing and explicit realtime-adjusted timing when provided.

Scheduled-only data is labelled as scheduled. Missing realtime data is not presented as proof that a service is on time.

Selecting an alternative sends only the provider-neutral Journey overlay to the reusable Map Surface.

## Navigation lifecycle

Direct Route and public-transit Journey are separate application states.

- requesting a direct Route clears any Journey presentation;
- requesting a Journey clears any direct Route presentation;
- changing origin, destination, travel mode or Journey time invalidates stale navigation state;
- stale in-flight requests are aborted and sequence-guarded;
- clearing navigation removes Route/Journey presentation without changing the active discovery collection or candidate evidence;
- switching back to direct routing removes Journey alternatives and time controls.

The Map Surface itself remains capable of holding Route and Journey independently; the standalone product owns this mutually exclusive presentation policy.

## Failure states

The standalone client maps stable Orientation Journey failures to product messages for:

- invalid request;
- no Journey found;
- provider rate limiting;
- invalid provider response;
- timeout/unavailability.

Provider-specific error bodies are not exposed to the product UI.

## Acceptance

A focused production-browser smoke runs against the same pinned self-hosted MOTIS v2.11.0/Aachen OSM+GTFS fixture used by the provider acceptance gate. It:

1. imports a deterministic mapped discovery destination through the standalone UI;
2. selects an origin through a deterministic local Photon stub;
3. chooses Public transit and the pinned fixture time;
4. obtains real Journey alternatives through Orientation and self-hosted MOTIS;
5. renders a selected Journey on the Map Surface;
6. verifies mode switching clears Journey presentation without losing discovery state;
7. requests another Journey and verifies explicit clear preserves discovery state.

No public Transitous request is required for this acceptance path.

## Non-goals

This slice does not add:

- shared mobility/GBFS;
- fares, ticket purchase or booking;
- turn-by-turn live navigation;
- Vocation-specific behavior;
- automatic provider selection;
- a new `orientation.host-bridge` version.