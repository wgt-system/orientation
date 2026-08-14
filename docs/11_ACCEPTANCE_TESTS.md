# Orientation – Acceptance Tests

**Status:** Bootstrap baseline plus Issues #1–#4 map-surface acceptance evidence; v0.1.0 release approval remains a control-plane decision.

## Architecture invariants

1. Orientation domain code can compile/test without Spring, MapLibre, Valhalla or foreign domain packages.
2. No Orientation code directly reads/writes a Vocation, Illumination, WGT or Conveyance database.
3. Map surface public types contain no Vocation-specific domain names.
4. Map surface public contract types contain no MapLibre implementation objects.
5. Routing provider implementation does not leak Valhalla response types across the application boundary.
6. Current location is not persisted by default.
7. Host interaction rather than core renderer code owns product navigation/external-resource execution.

## Bootstrap checks

- Java backend builds on Java 25.
- Spring application context test passes.
- TypeScript strict typecheck passes.
- Map reference host builds.
- Map model tests pass.
- CI runs backend and map checks independently.
- `git diff --check` is clean.

## First renderer proof

Given three generic Spatial Features:

- map initializes successfully;
- all features can be represented;
- selecting a feature emits its opaque feature ref;
- rich resources/actions can be presented without provider-specific code;
- the map can be destroyed/recreated without leaking state.

Issue #1 additionally requires deterministic repeated scene replacement, empty-scene clearing,
feature/source identity selection events, generic empty/focus/fit/preserve viewport resolution,
explicit renderer lifecycle states, and no duplicate marker handlers after updates.

Issue #2 additionally requires validated generic information/resources/actions, duplicate and
unsafe-URI rejection, immutable rich snapshots, opaque resource/action activation events,
keyboard-accessible text-only details controls, stale-detail replacement cleanup, and host-owned
resource/action execution.

Issue #3 additionally requires host-supplied immutable PositionFix validation, independent
set/update/clear behavior, geographic accuracy visualization, no automatic viewport following,
no retained location history, and deterministic cleanup across renderer lifecycle transitions.

Issue #4 acceptance evidence includes the validated `orientation.host-bridge` 1.0
JSON envelope/schema, independently testable protocol core, deterministic bridge/map
lifecycle, separate Embed Host artifact, malformed-message rejection, timestamp/
accuracy/antimeridian hardening, 500-feature sanity fixture, and Reference/Embed browser smoke.

## Future integration gates

### Vocation

- Vocation can consume Orientation geocoding without transferring Work Location/Precision authority.
- rich Vocation spatial projection supports required external resources.
- Vocation reference map no longer needs its own generic Leaflet implementation after migration.

### WGT Windows

- WGT can host the Orientation map surface;
- selection/resource/action events reach the WGT presentation adapter;
- WGT shell/navigation remains WGT-owned.

### WGT iPhone

- same renderer capability works on a physical iPhone host;
- touch/pan/zoom/selection are usable;
- lifecycle/reload behavior is correct;
- current-position input works when host permission is granted.

Legacy renderer deletion occurs only after the relevant gates pass.
