# Orientation – Domain Vision

**Status:** Post-v0.3 product vision

## 1. Purpose

Orientation is the personal spatial exploration and mobility bounded context of `wgt-system`.

It is independently useful and also owns the reusable generic geospatial capabilities that other bounded contexts may consume.

Orientation organizes its problem space around three connected user questions:

1. **Discover** — What is where, and which places/spatial objects satisfy my current question?
2. **Explore** — What do I know about this place/spatial object, how reliable is that knowledge, and what can I do with it?
3. **Navigate** — How can I get there?

A supporting concern is **Current Location**: where is the user/device now, with what accuracy, and how may that position participate in exploration and mobility planning?

## 2. Product hypothesis

Useful spatial questions are often richer than ordinary map-provider queries.

A user may want to identify nearby restaurants, shops, services, events or other spatial candidates using a combination of:

- geographic constraints;
- ordinary place/provider data;
- externally researched facts;
- user-defined criteria;
- source/provenance information;
- travel time or mobility constraints.

Orientation should let the user turn such a question into a durable, explorable spatial result instead of leaving the answer trapped in a chat or temporary browser tabs.

A representative workflow is:

```text
user spatial question / criteria
        ↓
Orientation prepares a structured research prompt
        ↓
external ChatGPT / research interaction
        ↓
versioned JSON result
        ↓
Orientation validates and imports
        ↓
local Orientation-owned spatial collection + provenance
        ↓
list/map exploration and filtering
        ↓
selection
        ↓
route / mobility planning
```

The initial workflow does not require a paid LLM API. Copy/paste or file-based exchange with external ChatGPT is a valid first-class acquisition path, analogous in shape to existing Vocation and Illumination workflows while retaining Orientation-specific semantics.

## 3. Core-domain hypothesis

> Orientation's core domain is turning personal spatial questions and evidence into useful spatial exploration and mobility decisions.

The map is not the domain by itself. Routing is not the domain by itself. Prompt generation is not the domain by itself.

The value is the complete loop from a spatial question to researched/resolved candidates, understandable evidence, exploration and getting there.

## 4. Independent bounded context, reusable capability owner

Orientation is not an infrastructure-only helper created merely to remove duplicate map code.

It is intentionally both:

1. an independent personal application/domain with its own future state and workflows; and
2. the accepted system owner of reusable generic geospatial capability.

These roles reinforce rather than contradict each other. The independent Orientation product uses the same provider-neutral place, map and routing foundations that Vocation, WGT or future contexts may consume.

## 5. Relationship to Vocation

Vocation and Orientation remain separate bounded contexts.

Vocation owns personal job-market meaning, including Opportunities, Companies, Postings, Work Location, Precision, assessments and decisions.

Orientation owns generic spatial discovery/mobility meaning and its own personal spatial-research state.

Examples of legitimate integration:

```text
Vocation Work Location
    -> Orientation geocoding
    -> Vocation interprets resolution/precision

Vocation destination + host current position
    -> Orientation routing
    -> route shown in Vocation/WGT presentation
```

A job being located somewhere does not make job-market semantics part of Orientation. Conversely, Orientation must remain useful for restaurant, leisure, service, travel or other spatial questions that have nothing to do with Vocation.

## 6. Relationship to Wiiii Got This

WGT owns cross-platform integration, device/platform composition, product-shell navigation and platform permission handling.

WGT may present Orientation capabilities on Windows/iPhone, but it does not become owner of Orientation's domain data or rules.

Orientation may also have a standalone end-user application. The current Reference Host is a development/acceptance host, not the permanent definition of Orientation's product surface.

## 7. External research and prompt acquisition

Orientation may generate domain-specific prompts that ask an external research context such as ChatGPT to produce structured spatial research.

The resulting import contract belongs to Orientation because the imported data becomes Orientation-owned state after validation and translation.

Important rules:

- externally generated claims are evidence/input, not automatic truth;
- source/provenance should be retained where the imported claim depends on research;
- research criteria are explicit user intent;
- heuristic criteria must not silently become asserted sensitive personal characteristics;
- prompt wording, schema and import validation evolve with Orientation's domain;
- a shared prompt-rendering utility does not imply shared ownership of prompt semantics.

No generic prompt/LLM microservice is required merely because Vocation, Illumination and Orientation each generate prompts. A separate system capability would require a concrete shared execution problem and an explicit architecture decision.

## 8. Discover

Discover includes both ordinary provider-backed place discovery and richer personal spatial research.

Possible inputs include:

- text/place search;
- nearby/spatial constraints;
- current or explicit location bias;
- imported research results;
- user-defined criteria;
- later provider datasets suitable for POI or mobility discovery.

Orientation owns the generic spatial interpretation and its own imported research state, not external-provider datasets themselves.

## 9. Explore

Explore makes spatial results understandable and actionable.

This includes:

- map/list presentation;
- spatial features and geometry;
- source/provenance visibility;
- rich information/resources/actions;
- filtering and comparison of Orientation-owned discovery results;
- current-position context;
- selection and viewport behavior.

Provider-owned foreign features remain provider-owned when Orientation is only rendering them.

## 10. Navigate and mobility direction

The released v0.3.0 baseline proves provider-neutral two-point routing for:

- driving;
- cycling;
- walking.

The long-term mobility problem is broader and may include concrete future slices for:

- public transit;
- departure/arrival-time-aware routing;
- service disruptions/realtime data where available;
- bike/scooter/car sharing;
- multimodal journeys;
- alternative-route comparison;
- route quality and mobility-provider availability.

These capabilities must be introduced through explicit provider-neutral Orientation semantics rather than forcing transit/sharing data into the existing v0.3 travel-profile model.

## 11. Data ownership and persistence

The v0.1–v0.3 foundations were deliberately mostly stateless because no Orientation-owned persistent product state had yet been accepted.

The independent research/import workflow provides the first concrete reason for local Orientation persistence.

Orientation may own and persist data such as:

- imported spatial research items/observations;
- provenance/source references needed to evaluate those observations;
- user-created discovery collections;
- user-supplied research criteria/configuration where useful;
- resolved generic place identity/coordinates as Orientation state;
- future Orientation-specific personal organization state.

This must not become a copy of Vocation, Illumination or another bounded context's authoritative database.

## 12. Non-goals

Orientation is not:

- a universal business-entity catalog;
- a job-search application;
- a learning application;
- the owner of WGT product navigation;
- a generic UI design system;
- a mandatory remote microservice;
- a synchronization/relay service;
- a generic LLM/research platform;
- an owner of external provider truth;
- an excuse to copy foreign domain state into shared persistence.

## 13. Success direction

Orientation is successful when a user can move from:

```text
"I want to find places matching this spatial/research question"
        ↓
structured, source-aware candidates
        ↓
clear map/list exploration
        ↓
confident selection
        ↓
useful mobility options
        ↓
"I know where I am going and how to get there"
```

while the same generic geospatial foundations remain reusable by Vocation, WGT and future bounded contexts without transferring their business ownership.
