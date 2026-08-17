# Orientation – Journey Map Surface

**Status:** v0.5.0 Issue #42 reusable presentation boundary.

The Journey overlay extends the reusable Orientation Map Surface without changing the released direct `Route` overlay. It consumes only Orientation-owned provider-neutral Journey presentation data; MOTIS DTOs and provider enums do not cross into the map package.

## Public map lifecycle

`OrientationMapSurface` now owns Journey state independently from Spatial Scene, Current Position and direct Route state:

```text
setJourney(journey, viewport?)
clearJourney()
currentJourney()
```

Setting, replacing or clearing a Journey does not implicitly mutate the Spatial Scene, Current Position or direct Route. Product hosts decide explicitly when mutually exclusive navigation presentations should be cleared.

Destroy clears the Journey controller and removes Journey-specific layers/source before the MapLibre instance is removed.

## Journey presentation model

`JourneyOverlay` contains one to 64 ordered provider-neutral legs. Each leg contains:

- one Orientation Journey mode (`WALK`, `RAIL`, `SUBURBAN_RAIL`, `SUBWAY`, `TRAM`, `BUS`, `COACH`, `FERRY`, `OTHER_TRANSIT`);
- origin and destination stops with coordinates;
- optional decoded geometry bounded to 10,000 coordinates per leg.

At least one leg must be transit. The overlay intentionally excludes timetable text, fares, booking actions, provider logos and MOTIS-specific fields.

## Rendering semantics

Journey rendering uses a dedicated GeoJSON source and dedicated MapLibre layers, separate from `orientation-route`.

- walking legs are dashed;
- transit legs are solid;
- transit boarding/alighting points are rendered as stop markers;
- Journey origin and destination are explicit point markers;
- ordered legs remain independent GeoJSON line features with `legIndex` and provider-neutral `mode` properties.

The dashed/solid distinction and explicit stop markers ensure mode/leg transitions do not rely on color alone.

## Viewport

The default Journey viewport fits all leg endpoints and all available decoded leg geometry with the same antimeridian-safe coordinate-boundary implementation used elsewhere in Orientation.

Hosts may request `preserve` instead of `fit`. Journey viewport changes do not change the stored Spatial Scene, direct Route or Current Position.

## Boundaries

This slice does **not**:

- add `TRANSIT` to the direct `TravelProfile`;
- expose MOTIS types;
- add timetable/product controls to the reusable map package;
- add shared mobility/GBFS rendering;
- change `orientation.host-bridge` 1.0;
- automatically clear an existing direct Route when a Journey is set.

The standalone product flow in Issue #43 owns the UX decision to switch between direct Route and Journey presentation.