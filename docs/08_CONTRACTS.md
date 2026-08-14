# Orientation – Contracts

**Status:** No public Orientation contract frozen at bootstrap.

## Principle

Do not create a broad `orientation.v1` mega-contract before concrete consumers exist.

Introduce small, explicit, versioned boundaries per concrete capability.

Expected future boundary families include:

- geocoding/reverse-geocoding application API;
- routing application API;
- place discovery API;
- map-scene / embedded-renderer boundary;
- host interaction event boundary.

## Provider-owned spatial data

Orientation does not require providers to surrender semantic ownership.

A provider such as Vocation may publish a rich provider-owned map/spatial projection. WGT or another host adapter can transform that provider contract into an Orientation Spatial Scene.

The current Vocation `Published Map Projection 1.0` being URL-free is not an Orientation or system invariant. A future provider version may include external resources and richer information when Vocation chooses.

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
