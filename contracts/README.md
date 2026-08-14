# Orientation Contracts

## `orientation.host-bridge` 1.0

The accepted v0.1.0 browser/WebView host bridge is defined by
[`orientation-host-bridge-v1.schema.json`](orientation-host-bridge-v1.schema.json).
It is a narrow renderer host boundary, not an HTTP/network or backend API.

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
It does not expose the map surface or any provider/domain objects globally.

Concrete versioned schemas belong here only after an implemented consumer/provider scenario establishes the required semantics.

See `docs/08_CONTRACTS.md`.
