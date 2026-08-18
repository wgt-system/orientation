# Orientation local Hamburg runtime

This is the Windows-first local runtime for normal manual use of the standalone Orientation browser app.

## Runtime topology

After one explicit setup, normal semantic requests stay local:

```text
Browser
  +-- OpenFreeMap hosted basemap
  |
  +-- Orientation backend 127.0.0.1:8080
          +-- MOTIS 127.0.0.1:8081
          |      +-- Place Search / Reverse Geocoding
          |      +-- public-transit Journey
          +-- Valhalla 127.0.0.1:8002
                 +-- DRIVING / CYCLING / WALKING Route
```

Setup itself intentionally downloads runtime/data artifacts. This is different from forwarding user searches or routes to hosted APIs during normal use.

## Prerequisites

- Windows;
- Java 25;
- Node.js 24 / npm;
- Docker Desktop with Docker Compose v2 running.

## First setup

From the repository root:

```powershell
.\scripts\local-runtime.ps1 setup
```

The first setup:

1. downloads the official pinned MOTIS v2.11.0 Windows release;
2. downloads the pinned Geofabrik Hamburg OSM snapshot and verifies its published MD5;
3. downloads the official hvv 2026 GTFS dataset;
4. imports MOTIS with geocoding, reverse geocoding, street routing and the hvv timetable;
5. pulls/builds the pinned Valhalla Hamburg runtime once and leaves its Docker volume cached;
6. builds backend/frontend dependencies once.

The Hamburg OSM and hvv GTFS source downloads are roughly 53 MB + 38.5 MB before generated routing indexes. The generated MOTIS/Valhalla indexes are larger than the source downloads and take the majority of first-run preparation time/disk.

All generated/downloaded state lives under `.runtime/` or the existing Valhalla Docker volume and is excluded from Git.

## Normal start

```powershell
.\scripts\local-runtime.ps1 start -OpenBrowser
```

Or without opening the browser automatically:

```powershell
.\scripts\local-runtime.ps1 start
```

Then open:

```text
http://127.0.0.1:5173/app.html
```

Normal starts reuse the imported MOTIS data, Valhalla tile volume, Maven build and npm dependencies. They do not re-download the Hamburg datasets.

## Status / stop

```powershell
.\scripts\local-runtime.ps1 status
.\scripts\local-runtime.ps1 stop
```

Runtime logs are written below `.runtime/logs/`.

## Explicit rebuild

```powershell
.\scripts\local-runtime.ps1 rebuild
```

`rebuild` stops the tracked runtime, re-downloads the pinned reviewed artifacts, verifies/reimports them and rebuilds local caches. It is deliberately explicit; there is no background updater.

The pinned hvv GTFS has stated coverage through **2026-12-12**. The bootstrap refuses setup/start after that date rather than silently planning against known-expired schedule data. A newer official hvv dataset must then be reviewed and pinned in the repository.

## Dataset boundary

See [`ATTRIBUTION.md`](ATTRIBUTION.md).

The current profile is intentionally Hamburg-sized. It is not a Germany-wide local deployment and does not claim complete realtime transit coverage. Realtime feeds and mobile runtime distribution are separate future capabilities.
