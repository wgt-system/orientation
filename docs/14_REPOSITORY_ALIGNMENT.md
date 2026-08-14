# Orientation – Repository Alignment

**Status:** Initial architecture alignment completed; remaining findings are future owning-repository work

This file records cross-repository work that must be executed in the owning repository/control plane. Orientation workers must not opportunistically edit foreign repositories unless the user explicitly authorizes that cross-project scope.

## wgt-system/architecture

Completed in `wgt-system/architecture` commit
`db754123ef632d4f6929afe0f62e9a588ae60a47`, system ADR-0003:

- add Orientation as accepted fifth bounded context;
- assign generic geospatial/map/geocoding/routing capability ownership;
- remove Shared Map from unresolved/hypothetical capability status;
- system ADR for Orientation ownership and cross-context interaction;
- update Service Catalog, Capability Catalog, System Context, Integration Policy, Architecture Principles, README, agent template and derived Structurizr model.

The Architecture Control Plane now records Orientation as the accepted fifth bounded context
and generic geospatial owner. The findings below remain future work in their owning repositories.

## wgt-system/vocation

Known legacy/migration findings:

1. `docs/adr/0005-map-projection.md` correctly preserves Vocation ownership of fachlich correct Map Projection, Work Location and Precision. It does **not** prohibit rich external resources.
2. `docs/09_READ_MODELS.md` later introduced a URL-free MapProjection rule. This is not a system invariant and should be superseded.
3. `Published Map Projection 1.0` is closed/frozen and therefore should not be silently mutated. Create a successor version for rich map composition; keep 1.0 only as a temporary compatibility bridge and remove it after all consumers migrate.
4. Current React Leaflet map is generic renderer duplication and a migration target.
5. Current Vocation Nominatim geocoder is generic geocoding duplication and a migration target once Orientation provides a replacement.
6. Current map UI separately fetches External Links per Opportunity. The successor spatial publication/composition should avoid this unnecessary map N+1 pattern where domain/privacy rules permit.
7. Vocation must remain authoritative for Work Location, Precision, Opportunity, Company, Posting, External Link selection/meaning, Availability and other job-domain semantics.

## wgt-system/wiiii-got-this

Known migration findings:

- existing Mapsui generic renderer is a migration target;
- WGT remains product composition/presentation owner;
- WGT should adapt provider data into Orientation scenes rather than make Orientation aware of Vocation internals;
- embedded Orientation map surface must be reconciled explicitly with WGT presentation ADRs;
- do not delete Mapsui before Windows + physical-iPhone Orientation gates pass.

## wgt-system/illumination

No required immediate implementation change.

Future spatial learning scenarios must use explicit Orientation boundaries rather than introduce a separate generic map stack.

## wgt-system/conveyance

No change required.

Orientation must not duplicate durable cross-device relay behavior. Conveyance remains opaque to foreign business semantics.
