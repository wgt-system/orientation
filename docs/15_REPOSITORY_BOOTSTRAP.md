# Orientation – Repository Bootstrap Specification

**Status:** Completed historical bootstrap record

## Repository

Canonical repository/folder:

`wgt-system/orientation`

Canonical product/context name:

`Orientation`

Canonical local path:

`P:\wgt-system\orientation`

## Branch model

- `main` — stable accepted baseline/milestone state.
- `dev` — active integrated development.

Initial bootstrap may commit the accepted scaffold to `main`, push it, create `dev` from that commit and continue on `dev`.

## Toolchain

Backend:

- Java 25 LTS
- Spring Boot 4.1.x
- Maven 3.9.x wrapper

Map:

- Node.js 24 LTS
- TypeScript
- Vite
- Vitest
- MapLibre GL JS 6

Routing:

- Valhalla upstream runtime; no vendored source at bootstrap.

## Initial repository shape

```text
backend/
map/
contracts/
deployment/valhalla/
docs/
scripts/
.github/
```

No database is bootstrapped until a concrete persistence need exists.

## Initial backend

The bootstrap backend proves:

- Java/Maven build;
- Spring Boot executable host;
- application-context test;
- dependency/package seams for domain/application/infrastructure/host.

It must not invent geocoding/routing APIs before those slices are designed.

## Initial map surface

The bootstrap map proves:

- TypeScript strict build;
- MapLibre initialization;
- generic Spatial Feature model;
- reusable map-surface wrapper;
- feature selection callback;
- simple rich resource/action reference UI;
- standalone reference host;
- unit test for non-DOM scene/model behavior.

The reference host is not the WGT product UI.

## CI

CI runs backend and map jobs independently.

## Historical GitHub bootstrap

The initial worker performed the following steps:

1. unpack the bootstrap ZIP into `P:\wgt-system\orientation`;
2. read `.bootstrap/LUNA_BOOTSTRAP.md` and all repository docs;
3. validate/update dependency versions only when required for a working current stable toolchain;
4. generate Maven wrapper pinned to Maven 3.9.x;
5. generate `map/package-lock.json` from the pinned package manifest;
6. run all checks;
7. remove `.bootstrap/` and the bootstrap ZIP before staging;
8. `git init -b main`;
9. commit the clean baseline;
10. create/push public `wgt-system/orientation` with local authenticated `gh`, or attach the existing remote if already created;
11. create `dev` from the accepted baseline and push it;
12. remain on `dev`;
13. return remote URL, main/dev SHAs and validation results.

Do not create Issues/Milestones in this bootstrap run. They are created after the pushed repository is reviewed.

## Completion result

The bootstrap completed with:

- remote `wgt-system/orientation` exists and is readable;
- `main` and `dev` exist remotely;
- backend tests/build pass;
- map tests/typecheck/build pass;
- reference map renders when run locally;
- no ZIP/bootstrap temp files are tracked;
- documentation agrees on ownership/non-ownership;
- no foreign repository was modified implicitly.
