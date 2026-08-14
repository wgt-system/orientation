# ADR-0002: Version 1 technology stack

- **Status:** Accepted
- **Date:** 2026-08-14

## Decision

Backend baseline:

- Java 25 LTS;
- Maven;
- Spring Boot 4.1.x.

Map surface baseline:

- TypeScript;
- MapLibre GL JS 6;
- ESM;
- Node.js 24 LTS tooling.

No database is selected at bootstrap.

## Rationale

The backend is primarily provider integration/orchestration and I/O-heavy service logic. Java provides a mature HTTP/service ecosystem and strong productivity without requiring low-level memory control.

TypeScript is selected for the reusable MapLibre GL JS renderer, not as the language of the whole bounded context.

## Consequences

The repository is intentionally polyglot because runtime responsibilities differ.

C++ used by Valhalla remains upstream implementation technology, not a third Orientation implementation language.
