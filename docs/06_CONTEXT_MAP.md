# Orientation – Context Map

**Status:** Bootstrap baseline, subject to matching system ADR in `wgt-system/architecture`.

```text
                         Wiiii Got This
                      product composition
                     /         |          \
                    /          |           \
                   v           v            v
             Vocation    Illumination   Orientation
                |                          ^
                | generic geospatial use  |
                +--------------------------+

Orientation -> external geospatial providers/engines
Orientation -> Valhalla (routing engine)

WGT -> Conveyance only for accepted durable opaque delivery scenarios
```

## Vocation -> Orientation

Allowed when Vocation needs a generic geographic result and then interprets it in Vocation semantics, such as geocoding a Work Location.

Vocation remains authoritative for Work Location and Precision.

## Vocation/WGT -> Orientation map presentation

Vocation may publish rich domain-correct spatial projections. WGT may adapt/compose provider data into an Orientation Spatial Scene.

Orientation does not become authoritative for Opportunities, Companies, Postings or External Links.

## Illumination -> Orientation

No concrete initial requirement is frozen. Illumination may consume Orientation later through an explicit boundary when a real spatial learning scenario exists.

## WGT -> Orientation

WGT hosts/composes Orientation capabilities for Windows/iPhone product presentation. WGT owns product shell, product navigation, platform permission adapters and device/platform-specific composition.

## Orientation -> Conveyance

No direct requirement exists at bootstrap. If cross-device delivery becomes necessary, apply the system Conveyance decision model. Do not build private relay behavior into Orientation.

## External providers

Map tiles/styles, place/geocoding providers and Valhalla are infrastructure/provider dependencies behind Orientation-owned adapters. Provider choice is not cross-context domain ownership.
