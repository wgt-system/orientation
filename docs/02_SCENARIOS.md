# Orientation – Scenarios

**Status:** Bootstrap baseline

## S1 — Render provider-owned spatial features

A host supplies a set of spatial features with positions, summaries, resources/actions and opaque provider references. Orientation renders them, supports selection/hit testing and emits generic interaction events.

Orientation does not need to know what a Vocation Opportunity means.

## S2 — Rich Vocation marker

Vocation publishes domain-correct opportunity/location information including external-resource metadata where appropriate. A Vocation reference host or WGT adapter maps it into Orientation's generic spatial scene.

The marker can expose:

- title/company/location;
- domain-owned status information supplied by Vocation;
- external posting/company/review resources;
- a generic "route here" action;
- an opaque "open provider item" action.

External-link business meaning remains Vocation-owned. Product navigation remains host-owned.

## S3 — Geocode a Vocation Work Location

Vocation has a Work Location string whose geographic resolution is missing.

Vocation calls an Orientation geocoding capability through an explicit adapter. Orientation returns a generic geocoding result. Vocation decides how that result affects its own Work Location resolution/precision state.

```text
Vocation -> Orientation geocode -> Vocation interpretation/state
```

## S4 — Route from current position to a selected provider object

WGT obtains current position through the platform permission/location adapter, selects a Vocation spatial destination, and asks Orientation for a route.

```text
Vocation destination ----\
                          -> WGT composition -> Orientation routing/rendering
Host current position ---/
```

Vocation need not know routing semantics when it does not interpret the route.

## S5 — Current location visualization

A host with permission supplies a position fix and accuracy. Orientation renders the position/accuracy and can use it as a routing origin.

Orientation does not own the permission prompt and does not persist location history by default.

## S6 — Place discovery

A user searches or browses nearby places through an accepted provider-backed Orientation slice. Orientation returns generic Place results with provenance/provider facts needed for technical/legal behavior.

No consuming domain gains authority over the external provider dataset merely by displaying it.

## S7 — Multiple runtime forms

The map surface may run embedded in a browser/WebView while routing/geocoding uses a Java Orientation backend and Valhalla may run as a separate external process/container.

This remains one bounded context with multiple justified runtime artifacts.

## S8 — Provider unavailable

Geocoding/routing/place providers can fail independently. Orientation returns explicit technical failure/unavailability states; it does not invent foreign domain fallback meaning.
