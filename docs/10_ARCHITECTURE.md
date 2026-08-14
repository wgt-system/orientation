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

Do not depend on React in the reusable renderer core.

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
