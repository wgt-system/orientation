# Orientation – Context Map

**Status:** Post-v0.3 product alignment; system-level ownership remains governed by `wgt-system/architecture`.

```text
                     external ChatGPT / research
                         ^             |
            prompt / context           | versioned JSON
                         |             v
                    +-----------------------+
                    |      Orientation      |
                    | Discover / Explore /  |
                    | Navigate / own state  |
                    +-----------+-----------+
                                |
                 generic geospatial/mobility providers
                 maps / places / routing / future transit

                         Wiiii Got This
                      product composition
                     /         |          \
                    /          |           \
                   v           v            v
             Vocation    Illumination   Orientation
                |                          ^
                | optional generic use     |
                +--------------------------+

WGT -> Conveyance only for accepted durable opaque delivery scenarios
```

## User -> Orientation

Orientation is independently usable. A user may perform spatial research, import/maintain Orientation-owned spatial collections, explore them and plan mobility without Vocation being involved.

WGT may provide another presentation/composition path, but WGT is not required for Orientation to have domain meaning.

## Orientation <-> External Research Context

Orientation may generate a domain-specific research prompt containing the spatial question, accepted schema guidance and only the context needed for the requested research.

An external ChatGPT/research interaction returns a versioned structured artifact. Orientation validates/translates it before any resulting data becomes Orientation-owned state.

The external research context is not trusted as authoritative storage and is not an accepted `wgt-system` bounded context merely because prompts are exchanged with it.

## Vocation -> Orientation

Allowed when Vocation needs a generic geographic/mobility result and then interprets it in Vocation semantics, such as geocoding a Work Location.

Vocation remains authoritative for Work Location, Precision and all job-market semantics.

The relationship is integration between two independent bounded contexts, not a reason to merge them.

## Vocation/WGT -> Orientation map presentation

Vocation may publish rich domain-correct spatial projections. WGT may adapt/compose provider data into an Orientation Spatial Scene.

Orientation does not become authoritative for Opportunities, Companies, Postings, External Links or job decisions.

## Illumination -> Orientation

No concrete initial requirement is frozen. Illumination may consume Orientation later through an explicit boundary when a real spatial learning scenario exists.

## WGT -> Orientation

WGT may host/compose Orientation capabilities for Windows/iPhone product presentation and owns platform permission adapters and device/platform-specific composition.

Orientation remains independently authoritative and may also expose its own end-user application.

## Orientation -> Conveyance

No direct requirement is currently accepted. If Orientation-owned data needs durable cross-device delivery, apply the system Conveyance decision model. Do not build private relay behavior into Orientation.

## External providers

Map tiles/styles, place/geocoding providers, Valhalla, and future transit/shared-mobility providers are infrastructure dependencies behind Orientation-owned adapters and policies.

Provider choice is not cross-context domain ownership. External provider facts remain external evidence/data and must not be confused with Orientation-authored truth.
