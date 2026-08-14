# ADR-0003: Reusable MapLibre map surface

- **Status:** Accepted
- **Date:** 2026-08-14

## Decision

Orientation owns one framework-independent TypeScript map surface built on MapLibre GL JS.

The reusable core does not depend on React, Avalonia or provider-domain UI models.

Consumers supply generic scene data and receive generic interaction events.

## Hosts

Initial target hosts:

- standalone browser reference/development host;
- WGT Windows embedded web surface;
- WGT iPhone embedded web surface.

## Gate

Do not remove existing Vocation Leaflet or WGT Mapsui renderers until their replacement path has passed the relevant host tests, including a physical iPhone proof.

## Consequences

MapLibre is replaceable infrastructure and must not leak through public Orientation contract types.
