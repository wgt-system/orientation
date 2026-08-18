# Valhalla Runtime

Valhalla is the selected upstream direct-routing engine behind the Orientation `RoutingPort` adapter. Consumers never call Valhalla directly.

## Reviewed development baseline

- Valhalla: `3.8.3`
- image: `ghcr.io/valhalla/valhalla-scripted:3.8.3`
- loopback service: `127.0.0.1:8002`
- dataset: Geofabrik Hamburg monthly snapshot `hamburg-260801.osm.pbf`
- dataset URL: `https://download.geofabrik.de/europe/germany/hamburg-260801.osm.pbf`
- persistent generated graph/config data: Docker volume `valhalla-data`

The Valhalla version and monthly OSM snapshot are deliberately pinned. Do not replace either with `latest` in committed development configuration. Geofabrik retains the monthly Hamburg snapshots in its region download index.

The compose port is explicitly bound to `127.0.0.1`; normal local development does not publish Valhalla to the LAN.

## Preferred local product workflow

For normal standalone Orientation use, do not start Valhalla separately. Use the repository-level Hamburg runtime bootstrap:

```powershell
.\scripts\local-runtime.ps1 setup
.\scripts\local-runtime.ps1 start -OpenBrowser
```

See [`../local-hamburg/README.md`](../local-hamburg/README.md).

The first `setup` starts this compose runtime long enough to pull/build the pinned Hamburg graph and then stops it without deleting the named volume. Later `start` calls reuse that graph.

## Manual diagnostics

From this directory:

```text
docker compose up -d
```

Inspect startup/build progress with:

```text
docker compose logs -f valhalla
```

Orientation defaults to `http://127.0.0.1:8002`. The backend can start while Valhalla is unavailable; direct route requests then produce the stable Orientation provider-unavailable outcome.

A direct provider diagnostic can use two central Hamburg coordinates:

```text
curl -s http://127.0.0.1:8002/route \
  -H "Content-Type: application/json" \
  -d '{"locations":[{"lat":53.5526,"lon":10.0067},{"lat":53.5504,"lon":9.9921}],"costing":"auto","units":"kilometers","directions_type":"none"}'
```

The direct Valhalla call is diagnostic evidence only; Orientation `POST /api/v1/routes` is the consumer-facing boundary.

## Boundary rules

- Valhalla costing names stay inside the adapter.
- Valhalla error codes stay inside the adapter.
- Valhalla polyline6 geometry is decoded before a `Route` crosses `RoutingPort`.
- no Valhalla response DTO/provider identifier enters Orientation domain/application types.
- provider failures map to stable Orientation failure kinds.
- `orientation.host-bridge` 1.0 is unaffected.

## Cleanup

Stop without deleting graph cache:

```text
docker compose down
```

Delete the graph/config cache only for an intentional clean rebuild:

```text
docker compose down -v
```
