import "./styles.css";

import { OrientationMapSurface } from "../lib/map-surface";
import type { SpatialFeature, SpatialFeatureSelectedEvent, SpatialScene } from "../lib/model";
import type { TravelProfile } from "../lib/route-overlay";
import { PlaceApiError, searchPlaces, type Place } from "../reference/place-api";
import { requestRoute, RouteApiError } from "../reference/route-api";
import {
  DiscoveryApiError,
  generateResearchPrompt,
  getDiscoveryCollection,
  importDiscoveryBundle,
  listDiscoveryCollections,
  type DiscoveryCandidate,
  type DiscoveryDetail,
  type DiscoverySummary,
  type EvaluationMode,
  type ResearchPromptRequest,
} from "./discovery-api";
import { candidateToSpatialFeature } from "./discovery-feature";

function required<T extends Element>(selector: string): T {
  const element = document.querySelector<T>(selector);
  if (!element) throw new Error(`Standalone Orientation control is missing: ${selector}`);
  return element;
}

const researchForm = required<HTMLFormElement>("#research-form");
const researchQuestion = required<HTMLTextAreaElement>("#research-question");
const researchCenter = required<HTMLInputElement>("#research-center");
const researchRadius = required<HTMLInputElement>("#research-radius");
const criteriaList = required<HTMLElement>("#criteria-list");
const addCriterionButton = required<HTMLButtonElement>("#add-criterion");
const promptStatus = required<HTMLElement>("#prompt-status");
const promptOutput = required<HTMLElement>("#prompt-output");
const promptText = required<HTMLTextAreaElement>("#prompt-text");
const copyPromptButton = required<HTMLButtonElement>("#copy-prompt");
const bundleJson = required<HTMLTextAreaElement>("#bundle-json");
const importButton = required<HTMLButtonElement>("#import-bundle");
const importStatus = required<HTMLElement>("#import-status");
const importErrors = required<HTMLUListElement>("#import-errors");
const refreshCollectionsButton = required<HTMLButtonElement>("#refresh-collections");
const collectionsStatus = required<HTMLElement>("#collections-status");
const collectionsList = required<HTMLOListElement>("#collections-list");
const collectionDetail = required<HTMLElement>("#collection-detail");
const collectionMeta = required<HTMLElement>("#collection-meta");
const collectionQuestion = required<HTMLElement>("#collection-question");
const collectionArea = required<HTMLElement>("#collection-area");
const candidateFilter = required<HTMLInputElement>("#candidate-filter");
const candidateSort = required<HTMLSelectElement>("#candidate-sort");
const showAllCandidates = required<HTMLButtonElement>("#show-all-candidates");
const candidateStatus = required<HTMLElement>("#candidate-status");
const candidateList = required<HTMLOListElement>("#candidate-list");
const mapContainer = required<HTMLElement>("#app-map");
const mapStatus = required<HTMLElement>("#app-map-status");
const selectedDestination = required<HTMLElement>("#selected-destination");
const routeOriginQuery = required<HTMLInputElement>("#route-origin-query");
const routeOriginSearch = required<HTMLButtonElement>("#route-origin-search");
const routeOriginStatus = required<HTMLElement>("#route-origin-status");
const routeOriginResults = required<HTMLOListElement>("#route-origin-results");
const routeProfile = required<HTMLSelectElement>("#route-profile");
const requestRouteButton = required<HTMLButtonElement>("#request-route");
const clearRouteButton = required<HTMLButtonElement>("#clear-route");
const routeStatus = required<HTMLElement>("#route-status");
const routeSummary = required<HTMLElement>("#route-summary");
const candidateDetail = required<HTMLElement>("#candidate-detail");
const candidateDetailTitle = required<HTMLElement>("#candidate-detail-title");
const candidateLocation = required<HTMLElement>("#candidate-location");
const candidateClaims = required<HTMLElement>("#candidate-claims");
const candidateSources = required<HTMLElement>("#candidate-sources");

let collectionSummaries: readonly DiscoverySummary[] = [];
let currentCollection: DiscoveryDetail | undefined;
let selectedCandidate: DiscoveryCandidate | undefined;
let routeOrigin: Place | undefined;
let promptAbort: AbortController | undefined;
let importAbort: AbortController | undefined;
let collectionAbort: AbortController | undefined;
let originAbort: AbortController | undefined;
let routeAbort: AbortController | undefined;
let routePending = false;
let routeRendered = false;
let promptRequest = 0;
let importRequest = 0;
let collectionRequest = 0;
let originRequest = 0;
let routeRequestSequence = 0;

const surface = new OrientationMapSurface(mapContainer, {
  onFeatureSelected: (event) => selectFromMap(event),
  onStatusChanged: ({ status }) => {
    mapStatus.hidden = status !== "error";
    mapStatus.textContent = status === "error" ? "Map unavailable. Basemap style or tiles could not be loaded." : "";
  },
});
surface.setScene({ features: [] });

addCriterion();
void refreshCollections();
updateRouteControls();

addCriterionButton.addEventListener("click", () => addCriterion());
researchForm.addEventListener("submit", (event) => {
  event.preventDefault();
  void generatePrompt();
});
copyPromptButton.addEventListener("click", () => void copyPrompt());
importButton.addEventListener("click", () => void importBundle());
refreshCollectionsButton.addEventListener("click", () => void refreshCollections(currentCollection?.collectionId));
candidateFilter.addEventListener("input", renderCandidates);
candidateSort.addEventListener("change", renderCandidates);
showAllCandidates.addEventListener("click", () => renderCollectionScene());
routeOriginSearch.addEventListener("click", () => void searchRouteOrigin());
routeOriginQuery.addEventListener("keydown", (event) => {
  if (event.key === "Enter") {
    event.preventDefault();
    void searchRouteOrigin();
  }
});
routeProfile.addEventListener("change", () => invalidateRoute("Travel profile changed. Request the route again."));
requestRouteButton.addEventListener("click", () => void requestSelectedRoute());
clearRouteButton.addEventListener("click", () => clearRoute("No route requested."));

function addCriterion(description = "", mode: EvaluationMode = "EVIDENCE_REQUIRED"): void {
  const row = document.createElement("div");
  row.className = "criterion-row";
  row.dataset.criterion = "true";

  const descriptionLabel = document.createElement("label");
  descriptionLabel.textContent = "Criterion";
  const descriptionInput = document.createElement("input");
  descriptionInput.className = "criterion-description";
  descriptionInput.value = description;
  descriptionInput.placeholder = "e.g. Vegetarian options documented";
  descriptionInput.required = true;
  descriptionLabel.append(descriptionInput);

  const modeLabel = document.createElement("label");
  modeLabel.textContent = "Evidence";
  const modeSelect = document.createElement("select");
  modeSelect.className = "criterion-mode";
  for (const [value, label] of [
    ["EVIDENCE_REQUIRED", "Required"],
    ["HEURISTIC", "Heuristic"],
  ] as const) {
    const option = document.createElement("option");
    option.value = value;
    option.textContent = label;
    option.selected = value === mode;
    modeSelect.append(option);
  }
  modeLabel.append(modeSelect);

  const remove = document.createElement("button");
  remove.type = "button";
  remove.className = "secondary criterion-remove";
  remove.textContent = "×";
  remove.setAttribute("aria-label", "Remove criterion");
  remove.addEventListener("click", () => {
    if (criteriaList.querySelectorAll("[data-criterion]").length <= 1) return;
    row.remove();
  });

  row.append(descriptionLabel, modeLabel, remove);
  criteriaList.append(row);
}

async function generatePrompt(): Promise<void> {
  let request: ResearchPromptRequest;
  try {
    request = researchRequestFromForm();
  } catch (error) {
    setStatus(promptStatus, error instanceof Error ? error.message : "Research question is incomplete.", "error");
    return;
  }

  promptAbort?.abort();
  const controller = new AbortController();
  promptAbort = controller;
  const sequence = ++promptRequest;
  setStatus(promptStatus, "Generating prompt…");
  promptOutput.hidden = true;
  try {
    const response = await generateResearchPrompt(request, { signal: controller.signal });
    if (sequence !== promptRequest) return;
    promptText.value = response.prompt;
    promptOutput.hidden = false;
    setStatus(promptStatus, `Prompt ready · ${response.contract} ${response.version}`, "success");
  } catch (error) {
    if (controller.signal.aborted || sequence !== promptRequest) return;
    setStatus(promptStatus, apiMessage(error, "Prompt generation is temporarily unavailable."), "error");
  }
}

function researchRequestFromForm(): ResearchPromptRequest {
  const text = researchQuestion.value.trim();
  const center = researchCenter.value.trim();
  const radiusKm = Number(researchRadius.value);
  if (!text) throw new Error("Enter a research question.");
  if (!center) throw new Error("Enter an area center.");
  if (!Number.isFinite(radiusKm) || radiusKm <= 0 || radiusKm > 500) throw new Error("Radius must be between 0.1 and 500 km.");

  const rows = [...criteriaList.querySelectorAll<HTMLElement>("[data-criterion]")];
  const criteria = rows.map((row, index) => {
    const description = row.querySelector<HTMLInputElement>(".criterion-description")?.value.trim() ?? "";
    const mode = row.querySelector<HTMLSelectElement>(".criterion-mode")?.value as EvaluationMode | undefined;
    if (!description || !mode) throw new Error("Every criterion needs a description and evidence mode.");
    return { criterionRef: `criterion-${index + 1}`, description, evaluationMode: mode };
  });
  if (!criteria.length) throw new Error("Add at least one criterion.");

  return {
    questionRef: questionRef(text),
    text,
    area: { center: { label: center }, radiusMeters: Math.round(radiusKm * 1000) },
    criteria,
  };
}

function questionRef(text: string): string {
  const slug = text
    .normalize("NFKD")
    .replace(/[^A-Za-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .toLowerCase()
    .slice(0, 60);
  return `research-${slug || "question"}`.slice(0, 80);
}

async function copyPrompt(): Promise<void> {
  if (!promptText.value) return;
  try {
    await navigator.clipboard.writeText(promptText.value);
  } catch {
    promptText.focus();
    promptText.select();
    if (!document.execCommand("copy")) {
      setStatus(promptStatus, "Copy failed. Select the prompt text and copy it manually.", "error");
      return;
    }
  }
  setStatus(promptStatus, "Prompt copied.", "success");
}

async function importBundle(): Promise<void> {
  const json = bundleJson.value.trim();
  if (!json) {
    setStatus(importStatus, "Paste a Spatial Research Bundle first.", "error");
    return;
  }
  importAbort?.abort();
  const controller = new AbortController();
  importAbort = controller;
  const sequence = ++importRequest;
  importErrors.replaceChildren();
  setStatus(importStatus, "Validating and importing…");
  try {
    const report = await importDiscoveryBundle(json, { signal: controller.signal });
    if (sequence !== importRequest) return;
    if (report.status === "REJECTED") {
      setStatus(importStatus, "Bundle rejected. No local data was changed.", "error");
      renderErrors(report.errors);
      return;
    }
    const label = report.status === "CREATED" ? "Collection imported." : "Collection already exists; no duplicate was created.";
    setStatus(importStatus, `${label} ${report.candidateCount} candidate${report.candidateCount === 1 ? "" : "s"}.`, "success");
    bundleJson.value = "";
    await refreshCollections(report.collectionId ?? undefined);
  } catch (error) {
    if (controller.signal.aborted || sequence !== importRequest) return;
    setStatus(importStatus, apiMessage(error, "Discovery import is temporarily unavailable."), "error");
  }
}

function renderErrors(errors: readonly string[]): void {
  importErrors.replaceChildren();
  for (const error of errors) {
    const item = document.createElement("li");
    item.textContent = error;
    importErrors.append(item);
  }
}

async function refreshCollections(openCollectionId?: string): Promise<void> {
  collectionAbort?.abort();
  const controller = new AbortController();
  collectionAbort = controller;
  const sequence = ++collectionRequest;
  setStatus(collectionsStatus, "Loading saved discoveries…");
  try {
    collectionSummaries = await listDiscoveryCollections({ signal: controller.signal });
    if (sequence !== collectionRequest) return;
    renderCollectionList();
    if (!collectionSummaries.length) {
      setStatus(collectionsStatus, "No saved discoveries yet.");
      collectionDetail.hidden = true;
      currentCollection = undefined;
      selectedCandidate = undefined;
      surface.setScene({ features: [] });
      updateRouteControls();
      return;
    }
    setStatus(collectionsStatus, `${collectionSummaries.length} saved collection${collectionSummaries.length === 1 ? "" : "s"}.`, "success");
    const target = openCollectionId ?? currentCollection?.collectionId;
    if (target && collectionSummaries.some((summary) => summary.collectionId === target)) {
      await openCollection(target);
    }
  } catch (error) {
    if (controller.signal.aborted || sequence !== collectionRequest) return;
    setStatus(collectionsStatus, apiMessage(error, "Saved discoveries are temporarily unavailable."), "error");
  }
}

function renderCollectionList(): void {
  collectionsList.replaceChildren();
  for (const summary of collectionSummaries) {
    const item = document.createElement("li");
    const button = document.createElement("button");
    button.type = "button";
    button.className = "collection-button";
    button.dataset.collectionId = summary.collectionId;
    button.setAttribute("aria-current", String(summary.collectionId === currentCollection?.collectionId));
    const title = document.createElement("strong");
    title.textContent = summary.questionText;
    const meta = document.createElement("span");
    meta.textContent = `${summary.centerLabel} · ${formatRadius(summary.radiusMeters)} · ${summary.candidateCount} candidate${summary.candidateCount === 1 ? "" : "s"}`;
    button.append(title, meta);
    button.addEventListener("click", () => void openCollection(summary.collectionId));
    item.append(button);
    collectionsList.append(item);
  }
}

async function openCollection(collectionId: string): Promise<void> {
  collectionAbort?.abort();
  const controller = new AbortController();
  collectionAbort = controller;
  const sequence = ++collectionRequest;
  setStatus(collectionsStatus, "Opening collection…");
  try {
    const detail = await getDiscoveryCollection(collectionId, { signal: controller.signal });
    if (sequence !== collectionRequest) return;
    currentCollection = detail;
    selectedCandidate = undefined;
    candidateFilter.value = "";
    candidateSort.value = "research";
    clearRoute("No route requested.");
    renderCollectionList();
    renderCollection(detail);
    setStatus(collectionsStatus, "Collection open.", "success");
  } catch (error) {
    if (controller.signal.aborted || sequence !== collectionRequest) return;
    setStatus(collectionsStatus, apiMessage(error, "Collection could not be opened."), "error");
  }
}

function renderCollection(detail: DiscoveryDetail): void {
  collectionDetail.hidden = false;
  collectionMeta.textContent = `Researched ${formatDate(detail.researchedAt)}`;
  collectionQuestion.textContent = detail.question.text;
  collectionArea.textContent = `${detail.question.centerLabel} · ${formatRadius(detail.question.radiusMeters)}`;
  candidateDetail.hidden = true;
  selectedDestination.textContent = "Select a mapped candidate.";
  renderCandidates();
  renderCollectionScene();
}

function renderCandidates(): void {
  const detail = currentCollection;
  candidateList.replaceChildren();
  if (!detail) return;
  const filter = candidateFilter.value.trim().toLocaleLowerCase();
  let candidates = [...detail.candidates];
  if (filter) {
    candidates = candidates.filter((candidate) =>
      `${candidate.displayName} ${candidate.researchedLocation.label}`.toLocaleLowerCase().includes(filter),
    );
  }
  if (candidateSort.value === "name") {
    candidates.sort((a, b) => a.displayName.localeCompare(b.displayName));
  }
  candidateStatus.textContent = `${candidates.length} of ${detail.candidates.length} candidate${detail.candidates.length === 1 ? "" : "s"} shown · ${mappedCount(detail)} mapped.`;
  for (const candidate of candidates) {
    const item = document.createElement("li");
    const button = document.createElement("button");
    button.type = "button";
    button.className = "candidate-button";
    button.dataset.candidateRef = candidate.candidateRef;
    button.setAttribute("aria-current", String(candidate.candidateRef === selectedCandidate?.candidateRef));
    const title = document.createElement("strong");
    title.textContent = candidate.displayName;
    const meta = document.createElement("span");
    meta.textContent = `${candidate.researchedLocation.label}${candidate.researchedLocation.coordinate ? " · mapped" : " · no coordinate"}`;
    button.append(title, meta);
    button.addEventListener("click", () => selectCandidate(candidate, true));
    item.append(button);
    candidateList.append(item);
  }
}

function renderCollectionScene(): void {
  const detail = currentCollection;
  if (!detail) return;
  const features = detail.candidates
    .map((candidate) => candidateToSpatialFeature(detail, candidate))
    .filter((feature): feature is SpatialFeature => feature !== null);
  const scene: SpatialScene = features.length
    ? { features, viewport: { kind: "automatic", padding: 52, maxZoom: 15 } }
    : { features };
  surface.setScene(scene);
}

function selectFromMap(event: SpatialFeatureSelectedEvent): void {
  const detail = currentCollection;
  if (!detail || event.sourceRef !== `orientation/discovery/${detail.collectionId}`) return;
  const candidate = detail.candidates.find((value) => value.candidateRef === event.featureRef);
  if (candidate) selectCandidate(candidate, false);
}

function selectCandidate(candidate: DiscoveryCandidate, focusMap: boolean): void {
  selectedCandidate = candidate;
  invalidateRoute("Destination changed. Request the route again.");
  selectedDestination.textContent = candidate.researchedLocation.coordinate
    ? `${candidate.displayName} · ${candidate.researchedLocation.label}`
    : `${candidate.displayName} has no researched coordinate and cannot be routed yet.`;
  renderCandidates();
  renderCandidateDetail(candidate);
  if (focusMap && currentCollection) {
    const feature = candidateToSpatialFeature(currentCollection, candidate);
    if (feature) surface.setScene({ features: [feature], viewport: { kind: "automatic", padding: 70, maxZoom: 16 } });
  }
  updateRouteControls();
}

function renderCandidateDetail(candidate: DiscoveryCandidate): void {
  const detail = currentCollection;
  if (!detail) return;
  candidateDetail.hidden = false;
  candidateDetailTitle.textContent = candidate.displayName;
  candidateLocation.textContent = candidate.researchedLocation.label;
  candidateClaims.replaceChildren();
  const criterionMap = new Map(detail.criteria.map((criterion) => [criterion.criterionRef, criterion]));
  const claimGrid = document.createElement("div");
  claimGrid.className = "claim-grid";
  for (const claim of candidate.claims) {
    const card = document.createElement("article");
    card.className = "claim";
    const header = document.createElement("div");
    header.className = "claim-header";
    const title = document.createElement("strong");
    title.textContent = criterionMap.get(claim.criterionRef)?.description ?? claim.criterionRef;
    const badge = document.createElement("span");
    badge.className = "claim-badge";
    badge.textContent = `${claim.status.replace("_", " ")} · ${claim.basis.replace("_", " ")}`;
    header.append(title, badge);
    card.append(header);
    if (claim.observedValue || claim.note) {
      const body = document.createElement("p");
      body.textContent = [claim.observedValue?.value, claim.note].filter(Boolean).join(" · ");
      card.append(body);
    }
    claimGrid.append(card);
  }
  candidateClaims.append(claimGrid);

  const sourceRefs = new Set([...candidate.researchedLocation.sourceRefs, ...candidate.claims.flatMap((claim) => claim.sourceRefs)]);
  const sourceMap = new Map(detail.sources.map((source) => [source.sourceRef, source]));
  candidateSources.replaceChildren();
  if (sourceRefs.size) {
    const sourceList = document.createElement("div");
    sourceList.className = "source-list";
    for (const sourceRef of sourceRefs) {
      const source = sourceMap.get(sourceRef);
      if (!source) continue;
      const link = document.createElement("a");
      link.href = source.url;
      link.target = "_blank";
      link.rel = "noopener noreferrer";
      link.textContent = source.title ?? source.url;
      sourceList.append(link);
    }
    candidateSources.append(sourceList);
  }
}

async function searchRouteOrigin(): Promise<void> {
  const query = routeOriginQuery.value.trim();
  if (!query) return;
  originAbort?.abort();
  const controller = new AbortController();
  originAbort = controller;
  const sequence = ++originRequest;
  routeOriginResults.replaceChildren();
  setStatus(routeOriginStatus, "Searching…");
  try {
    const places = await searchPlaces(query, { signal: controller.signal });
    if (sequence !== originRequest) return;
    if (!places.length) {
      setStatus(routeOriginStatus, "No places found.");
      return;
    }
    setStatus(routeOriginStatus, `${places.length} result${places.length === 1 ? "" : "s"}. Select one.`);
    renderOriginResults(places);
  } catch (error) {
    if (controller.signal.aborted || sequence !== originRequest) return;
    setStatus(routeOriginStatus, error instanceof PlaceApiError ? error.message : "Start search is temporarily unavailable.", "error");
  }
}

function renderOriginResults(places: readonly Place[]): void {
  routeOriginResults.replaceChildren();
  for (const place of places) {
    const item = document.createElement("li");
    const button = document.createElement("button");
    button.type = "button";
    button.className = "place-result";
    const title = document.createElement("strong");
    title.textContent = place.displayLabel;
    const meta = document.createElement("span");
    meta.textContent = [place.address.postcode, place.address.city, place.address.country].filter(Boolean).join(" · ");
    button.append(title, meta);
    button.addEventListener("click", () => {
      routeOrigin = place;
      routeOriginResults.replaceChildren();
      setStatus(routeOriginStatus, `Selected: ${place.displayLabel}`, "success");
      invalidateRoute("Start changed. Request the route again.");
      updateRouteControls();
    });
    item.append(button);
    routeOriginResults.append(item);
  }
}

async function requestSelectedRoute(): Promise<void> {
  const origin = routeOrigin;
  const destination = selectedCandidate?.researchedLocation.coordinate;
  if (!origin || !destination || routePending) return;
  routeAbort?.abort();
  const controller = new AbortController();
  routeAbort = controller;
  const sequence = ++routeRequestSequence;
  routePending = true;
  setStatus(routeStatus, "Routing…");
  routeSummary.hidden = true;
  updateRouteControls();
  try {
    const route = await requestRoute(
      { origin: origin.coordinate, destination, profile: routeProfile.value as TravelProfile },
      { signal: controller.signal },
    );
    if (sequence !== routeRequestSequence) return;
    surface.setRoute(route);
    routeRendered = true;
    renderRouteSummary(origin.displayLabel, selectedCandidate?.displayName ?? "Destination", route.distanceMeters, route.durationSeconds, route.profile);
    setStatus(routeStatus, "Route ready.", "success");
  } catch (error) {
    if (controller.signal.aborted || sequence !== routeRequestSequence) return;
    surface.clearRoute();
    routeRendered = false;
    routeSummary.hidden = true;
    setStatus(routeStatus, error instanceof RouteApiError ? error.message : "Routing is temporarily unavailable.", "error");
  } finally {
    if (sequence === routeRequestSequence) {
      routePending = false;
      if (routeAbort === controller) routeAbort = undefined;
      updateRouteControls();
    }
  }
}

function clearRoute(message: string): void {
  routeAbort?.abort();
  routeAbort = undefined;
  routeRequestSequence += 1;
  routePending = false;
  surface.clearRoute();
  routeRendered = false;
  routeSummary.hidden = true;
  setStatus(routeStatus, message);
  updateRouteControls();
}

function invalidateRoute(message: string): void {
  const active = routePending || routeRendered;
  routeAbort?.abort();
  routeAbort = undefined;
  routeRequestSequence += 1;
  routePending = false;
  if (routeRendered) surface.clearRoute();
  routeRendered = false;
  routeSummary.hidden = true;
  if (active) setStatus(routeStatus, message);
  updateRouteControls();
}

function updateRouteControls(): void {
  const destinationAvailable = Boolean(selectedCandidate?.researchedLocation.coordinate);
  requestRouteButton.disabled = !routeOrigin || !destinationAvailable || routePending;
  clearRouteButton.disabled = !routeRendered && !routePending;
}

function renderRouteSummary(origin: string, destination: string, distanceMeters: number, durationSeconds: number, profile: TravelProfile): void {
  const title = document.createElement("strong");
  title.textContent = `${formatDistance(distanceMeters)} · ${formatDuration(durationSeconds)}`;
  const endpoints = document.createElement("span");
  endpoints.textContent = `${origin} → ${destination}`;
  const mode = document.createElement("small");
  mode.textContent = profile[0] + profile.slice(1).toLowerCase();
  routeSummary.replaceChildren(title, endpoints, mode);
  routeSummary.hidden = false;
}

function setStatus(element: HTMLElement, message: string, kind?: "success" | "error"): void {
  element.textContent = message;
  element.classList.toggle("success", kind === "success");
  element.classList.toggle("error", kind === "error");
}

function apiMessage(error: unknown, fallback: string): string {
  return error instanceof DiscoveryApiError ? error.message : fallback;
}

function mappedCount(detail: DiscoveryDetail): number {
  return detail.candidates.filter((candidate) => candidate.researchedLocation.coordinate !== null).length;
}

function formatRadius(meters: number): string {
  return meters < 1000 ? `${meters} m` : `${(meters / 1000).toFixed(meters % 1000 ? 1 : 0)} km`;
}

function formatDate(value: string): string {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString();
}

function formatDistance(meters: number): string {
  return meters < 1000 ? `${Math.round(meters)} m` : `${(meters / 1000).toFixed(meters < 10_000 ? 1 : 0)} km`;
}

function formatDuration(seconds: number): string {
  const minutes = Math.max(1, Math.round(seconds / 60));
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return remainder ? `${hours} h ${remainder} min` : `${hours} h`;
}

window.addEventListener(
  "beforeunload",
  () => {
    promptAbort?.abort();
    importAbort?.abort();
    collectionAbort?.abort();
    originAbort?.abort();
    routeAbort?.abort();
    surface.destroy();
  },
  { once: true },
);
