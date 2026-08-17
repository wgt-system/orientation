# Orientation – Discovery Import and Persistence

**Status:** v0.4.0 application/infrastructure slice for Issue #22.

## Purpose

The first standalone Orientation product needs durable local discovery collections. A validated `orientation.spatial-research-bundle` 1.0 can therefore be translated into Orientation-owned state and reopened after restart.

The accepted flow is:

```text
external JSON
  -> SpatialResearchBundleValidator
  -> SpatialResearchBundleTranslator (ACL)
  -> DiscoveryCollection
  -> DiscoveryRepository
  -> local SQLite
```

The Research Bundle remains an acquisition contract. The database stores Orientation domain state, not raw external JSON.

## Import report

Every import returns one of:

- `CREATED` — a new collection was persisted;
- `UNCHANGED` — the same canonical bundle was already imported and the existing collection is returned;
- `REJECTED` — contract/semantic validation failed and no repository mutation was attempted.

A successful report contains the effective collection id and candidate/source counts. A rejected report contains deterministic validation errors.

## Re-import identity

Contract-local `candidateRef` values never become cross-import identities.

The complete accepted bundle is canonicalized after JSON parsing:

- object property names are sorted;
- whitespace is irrelevant;
- array order is retained;
- the canonical representation is SHA-256 fingerprinted.

The fingerprint has a unique database constraint. Re-importing the same semantic bundle therefore returns the previously stored collection as `UNCHANGED` instead of creating a duplicate.

Changing researched data produces a new collection in this baseline. Issue #22 does not invent mutable merge/update semantics between research runs.

Within a stored candidate, only the Contract 1.0 identity hints are retained:

- canonical HTTPS URI;
- explicit provider/external identifier pairs.

Similar names, addresses, coordinates or free text never trigger automatic cross-collection merging.

## Persisted model

SQLite migration V001 stores:

- discovery collections and question/area snapshot;
- ordered criteria;
- ordered research sources and retrieval timestamps;
- ordered candidates;
- canonical URI and external identity hints;
- researched location and its source references;
- one claim per criterion;
- claim status/basis;
- typed scalar observed values;
- claim notes and source references.

Provider-backed Orientation Places are not stored as if they were researched facts. `researchedLocation` remains explicitly external evidence.

## Transaction and migration behavior

A new collection plus all dependent rows is written in one SQLite transaction with foreign keys enabled. Failure rolls back the transaction.

The repository initializes an explicit Orientation schema-migration table and applies `V001__discovery_collections.sql` exactly once. A database with a newer schema version than the runtime supports is rejected rather than silently downgraded.

## Local database

Default path:

`./data/orientation.db`

Override:

`ORIENTATION_DATABASE_PATH`

`data/` is ignored by Git because authoritative personal discovery data is runtime state, not repository content.

## Host boundary

The v0.4 backend exposes:

- `POST /api/v1/discovery/imports`
- `GET /api/v1/discovery/collections`
- `GET /api/v1/discovery/collections/{collectionId}`

The detail boundary returns Orientation read DTOs. It does not expose SQL rows, JDBC types or the original external bundle.

## Explicit non-goals

- no mutable cross-import merge workflow;
- no fuzzy identity matching;
- no cross-device synchronization;
- no cloud/server database;
- no Vocation/Illumination/WGT records;
- no provider-cache persistence;
- no LLM execution or crawling;
- no transit/sharing/multimodal state.
