# Orientation – Architecture

**Status:** Bootstrap baseline

## Logical architecture

```text
                       Orientation bounded context
        +--------------------------------------------------+
        |                                                  |
        |  Java backend                     TS map surface  |
        |  +----------------------+        +-------------+ |
        |  | application/domain   |        | scene API   | |
        |  | provider ports       |        | MapLibre    | |
        |  +----------+-----------+        +------+------+ |
        |             |                           |         |
        +-------------|---------------------------|---------+
                      |                           |
              +-------+------+             embedded by
              | providers /  |             browser/WGT
              | Valhalla     |
              +--------------+
```

## Technology baseline

### Backend

- Java 25 LTS
- Maven
- Spring Boot 4.1.x
- stateless bootstrap
- provider interfaces at application boundary

Spring is host/infrastructure, not domain.

### Map surface

- TypeScript
- MapLibre GL JS 6
- ESM
- framework-independent reusable core
- standalone browser reference host for development/debug

The map package produces two browser artifacts: the rich Reference Host
(`index.html`) and the provider-neutral Embed Host (`embed.html`). The Embed
Host exposes only the documented Orientation Host Bridge entry point and does
not include reference/demo controls or provider data. Bridge protocol parsing
and validation are separate from the browser event transport; WebView2,
WKWebView and Avalonia/WGT adapters remain outside this repository.

Both surfaces use OpenFreeMap Liberty as the default external street basemap.
The Reference Host presents a small renderer error status when style/tile
loading fails; the Embed Host continues to report generic `map.status` and
`bridge.error` events without adding product-specific failure UI.

Do not depend on React in the reusable renderer core.

The v0.2.0 Java slice adds a stateless Photon provider adapter behind
application ports for place search and reverse geocoding. Photon is an
external, configurable provider at `https://photon.komoot.io` by default; its
availability is best-effort and its fields do not become Orientation domain
types. The Reference Host calls only `/api/v1/places/search` and
`/api/v1/places/reverse`; Vite dev/preview proxying to the local backend keeps
the browser same-origin and does not expose Photon configuration. Search is
explicit-submit only, and reverse lookup is an explicit map-center action.
No database or cache is introduced.

### Routing

Valhalla is the selected upstream routing engine candidate/baseline. Orientation owns the adapter and generic routing semantics exposed to WGT-system consumers.

Valhalla source is not copied into this repository.

## Process/deployment model

A bounded context is not a single process prescription.

Possible deployment:

```text
WGT / browser host
      |
      +-- embedded Orientation map surface
      |
      +-- Orientation Java backend (local or remote as scenario requires)
                         |
                         +-- geocode/place providers
                         +-- Valhalla runtime
```

Exact local/remote topology is decided per capability and platform.

## WGT integration gate

Before deleting the existing WGT Mapsui implementation, the Orientation map surface must prove:

1. browser/reference host;
2. WGT Windows embedded host;
3. physical iPhone embedded host.

The physical iPhone proof is a technology/runtime compatibility gate, not a reason to duplicate renderer ownership permanently.

## Legacy migration targets

- Vocation React Leaflet renderer
- Vocation Nominatim geocoder implementation
- WGT Mapsui renderer

These remain only until accepted Orientation replacements and contract migrations exist.

## Data ownership

No cross-context database access.

Technical caches/indexes introduced in Orientation may store only data justified by Orientation's capability/provider rules and must not become authoritative copies of foreign business domains.
