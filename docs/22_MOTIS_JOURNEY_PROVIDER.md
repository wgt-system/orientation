# Orientation – MOTIS Provider Runtime

**Status:** v0.5.0 Journey provider accepted; post-v0.5 hardening also uses local MOTIS for Place Search and Reverse Geocoding.

MOTIS is infrastructure behind separate Orientation-owned application ports. It does not define Orientation domain semantics.

## Reviewed baseline

- MOTIS `v2.11.0`;
- Journey API `v6`, `GET /api/v6/plan`;
- geocoding `GET /api/v1/geocode`;
- reverse geocoding `GET /api/v1/reverse-geocode`;
- official Linux amd64 archive `motis-linux-amd64.tar.bz2`;
- archive SHA-256 `508505d3f9cd2e872c763743c459cae4e0539fad14bf490e23251e013d3a6dfa`.

Compatibility with future MOTIS versions must be reviewed rather than inferred from unchanged endpoint paths.

## Runtime configuration

```yaml
orientation:
  motis:
    base-url: ${ORIENTATION_MOTIS_BASE_URL:http://127.0.0.1:8081}
    connect-timeout: 3s
    read-timeout: 15s
```

The default is deliberately local. Orientation has no automatic Transitous or other hosted fallback.

The same configured MOTIS process serves two independent adapter roles:

```text
PlaceSearchPort / ReverseGeocodingPort -> MotisPlaceAdapter
JourneyPort                           -> MotisJourneyAdapter
```

This runtime consolidation does not collapse Place and Journey domain/application boundaries.

## Place Search / Reverse Geocoding

Forward search maps Orientation `PlaceSearchQuery` to `/api/v1/geocode`:

- `text` from explicit submitted search text;
- `numResults` from the bounded Orientation limit;
- optional `language`;
- optional location bias via MOTIS `place=lat,lon`.

Reverse geocoding maps the explicit Coordinate to `/api/v1/reverse-geocode?place=lat,lon&numResults=1`.

MOTIS `Match` data is translated into Orientation `Place`:

- opaque provider reference is prefixed `motis:`;
- name becomes display label;
- `lon`/`lat` become Orientation Coordinate;
- MOTIS match type is retained only as optional generic place kind;
- address fields are populated only when supplied; missing city/state/country labels are not invented.

Provider responses are bounded to 1 MiB and malformed/out-of-range results become stable Orientation Place failures.

Photon remains part of v0.2 release history but is no longer part of the current default runtime.

## Journey request mapping

The Journey adapter maps to `/api/v6/plan` with:

- `fromPlace` / `toPlace` as `latitude,longitude`;
- offset-aware `time`;
- `arriveBy=true` only for `ARRIVE_BY`;
- WALK pre/post-transit;
- no direct modes in this transit slice;
- detailed legs/transfers;
- at most eight retained alternatives;
- realtime-capable mode;
- an explicit accepted public-transit mode subset.

Timezone offsets are URI-encoded as data. Shared/rental/ODM/ride-sharing modes are not requested or silently normalized.

## Journey response translation

MOTIS itineraries are translated before crossing `JourneyPort`:

- WALK stays WALK;
- standard transit modes map to Orientation transit modes;
- rail-family modes map to RAIL/SUBURBAN_RAIL as defined by the accepted adapter;
- unsupported modes are rejected;
- scheduled timing is always retained;
- realtime-adjusted timing is optional and explicit;
- detailed polyline geometry is accepted only at reviewed precision 6 and decoded before crossing infrastructure;
- geometry is bounded to 10,000 Coordinates per leg;
- provider IDs/error bodies do not leak through the stable HTTP contract.

Journey provider responses are bounded to 4 MiB.

## Deterministic self-hosted acceptance

`MOTIS Journey Smoke` pins:

- MOTIS v2.11.0 archive and checksum;
- `motis-project/test-data` commit `e2a596045675e12760d77db991b57f1979a998e6`;
- Aachen OSM blob `d4f8a764450637f25a687ba2444914a13b087cab`;
- AVV GTFS blob `8dd7acedd31f961217bf69e4e8bf7d5dae4c8c97`.

The gate now proves both provider roles using the same self-hosted runtime:

```text
pinned OSM + GTFS
      -> MOTIS config/import/server
      -> Orientation Place Search
      -> Orientation Journey planning
      -> production standalone browser
```

The browser acceptance searches the real pinned MOTIS dataset for its origin, plans a real transit Journey and renders/selects/clears it while preserving Discovery state.

No public Transitous or Photon request is required for deterministic acceptance.

## Privacy boundary

By default, search text, origin, destination and Journey time stay between Orientation and the loopback MOTIS process. A deployment that deliberately points `ORIENTATION_MOTIS_BASE_URL` at a non-loopback endpoint changes that privacy boundary and requires explicit operator review.

Orientation must not silently choose a hosted endpoint when local MOTIS is missing.

## Non-goals

This runtime does not add:

- GBFS/shared mobility;
- fares/ticketing or booking;
- complete realtime-coverage claims;
- arbitrary multimodal sharing optimization;
- mobile dataset packaging;
- automatic provider selection/failover.

## Primary provider references

- MOTIS repository: https://github.com/motis-project/motis
- MOTIS v2.11.0: https://github.com/motis-project/motis/releases/tag/v2.11.0
- MOTIS setup: https://github.com/motis-project/motis/blob/v2.11.0/docs/setup.md
- MOTIS OpenAPI: https://github.com/motis-project/motis/blob/v2.11.0/openapi.yaml
- MOTIS test data: https://github.com/motis-project/test-data
