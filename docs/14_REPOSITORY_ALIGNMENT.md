# Orientation – Repository Alignment

**Status:** Architecture alignment complete; remaining items are owning-repository migration/future-work concerns, not Orientation v0.4.0 release blockers.

This file records cross-repository facts relevant to Orientation. Orientation workers must not opportunistically edit foreign repositories unless the user explicitly authorizes that cross-project scope.

## wgt-system/architecture

Completed in `wgt-system/architecture` commit
`db754123ef632d4f6929afe0f62e9a588ae60a47`, system ADR-0003:

- Orientation is an accepted bounded context;
- generic geospatial/map/geocoding/routing capability ownership belongs to Orientation;
- Shared Map is no longer unresolved/hypothetical capability space;
- system context, integration policy and architecture catalogs reflect Orientation ownership.

The later system decision also recognizes Orientation's independently useful product role and its domain-owned research/import workflow.

## wgt-system/vocation

Vocation remains authoritative for Work Location, Precision, Opportunity, Company, Posting, External Link selection/meaning, Availability and all other job-market semantics.

Known owning-repository migration concerns remain separate from the Orientation v0.4.0 release:

1. `docs/adr/0005-map-projection.md` preserves Vocation ownership of fachlich correct Map Projection, Work Location and Precision; this does not prohibit rich external resources.
2. The later URL-free MapProjection restriction is not a system invariant and may be superseded by an explicit successor publication contract.
3. `Published Map Projection 1.0` is closed/frozen and must not be silently mutated; a successor is required if richer composition becomes necessary.
4. Vocation's generic Leaflet renderer and generic Nominatim geocoder remain migration targets where/when the owning repository accepts replacement work.
5. Orientation v0.4.0 does not claim Vocation migration completion.

## wgt-system/wiiii-got-this

Current verified state:

- WGT remains product composition/presentation owner;
- Desktop already hosts the accepted Orientation map surface through the Vocation Map Projection -> WGT adapter -> Orientation scene boundary;
- the former Mapsui fallback has been removed from the completed Desktop integration;
- `orientation.host-bridge` remains version 1.0 and the v0.4.0 work does not change that consumer contract;
- physical iPhone validation is not established; the corresponding WGT work is intentionally deferred/not planned while Desktop product quality has priority and no physical Apple validation environment is available.

The lack of physical-iPhone evidence is therefore not an Orientation v0.4.0 release blocker and must not be misrepresented as released iPhone support.

## wgt-system/illumination

No required immediate implementation change.

Future spatial learning scenarios must use explicit Orientation boundaries rather than introduce a separate generic map stack.

## wgt-system/conveyance

No change required.

Orientation must not duplicate durable cross-device relay behavior. Conveyance remains the owner of generic durable opaque delivery and stays opaque to foreign business semantics.
