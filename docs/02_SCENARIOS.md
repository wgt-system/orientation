# Orientation – Scenarios

**Status:** Post-v0.3 product scenarios; future contracts are not frozen by this document.

## S1 — Standalone spatial research workflow

A user starts in Orientation with a spatial question and explicit criteria, for example finding nearby places that satisfy facts not reliably available through an ordinary map provider.

Orientation prepares a domain-specific research prompt. The user runs that prompt in an external ChatGPT/research interaction and receives structured versioned JSON.

Orientation validates the artifact, rejects invalid/incompatible input, translates accepted data into Orientation-owned state, preserves relevant provenance, and presents the resulting candidates in a useful list/map workflow.

No paid LLM API or automatic crawling is required.

## S2 — Explore an Orientation-owned discovery collection

A user opens a previously imported or assembled collection of spatial candidates.

Orientation can:

- show candidates on the map and in a list;
- expose source-aware researched information;
- filter/sort using accepted Orientation-owned fields/criteria;
- select a candidate;
- compare useful spatial/travel context;
- initiate a route to the selected destination.

This is Orientation's own product workflow, not provider-domain rendering.

## S3 — Navigate from current or explicit position

A host supplies current position after handling platform permission, or the user selects an explicit origin.

Orientation determines and renders a route to the selected destination using an accepted travel profile/provider boundary.

The v0.3.0 baseline supports DRIVING/CYCLING/WALKING. Public transit, shared mobility and multimodal journeys require later explicit slices.

## S4 — Direct place discovery

A user searches or browses nearby places through an accepted provider-backed Orientation capability.

Orientation returns generic Place results with provider/provenance facts required for technical/legal behavior and can turn selected results into Orientation exploration/navigation state.

No external provider dataset becomes Orientation-authored truth merely because Orientation displays it.

## S5 — Render provider-owned spatial features

A host supplies a set of spatial features with positions, summaries, resources/actions and opaque provider references. Orientation renders them, supports selection/hit testing and emits generic interaction events.

Orientation does not need to know what a Vocation Opportunity means.

## S6 — Rich Vocation marker

Vocation publishes domain-correct opportunity/location information including external-resource metadata where appropriate. A Vocation reference host or WGT adapter maps it into Orientation's generic spatial scene.

The marker can expose:

- title/company/location;
- domain-owned status information supplied by Vocation;
- external posting/company/review resources;
- a generic "route here" action;
- an opaque "open provider item" action.

External-link business meaning remains Vocation-owned. Product navigation remains host-owned.

## S7 — Geocode a Vocation Work Location

Vocation has a Work Location string whose geographic resolution is missing.

Vocation calls an Orientation geocoding capability through an explicit adapter. Orientation returns a generic geocoding result. Vocation decides how that result affects its own Work Location resolution/precision state.

```text
Vocation -> Orientation geocode -> Vocation interpretation/state
```

## S8 — Route to a selected provider object

WGT obtains current position through the platform permission/location adapter, selects a Vocation spatial destination, and asks Orientation for a route.

```text
Vocation destination ----\
                          -> WGT composition -> Orientation routing/rendering
Host current position ---/
```

Vocation need not know routing semantics when it does not interpret the route.

## S9 — Current location visualization

A host with permission supplies a position fix and accuracy. Orientation renders the position/accuracy and can use it as a routing origin.

Orientation does not own the permission prompt and does not persist location history by default.

## S10 — Future public-transit/shared-mobility planning

A user asks for useful mobility options from an origin to a destination where walking/cycling/driving alone are insufficient.

A future accepted Orientation slice may combine provider-neutral transit, time, realtime and shared-mobility information to produce one or more route alternatives.

Such a slice must define its own explicit semantics and provider/data boundaries. It must not pretend that transit or shared mobility is merely another Valhalla v0.3 costing string.

## S11 — Multiple runtime forms

Orientation may have:

- a first-class standalone end-user application;
- a browser/reference acceptance host;
- an embeddable map surface used by WGT or another host;
- a Java application/backend runtime;
- external provider/runtime dependencies such as Valhalla.

This remains one bounded context with multiple justified runtime artifacts.

## S12 — Provider unavailable

Geocoding, map, routing, transit or other mobility providers can fail independently. Orientation returns explicit technical failure/unavailability states and preserves its own local state where possible.

It does not convert a technical provider outage into foreign-domain meaning such as "job unavailable" or silently fabricate research facts.
