# ADR-0005: Host owns current-location permission and acquisition

- **Status:** Accepted
- **Date:** 2026-08-14

## Decision

Orientation consumes generic position fixes. The embedding host owns OS/browser permission prompts and platform-specific location acquisition.

Orientation owns validation, generic use and visualization of the supplied geographic position.

## Consequences

- WGT platform hosts keep permission/device responsibilities.
- browser reference host may use browser geolocation only through a host adapter.
- precise position history is not persisted by default.
- Orientation APIs do not require platform SDK types.
