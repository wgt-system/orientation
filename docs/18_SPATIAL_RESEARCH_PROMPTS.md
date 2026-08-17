# Orientation – Spatial Research Prompt Generation

**Status:** v0.4.0 application slice for Issue #21.

## Purpose

Orientation can generate a deterministic external-research prompt from an explicit spatial question that conforms to `orientation.spatial-research-bundle` 1.0.

The application boundary is:

```text
SpatialResearchQuestion
  -> SpatialResearchPromptService
  -> GeneratedSpatialResearchPrompt
  -> host may display/copy/export the prompt
```

No LLM is called by Orientation in this slice.

## Question model

`SpatialResearchQuestion` contains only the input required by Contract 1.0:

- question ref;
- question text;
- explicit area center label;
- optional explicitly supplied center coordinate;
- radius in metres;
- one to twenty ordered criteria.

Each criterion is either `EVIDENCE_REQUIRED` or `HEURISTIC`.

Current device position is not acquired implicitly. If a later host wants to use the current position as the area center, it must supply that position explicitly after the relevant platform/user interaction.

## Prompt semantics

The generated prompt is deterministic for deterministic input. It includes:

- exact contract/version/schema identity;
- the supplied spatial question and area;
- ordered criteria and evaluation modes;
- provenance requirements;
- uncertainty behavior;
- identity-hint restrictions;
- researched-location versus provider-backed Place separation;
- the heuristic rule preventing proxy matches from becoming asserted sensitive-trait facts;
- a compact required JSON shape.

The generator does not insert the current time, perform research, fetch URLs, infer additional criteria or modify the user's question.

## Host API

`POST /api/v1/research/prompts`

Request shape mirrors the application question input. The response contains:

- `contract`;
- `version`;
- `schemaId`;
- `prompt`.

The returned string is the copy/export boundary. Clipboard interaction and standalone UI controls remain host/presentation concerns and will be composed in the standalone workflow slice rather than added to domain/application code.

Invalid question refs, coordinates, radius, duplicate criteria or unsupported evaluation modes are rejected as ordinary `400 invalid-input` outcomes through the existing host error boundary.

## Non-goals

- no ChatGPT/OpenAI API call;
- no web crawling;
- no queued/background research;
- no persistence/import mutation;
- no generic cross-domain prompt service;
- no Vocation or Illumination prompt semantics;
- no Host Bridge 1.0 changes.
