# Orientation – Standalone Public-Transit Journey Flow

**Status:** v0.5.0 Journey product flow with post-v0.5 local-first and standalone-usability hardening.

The standalone Orientation application composes the provider-neutral Journey boundary, local MOTIS adapter and reusable Journey Map Surface into an end-user public-transit workflow. Direct routing remains available beside it.

## Immediate navigation

Research is optional for navigation.

A user can open Orientation and immediately:

1. choose `Use my location` or search a Start through the local MOTIS-backed Place boundary;
2. search a Destination through the same local Place boundary, or select a mapped Discovery Candidate;
3. choose Driving, Cycling, Walking or Public transit;
4. request a Route or Journey.

No Spatial Research Bundle, Discovery Collection or imported candidate is required for the ad-hoc flow.

`Use my location` is an explicit one-shot browser action. Orientation does not request location permission on page load, does not reverse-geocode the fix, does not call an external geolocation API, and does not persist or continuously watch position history. The existing Map Surface current-position layer renders the returned coordinate and browser-reported accuracy.

Manual Start search replaces the current-location origin. A mapped Discovery Candidate remains another valid destination source and replaces the current navigation destination while keeping its evidence available.

## Reachability and layout

Navigate is a first-class workspace area, not a control hidden below the map.

- the header provides direct Research / Collections / Navigate jumps;
- on wide desktop layouts, the long workspace columns scroll independently inside the viewport;
- Navigate is placed before the map in the navigation/map column;
- on narrow/mobile layouts, Navigate + map are presented before Research and Collections, while all three sections remain directly reachable through the sticky jump navigation;
- responsive browser support does not imply that a full local MOTIS/Valhalla dataset is installed on the phone.

## Client loading

The standalone navigation shell does not synchronously load the reusable Map Surface.

- the form, Start/Destination search, current-location action and other application controls initialize first;
- after the first browser paint, a standalone loading boundary dynamically imports MapLibre, its worker/CSS and the existing Map Surface;
- only then does the Map Surface request the hosted OpenFreeMap style/vector resources needed for the visible map;
- the loading boundary retains the latest Scene, Route, Journey and current-position presentation state while the map module is pending and replays it when the real surface is available;
- map loading failure is a visible map failure and does not disable the local Place/Route/Journey application APIs;
- Reference Host and Embed Host keep their existing eager Map Surface behavior because this optimization is specific to the end-user standalone startup path.

This is a first-load optimization, not an offline-map guarantee. Browser HTTP caching may make repeated asset/tile loads cheaper, but new/evicted map resources still require network access to OpenFreeMap.

## Public-transit controls

For Public transit the user chooses:

- `DEPART_AT` or `ARRIVE_BY`;
- a local date and time.

The browser converts that local value into an offset-aware ISO-8601 timestamp before calling `POST /api/v1/journeys`.

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

- changing either selected endpoint invalidates stale navigation presentation;
- requesting a direct Route clears Journey presentation;
- requesting a Journey clears direct Route presentation;
- changing mode or Journey time invalidates stale navigation state;
- stale in-flight requests are aborted and sequence-guarded;
- clearing navigation removes Route/Journey presentation but retains the chosen Start and Destination for another request;
- Discovery state is independent of the ad-hoc endpoint state;
- switching from current location to a searched Start clears the current-position presentation;
- switching back to direct routing removes Journey alternatives/time controls.

The Map Surface itself remains capable of holding Route and Journey independently; the standalone product owns the mutually exclusive presentation policy.

## Failure states

Stable product messages cover invalid request, no Journey found, provider rate limiting, invalid provider response, timeout and unavailability. Provider-specific error bodies are not exposed.

Browser current-location failures are local UI states: permission denied, unavailable, timeout or invalid fix. They do not trigger a hosted fallback.

If local MOTIS is unavailable, Orientation fails visibly; it does not silently forward Place/Journey requests to Transitous, Photon or another hosted provider.

## Acceptance

The production-browser MOTIS smoke uses pinned self-hosted MOTIS v2.11.0/Aachen OSM+GTFS and proves the shortest product path:

1. verifies desktop/mobile workspace reachability;
2. starts with no imported Discovery Candidate;
3. injects a deterministic browser geolocation fix and selects it through the real `Use my location` control;
4. verifies the selected Start exposes browser-reported accuracy;
5. searches and selects a destination through real local MOTIS geocoding;
6. chooses Public transit and the pinned fixture time;
7. obtains real Journey alternatives through Orientation and self-hosted MOTIS;
8. renders a selected Journey;
9. verifies mode switching and explicit clear while retaining the selected endpoints.

The existing Valhalla/v0.4 regression separately continues to prove Discovery import/restart/reopen and candidate-to-direct-Route behavior.

No public Transitous, Photon or external geolocation request is required.

## Non-goals

- continuous position watching/tracking;
- location history;
- arbitrary waypoints;
- saved/favorite routes or Journey history;
- shared mobility/GBFS;
- fares, ticket purchase or booking;
- turn-by-turn live navigation;
- automatic provider selection/fallback;
- phone-local large-dataset packaging;
- offline/PWA map packaging;
- a new `orientation.host-bridge` version.
