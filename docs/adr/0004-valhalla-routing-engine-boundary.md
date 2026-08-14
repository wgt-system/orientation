# ADR-0004: Valhalla routing engine boundary

- **Status:** Accepted
- **Date:** 2026-08-14

## Decision

Use Valhalla as the initial upstream routing engine behind an Orientation-owned adapter.

Communicate through a technical runtime boundary such as its supported HTTP interface rather than importing Valhalla C++ domain objects into Orientation.

## Consequences

- Orientation owns generic route request/result semantics exposed to consumers.
- Valhalla can be updated/replaced without changing foreign domain ownership.
- Valhalla source is not vendored/forked into this repository by default.
- deployment/configuration belongs under Orientation infrastructure/deployment, not a separate WGT bounded context.
