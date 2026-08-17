# Orientation Contracts

## `orientation.host-bridge` 1.0

The accepted v0.1.0 browser/WebView host bridge is defined by
[`orientation-host-bridge-v1.schema.json`](orientation-host-bridge-v1.schema.json).
It is a narrow renderer host boundary, not an HTTP/network or backend API.
Its stable schema identifier is
`https://schemas.wgt-system.org/orientation/host-bridge/1.0/schema.json`; it
does not depend on a Git branch or mutable repository URL.

Inbound messages are JSON envelopes with `scene.replace`, `current-position.set`
or `current-position.clear` types. Outbound messages include `bridge.ready`,
`map.status`, generic feature/resource/action events and `bridge.error`.

Example inbound messages:

```json
{"contract":"orientation.host-bridge","version":"1.0","type":"scene.replace","payload":{"features":[]}}
{"contract":"orientation.host-bridge","version":"1.0","type":"current-position.clear","payload":{}}
```

The browser embed exposes one documented `window.orientationHostBridge.receive`
entry point for serialized messages and emits serialized outbound messages via
the `orientation-host-bridge-message` browser event. When embedded in an iframe,
the same serialized messages are also emitted to the parent with generic
`postMessage`; inbound parent messages are still validated by the protocol core.
The wildcard target is intentional for local-file and generic WebView embedding;
the containing parent is the authorized host boundary. Orientation does not emit
scene or current-position payloads spontaneously.
It does not expose the map surface or any provider/domain objects globally.

## `orientation.spatial-research-bundle` 1.0

The first standalone-product acquisition contract is defined by
[`orientation-spatial-research-v1.schema.json`](orientation-spatial-research-v1.schema.json).
Its stable schema identifier is
`https://schemas.wgt-system.org/orientation/spatial-research/1.0/schema.json`.

It carries one explicit radial spatial research question, its criteria, source
provenance and researched candidates. It is an external acquisition artifact,
not the persistence model and not a generic research contract shared with other
bounded contexts.

Canonical synthetic examples live under [`examples/`](examples/). Cross-field
semantics such as criterion/source references and heuristic/evidence rules are
also enforced by the backend `SpatialResearchBundleValidator` before any future
import mutation.

See `docs/17_SPATIAL_RESEARCH_CONTRACT.md` for semantic rules and boundaries.

Concrete versioned schemas belong here only after an implemented consumer/provider scenario establishes the required semantics.

See `docs/08_CONTRACTS.md`.
