# Valhalla Runtime

Valhalla is the selected upstream routing engine behind the Orientation `RoutingPort` adapter. Consumers never call Valhalla directly.

## Reviewed development baseline

- Valhalla: `3.8.3`
- image: `ghcr.io/valhalla/valhalla-scripted:3.8.3`
- service port: `8002`
- dataset: Geofabrik Hamburg snapshot `hamburg-260726.osm.pbf`
- dataset URL: `https://download.geofabrik.de/europe/germany/hamburg-260726.osm.pbf`
- persistent generated graph/config data: Docker volume `valhalla-data`

The version and OSM snapshot are deliberately pinned. Do not replace either with `latest` in committed development configuration.

The upstream scripted image is maintained in the Valhalla repository and builds its graph/configuration in `/custom_files`. The Orientation compose file keeps those generated artifacts in a named Docker volume rather than the repository working tree.

## Start

From this directory:

```text
docker compose up -d
```

The first start downloads the pinned Hamburg PBF and builds Valhalla graph data before the routing service becomes usable. Subsequent starts reuse the named volume unless the volume is deliberately removed.

Inspect startup/build progress with:

```text
docker compose logs -f valhalla
```

Orientation defaults to `http://localhost:8002` through `orientation.valhalla.base-url`. The backend can start while Valhalla is unavailable; route requests then produce the stable Orientation provider-unavailable outcome.

## Provider smoke

After Valhalla reports that the service is ready, a direct provider smoke can use two central Hamburg coordinates:

```text
curl -s http://localhost:8002/route \
  -H "Content-Type: application/json" \
  -d '{"locations":[{"lat":53.5526,"lon":10.0067},{"lat":53.5504,"lon":9.9921}],"costing":"auto","units":"kilometers","directions_type":"none"}'
```

For the v0.3.0 provider acceptance, repeat the same route using `bicycle` and `pedestrian`, then exercise the route through Orientation `POST /api/v1/routes` using `DRIVING`, `CYCLING`, and `WALKING`. The direct Valhalla call is diagnostic evidence only; the Orientation HTTP boundary is the consumer-facing API.

## Boundary rules

- Valhalla costing names stay inside the adapter.
- Valhalla error codes stay inside the adapter.
- Valhalla polyline6 geometry is decoded before a `Route` crosses `RoutingPort`.
- no Valhalla response DTO or provider identifier enters Orientation domain/application types.
- provider timeouts, rate limits, no-route outcomes, invalid responses, and unavailability map to Orientation failure kinds.
- `orientation.host-bridge` 1.0 is unaffected.

## Cleanup

Stop the runtime without deleting the graph cache:

```text
docker compose down
```

Delete the local Valhalla graph/config data only when a clean rebuild is intentionally required:

```text
docker compose down -v
```
