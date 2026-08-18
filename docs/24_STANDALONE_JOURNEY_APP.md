# Orientation – Standalone Public-Transit Journey Flow

**Status:** v0.5.0 Journey product flow, with post-v0.5 standalone usability hardening.

The standalone Orientation application composes the provider-neutral Journey boundary, MOTIS adapter and reusable Journey Map Surface into an end-user public-transit workflow. Direct routing remains available beside it.

## Reachability and layout

Navigate is a first-class workspace area, not a control hidden below the map.

- the header provides direct Research / Collections / Navigate jumps;
- on wide desktop layouts, the long workspace columns scroll independently inside the viewport;
- Navigate is placed before the map in the navigation/map column;
- on narrow/mobile layouts the app returns to normal document scrolling and keeps the section jumps touch-reachable;
- responsive browser support does not imply that a full local MOTIS/Valhalla dataset is installed on the phone.

The production-browser MOTIS smoke verifies both desktop scroll reachability and the mobile document-scroll/Navigate-jump behavior.

## Product flow

The Navigate card reuses the same explicit origin and selected mapped discovery candidate used by direct routing.

Origin Place Search now goes through Orientation's local MOTIS-backed Place boundary. The app never contacts a hosted geocoder directly.

For Public transit the user chooses:

- `DEPART_AT` or `ARRIVE_BY`;
- a local date and time.

The browser converts that local value into an offset-aware ISO-8601 timestamp before calling `POST /api/v1/journeys`.

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

- requesting a direct Route clears Journey presentation;
- requesting a Journey clears direct Route presentation;
- changing origin, destination, mode or Journey time invalidates stale navigation state;
- stale in-flight requests are aborted and sequence-guarded;
- clearing navigation removes Route/Journey presentation without changing the active discovery collection or candidate evidence;
- switching back to direct routing removes Journey alternatives/time controls.

The Map Surface itself remains capable of holding Route and Journey independently; the standalone product owns the mutually exclusive presentation policy.

## Failure states

Stable product messages cover invalid request, no Journey found, provider rate limiting, invalid provider response, timeout and unavailability. Provider-specific error bodies are not exposed.

If local MOTIS is unavailable, Orientation fails visibly; it does not silently forward the request to Transitous or another hosted provider.

## Acceptance

The production-browser smoke uses the same pinned self-hosted MOTIS v2.11.0/Aachen OSM+GTFS fixture as provider acceptance. It:

1. verifies desktop/mobile workspace reachability;
2. imports a deterministic mapped discovery destination;
3. selects an origin through real local MOTIS geocoding;
4. chooses Public transit and pinned fixture time;
5. obtains real Journey alternatives through Orientation and self-hosted MOTIS;
6. renders a selected Journey;
7. verifies mode switching and explicit clear preserve Discovery state.

No public Transitous or Photon request is required.

## Non-goals

- shared mobility/GBFS;
- fares, ticket purchase or booking;
- turn-by-turn live navigation;
- automatic provider selection/fallback;
- phone-local large-dataset packaging;
- a new `orientation.host-bridge` version.
