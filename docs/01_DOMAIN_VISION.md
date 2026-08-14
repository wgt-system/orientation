# Orientation – Domain Vision

**Status:** Bootstrap baseline

## Purpose

Orientation is the reusable geospatial bounded context of `wgt-system`.

It creates a coherent capability space around three connected user questions:

1. **Discover** — What is where?
2. **Explore** — What is this spatial object and what information/actions are attached to it?
3. **Navigate** — How do I get there?

A fourth supporting concern is **Current Location**: where is the user/device now, with what accuracy, and how may that position participate in map/routing operations?

## Why this is a bounded context

The system already has generic map/geocoding duplication across Vocation and WGT. The required capability is broader than rendering alone: place discovery, geocoding, spatial representation, map interaction and routing form one coherent geospatial language and lifecycle.

Orientation is not created because "shared code needs a repository." It exists because multiple domains can legitimately consume the same geospatial capability without owning its generic semantics.

## Core value

Orientation should make spatially referenced domain objects immediately explorable.

A Vocation Opportunity, for example, can remain Vocation-owned while being presented as a spatial feature with:

- its position;
- a rich summary;
- provider-owned external resources;
- generic map interaction;
- distance/travel-time context;
- route actions.

The value is not a static map. The value is a spatial interaction capability other bounded contexts can use.

## Non-goals

Orientation is not:

- a universal business entity catalog;
- the owner of job-market or learning semantics;
- the owner of WGT product navigation;
- a generic UI design system;
- a mandatory remote microservice;
- a synchronization/relay service;
- an excuse to copy foreign domain data into shared persistence;
- a replacement for domain-owned precision/authority rules.

## Guiding boundary

A capability belongs here when its immediate subject is a **place, spatial object, position, area or path** and its semantics remain generic across consuming domains.
