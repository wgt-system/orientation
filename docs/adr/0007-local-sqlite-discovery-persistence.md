# ADR-0007: Local SQLite persistence for Orientation discovery state

- **Status:** Accepted for the v0.4.0 discovery baseline
- **Date:** 2026-08-17

## Context

Orientation v0.1-v0.3 was intentionally stateless apart from bounded runtime/provider state. The first standalone spatial-research workflow introduces an explicit stateful requirement: imported discovery collections, their candidates and the provenance needed to understand researched claims must survive restart.

This is Orientation-owned personal application state. It is not a cache of Vocation/WGT/Illumination data and it is not remote synchronization state.

## Decision

Use one local SQLite database as the initial persistence mechanism for Orientation-owned discovery state.

The backend accesses the database through an Orientation `DiscoveryRepository` port and a plain JDBC SQLite adapter. The external Spatial Research Bundle is validated and translated through an application boundary before persistence; raw external bundle JSON is not the database model.

Schema changes use explicit ordered SQL migrations recorded in an Orientation-owned schema-migration table. Import of one discovery collection is one database transaction.

The default local path is `./data/orientation.db` and can be overridden through `ORIENTATION_DATABASE_PATH`.

## Consequences

- discovery collections survive backend restart;
- SQLite remains an internal Orientation implementation choice and is not an integration contract;
- the standalone product can list/reopen collections without ChatGPT reconstruction;
- researched claims and source provenance remain distinguishable from provider-backed Orientation `Place` results;
- a later cross-device requirement must go through the system Conveyance decision model rather than turning this SQLite database into shared remote storage;
- no ORM/domain entity leakage is introduced.

## Rejected alternatives

- **Continue stateless operation:** cannot satisfy the accepted standalone discovery workflow.
- **Persist raw Research Bundle JSON only:** would make the external acquisition contract the authoritative storage model and weaken migration/domain boundaries.
- **PostgreSQL/server database:** introduces deployment/network overhead with no current single-user local-first requirement.
- **JPA/Hibernate for the first persistence slice:** unnecessary abstraction/behavior for the small explicit relational model and migration boundary required here.
