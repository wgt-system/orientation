# Orientation – Spatial Research Contract 1.0

**Status:** v0.4.0 contract baseline for Issue #20.

## Purpose

`orientation.spatial-research-bundle` 1.0 is the first Orientation-owned acquisition contract for structured external spatial research.

It supports the independent Orientation workflow:

```text
explicit spatial question
  -> external research prompt
  -> structured external result
  -> contract + semantic validation
  -> later import into Orientation-owned state
```

The bundle is an acquisition artifact. It is **not** the Orientation persistence model, a generic cross-domain research format, or a trusted statement of truth merely because its JSON is valid.

## Contract identity

- contract: `orientation.spatial-research-bundle`
- version: `1.0`
- schema: `contracts/orientation-spatial-research-v1.schema.json`
- stable schema id: `https://schemas.wgt-system.org/orientation/spatial-research/1.0/schema.json`

Compatibility is exact for this baseline. Unknown contract names or versions are rejected before any future mutation.

## Question and area

A bundle carries the explicit research question that produced it:

- `questionRef` — bundle-local stable reference;
- `text` — the user-supplied research question;
- `area.center` — an explicit center label and optional coordinate;
- `area.radiusMeters` — explicit radial search scope from 1 m to 500 km;
- one to twenty explicit criteria.

The v1 contract intentionally models a bounded radial question rather than a universal spatial-query language. Current device position is not read or inserted automatically. A host or future prompt workflow may turn an explicitly supplied PositionFix into the area center only after user intent is clear.

## Criteria

Every criterion has:

- `criterionRef`;
- `description`;
- `evaluationMode`.

Accepted evaluation modes:

- `EVIDENCE_REQUIRED` — MATCH/NO_MATCH requires direct source evidence;
- `HEURISTIC` — the criterion is explicitly a user-defined heuristic and any result remains a heuristic match rather than an asserted personal/domain fact.

This distinction is important for criteria based on names, language, appearance or other proxies. For example, a criterion such as "public operator name matches this name pattern" may be represented as a heuristic. A matching public name does **not** become an Orientation assertion about ethnicity, nationality, religion or another sensitive trait.

## Sources and provenance

External researched claims refer to bundle-local `sourceRef` values. Sources contain:

- an absolute HTTPS URL;
- optional title;
- retrieval timestamp.

MATCH and NO_MATCH claims require at least one valid source reference. UNCERTAIN and UNKNOWN may legitimately have none.

A source reference records provenance of the external research artifact. It does not mean Orientation endorses the source or that the source remains current forever.

## Candidates

Each candidate contains:

- bundle-local `candidateRef`;
- display name;
- optional deterministic identity hints;
- researched location;
- one claim for every question criterion.

### Identity rules

`candidateRef` is only stable inside one bundle and is not a cross-import identity.

The only v1 hints that may later authorize deterministic cross-import reuse are:

1. a canonical absolute HTTPS URI; or
2. an explicit provider/external identifier pair.

If neither exists, later import may retain the candidate but must not silently merge it with an existing subject based only on similar name, text, address or coordinates. Issue #22 will define the persisted identity/re-import mechanics within this rule.

## Researched location versus provider-backed Place

`researchedLocation` is external research evidence. It may contain:

- a display label;
- optional reported coordinate;
- source references.

It is deliberately not the same thing as an Orientation `Place` returned by an Orientation geocoding/place provider.

A future importer may geocode or reconcile researched location data through explicit Orientation application logic. Provider-backed geographic results and externally researched claims must remain distinguishable in provenance and authority.

## Claims

Every candidate has exactly one claim for every criterion.

Claim statuses:

- `MATCH`
- `NO_MATCH`
- `UNCERTAIN`
- `UNKNOWN`

Claim bases:

- `DIRECT_EVIDENCE`
- `HEURISTIC`

Rules:

- a `HEURISTIC` criterion must always produce claims with basis `HEURISTIC`;
- MATCH/NO_MATCH for `EVIDENCE_REQUIRED` must use `DIRECT_EVIDENCE`;
- MATCH/NO_MATCH requires at least one known source;
- a heuristic MATCH means only that the candidate matches the user-defined heuristic;
- `observedValue` may preserve a small externally observed string/number/boolean but does not silently become a new first-class domain field.

## Validation boundary

`SpatialResearchBundleValidator` is the initial application-layer semantic guard.

It rejects deterministically before any future state mutation:

- malformed JSON;
- wrong contract/version;
- malformed timestamps/coordinates/HTTPS URIs;
- duplicate question/source/candidate refs;
- unknown criterion/source refs;
- missing criterion claims;
- invalid evidence/heuristic combinations;
- required evidence with no source.

The JSON Schema remains the exact transport-shape contract. The application validator adds semantic cross-reference rules that JSON Schema 2020-12 does not conveniently express.

## Canonical examples

- `contracts/examples/spatial-research-v1.valid.json`
- `contracts/examples/spatial-research-v1.invalid-heuristic-basis.json`

Examples are synthetic and must not be treated as provider data.

## Explicit non-goals of Contract 1.0

- no persistence schema;
- no generic research bundle shared with Vocation or Illumination;
- no LLM/API execution;
- no automatic crawling;
- no broad polygon/corridor/query language;
- no automatic sensitive-trait classification;
- no transit/sharing/multimodal model;
- no mutation of `orientation.host-bridge` 1.0.

## Acceptance for Issue #20

Issue #20 is complete when the contract/schema/examples and semantic validator agree, backend CI passes, and the existing v0.1-v0.3 map/place/routing behavior remains unchanged.
