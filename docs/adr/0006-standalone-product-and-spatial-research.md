# ADR-0006: Standalone product role and domain-owned spatial research acquisition

- **Status:** Accepted
- **Date:** 2026-08-17

## Context

Orientation v0.1.0 through v0.3.0 intentionally delivered reusable geospatial foundations first: map rendering, Current Location, place search/geocoding and routing.

That sequencing created a risk that Orientation could be read as infrastructure whose only purpose is to serve Vocation or WGT. The intended domain is broader: Orientation must also support independent personal spatial discovery, exploration and mobility workflows, including structured externally researched data that ordinary place providers cannot supply.

Vocation and Illumination already use domain-owned prompt/structured-import workflows. Similar mechanics in Orientation raise a second question: whether prompt generation should become a new shared bounded context or microservice.

## Decision

1. Orientation is an **independently useful local-first personal spatial exploration and mobility bounded context**.
2. Orientation remains the accepted system owner of reusable generic geospatial capability; independence and reuse are both first-class roles.
3. Orientation may own local persistent state required by its own product workflows, including validated imported spatial research, provenance and personal discovery collections.
4. Orientation may generate domain-specific research prompts for an external ChatGPT/research interaction and import versioned structured results through Orientation-owned contracts.
5. External research output is input/evidence and must be validated/translated before becoming Orientation-owned state.
6. Orientation and Vocation remain separate bounded contexts. Geographic overlap is handled through explicit integration; job-market semantics remain Vocation-owned.
7. No generic prompt/LLM/research microservice is introduced by this decision. Prompt wording, requested fields, schemas and import meaning remain with the bounded context whose data they create.
8. Shared non-semantic prompt/import mechanics may later be extracted as utilities/libraries if useful. A shared execution service requires a concrete system-wide operational need and a separate System Architecture decision.
9. The current Reference Host remains a development/acceptance artifact. This does not prohibit a future first-class standalone Orientation end-user application.

## Consequences

- A concrete standalone Orientation product slice can now justify local persistence without copying foreign domain state.
- Future Orientation planning should prioritize the complete Discover -> Explore -> Navigate user loop, not only consumer migrations.
- Vocation may consume Orientation geocoding/rendering/routing while remaining independently authoritative.
- WGT may present Orientation on supported platforms without owning Orientation domain semantics.
- Public transit, shared mobility and multimodal routing remain future Orientation slices and require explicit models beyond the v0.3.0 travel-profile contract.
- Agents must not reject standalone Orientation UI/product work merely because earlier releases used a Reference Host.

## Rejected alternatives

- **Treat Orientation as infrastructure-only:** contradicts the intended independent spatial-research/mobility product and would bias future work toward consumer needs only.
- **Merge Orientation into Vocation:** would couple unrelated spatial/mobility scenarios to the job-market lifecycle and language.
- **Move all GPT prompts into a generic prompt service now:** centralizes domain-specific schemas and change drivers without an independent generic business capability.
- **Make WGT the owner of Orientation product semantics:** confuses cross-platform composition/presentation with domain ownership.
