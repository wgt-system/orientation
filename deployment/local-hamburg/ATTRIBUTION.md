# Hamburg local runtime data attribution

Orientation's local Hamburg runtime is built from explicitly downloaded open datasets. These downloads happen during `scripts/runtime.ps1 setup` / `rebuild`; normal place-search, direct-route and Journey requests use the local imported runtimes.

## OpenStreetMap / Geofabrik

- Dataset: Geofabrik Hamburg OSM extract `hamburg-260801.osm.pbf`
- Source: https://download.geofabrik.de/europe/germany/hamburg.html
- Data: © OpenStreetMap contributors
- License: Open Data Commons Open Database License (ODbL) 1.0
- Geofabrik download page: https://download.geofabrik.de/europe/germany/hamburg.html

The bootstrap downloads the accompanying Geofabrik MD5 file and verifies the PBF before import.

## Hamburger Verkehrsverbund (hvv)

- Dataset: `hvv_Rohdaten_GTFS_Fpl_20260408`
- Source: https://suche.transparenz.hamburg.de/dataset/hvv-fahrplandaten-gtfs-april-2026-bis-dezember-2026
- Publishing organization / required attribution: **Hamburger Verkehrsverbund GmbH**
- License: Datenlizenz Deutschland – Namensnennung – Version 2.0
- Published: 2026-04-08
- Stated temporal coverage: 2026-04-08 through 2026-12-12

The pinned GTFS bootstrap must be replaced by a reviewed newer official hvv dataset before its stated temporal coverage expires.

## Runtime boundary

OpenFreeMap remains the intentional hosted basemap used by the browser. The local runtime does not use the datasets above as permission to forward Orientation search text, origins, destinations or Journey times to hosted semantic-provider APIs.
