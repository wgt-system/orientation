# Orientation – Contracts

**Status:** `orientation.host-bridge` 1.0 is accepted for the v0.1.0 embedded map renderer. Other Orientation network/application contracts remain unfrozen.

## Principle

Do not create a broad `orientation.v1` mega-contract before concrete consumers exist.

Introduce small, explicit, versioned boundaries per concrete capability.

Expected future boundary families include:

- geocoding/reverse-geocoding application API;
- routing application API;
- place discovery API;
- map-scene / embedded-renderer boundary;
- host interaction event boundary.

The v0.2.0 HTTP API is a narrow Orientation-owned host boundary for the first
place capability, not a foreign-domain Published Contract and not a revision of
`orientation.host-bridge` 1.0. Its JSON responses contain generic Place DTOs;
the Photon wire shape is kept behind the infrastructure adapter.

## Accepted host bridge

`orientation.host-bridge` version `1.0` is the first concrete versioned
renderer-host contract. Its canonical JSON Schema is
[`contracts/orientation-host-bridge-v1.schema.json`](../contracts/orientation-host-bridge-v1.schema.json).
It defines a small transport-independent JSON envelope for scene replacement,
host-supplied current-position updates/clears, generic interaction events,
map lifecycle status and bridge errors. It is not an HTTP, REST, routing,
geocoding or backend service contract.

## Provider-owned spatial data

Orientation does not require providers to surrender semantic ownership.

A provider such as Vocation may publish a rich provider-owned map/spatial projection. WGT or another host adapter can transform that provider contract into an Orientation Spatial Scene.

The current Vocation `Published Map Projection 1.0` being URL-free is not an Orientation or system invariant. A future provider version may include external resources and richer information when Vocation chooses.

## Routing host boundary (v0.3.0)

Issue #9 introduces the narrow, unversioned-in-bridge HTTP host boundary
`POST /api/v1/routes`. Its request contains explicit origin and destination
Coordinates plus the generic `TravelProfile`; its response contains a
provider-neutral Route with decoded bounded geometry, distance and duration.
It does not modify `orientation.host-bridge` 1.0 or any JSON schema in this
repository. Valhalla wire JSON, provider costing names, encoded polylines and
provider identifiers remain behind the later #10 adapter.

Stable HTTP outcomes are: `400` invalid Orientation request, `404` no route,
`502` invalid provider response, `503` unavailable or timeout, and `429` rate
limited where the provider abstraction retains that distinction. Error bodies
contain only stable Orientation codes/messages.

## Contract requirements

When a contract is frozen:

- explicit capability name;
- explicit contract version;
- closed/validated shape where useful;
- opaque foreign references;
- no foreign domain classes;
- transport-independent semantics unless transport is part of the capability;
- compatibility tests on provider and consumer sides;
- failure states explicit rather than inferred;
- no MapLibre/Valhalla implementation objects across the public boundary.
