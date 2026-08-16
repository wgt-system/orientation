import "maplibre-gl/dist/maplibre-gl.css";
import "../styles.css";

import { OrientationMapSurface } from "../lib/map-surface";
import { renderFeatureDetails } from "../lib/feature-details";
import type { PositionFix } from "../lib/current-location";
import type { SpatialFeature, SpatialFeatureSelectedEvent, SpatialScene } from "../lib/model";
import { PlaceApiError, reversePlace, searchPlaces, type Place } from "./place-api";
import { placeToSpatialFeature } from "./place-feature";

const mapContainer = document.querySelector<HTMLElement>("#map");
const selection = document.querySelector<HTMLElement>("#selection");
const searchForm = document.querySelector<HTMLFormElement>("#place-search");
const queryInput = document.querySelector<HTMLInputElement>("#place-query");
const searchStatus = document.querySelector<HTMLElement>("#place-search-status");
const results = document.querySelector<HTMLOListElement>("#place-results");
const identifyCenterButton = document.querySelector<HTMLButtonElement>("#identify-center");
const reverseStatus = document.querySelector<HTMLElement>("#reverse-status");
const locationStatus = document.querySelector<HTMLElement>("#location-status");
const setLocationButton = document.querySelector<HTMLButtonElement>("#set-location");
const updateLocationButton = document.querySelector<HTMLButtonElement>("#update-location");
const clearLocationButton = document.querySelector<HTMLButtonElement>("#clear-location");
const mapStatus = document.querySelector<HTMLElement>("#map-status");

if (!mapContainer || !selection || !searchForm || !queryInput || !searchStatus || !results || !identifyCenterButton || !reverseStatus || !locationStatus || !setLocationButton || !updateLocationButton || !clearLocationButton || !mapStatus) {
  throw new Error("Reference host controls are incomplete.");
}

const mapElement = mapContainer;
const selectionElement = selection;
const formElement = searchForm;
const inputElement = queryInput;
const searchStatusElement = searchStatus;
const resultsElement = results;
const identifyElement = identifyCenterButton;
const reverseStatusElement = reverseStatus;
const locationStatusElement = locationStatus;
const setLocationElement = setLocationButton;
const updateLocationElement = updateLocationButton;
const clearLocationElement = clearLocationButton;
const mapStatusElement = mapStatus;

let currentScene: SpatialScene = createDemoScene();
let searchAbort: AbortController | undefined;
let reverseAbort: AbortController | undefined;
let searchRequest = 0;
let reverseRequest = 0;

function renderMapStatus(status: "initializing" | "ready" | "error" | "destroyed"): void {
  mapStatusElement.hidden = status !== "error";
  mapStatusElement.textContent = status === "error" ? "Map unavailable. The basemap style or tiles could not be loaded." : "";
}

function renderSelection(event: SpatialFeatureSelectedEvent): void {
  const feature = currentScene.features.find((candidate) => candidate.ref === event.featureRef && candidate.sourceRef === event.sourceRef);
  if (feature) renderFeature(feature);
}

function renderFeature(feature: SpatialFeature): void {
  renderFeatureDetails(selectionElement, feature, {
    onResourceActivated: (activation) => {
      const message = document.createElement("small");
      message.textContent = `Host received resource activation: ${activation.resourceRef}`;
      selectionElement.append(message);
    },
    onActionActivated: (activation) => {
      const message = document.createElement("small");
      message.textContent = `Host received action activation: ${activation.actionRef}`;
      selectionElement.append(message);
    },
  });
}

const surface = new OrientationMapSurface(mapElement, {
  onFeatureSelected: renderSelection,
  onStatusChanged: ({ status }) => renderMapStatus(status),
});

function replaceWithPlace(place: Place): void {
  const feature = placeToSpatialFeature(place);
  currentScene = { features: [feature], viewport: { kind: "automatic", maxZoom: 16 } };
  surface.setScene(currentScene);
  renderFeature(feature);
}

formElement.addEventListener("submit", async (event) => {
  event.preventDefault();
  const query = inputElement.value.trim();
  if (!query) return;
  searchAbort?.abort();
  const controller = new AbortController();
  searchAbort = controller;
  const request = ++searchRequest;
  resultsElement.replaceChildren();
  searchStatusElement.textContent = "Searching…";
  try {
    const places = await searchPlaces(query, { signal: controller.signal });
    if (request !== searchRequest) return;
    renderSearchResults(places);
  } catch (error) {
    if (controller.signal.aborted || request !== searchRequest) return;
    searchStatusElement.textContent = error instanceof PlaceApiError ? error.message : "Place search is temporarily unavailable.";
  }
});

function renderSearchResults(places: readonly Place[]): void {
  resultsElement.replaceChildren();
  if (!places.length) {
    searchStatusElement.textContent = "No places found.";
    return;
  }
  searchStatusElement.textContent = `${places.length} place${places.length === 1 ? "" : "s"} found.`;
  for (const place of places) {
    const item = document.createElement("li");
    const button = document.createElement("button");
    button.type = "button";
    button.className = "place-result";
    button.addEventListener("click", () => replaceWithPlace(place));
    const title = document.createElement("strong");
    title.textContent = place.displayLabel;
    button.append(title);
    const disambiguation = placeDisambiguation(place);
    if (disambiguation) {
      const text = document.createElement("span");
      text.textContent = disambiguation;
      button.append(text);
    }
    item.append(button);
    resultsElement.append(item);
  }
}

function placeDisambiguation(place: Place): string {
  const address = place.address;
  return [address.street && address.houseNumber ? `${address.street} ${address.houseNumber}` : address.street, address.postcode, address.city, address.state, address.country].filter(Boolean).join(" · ");
}

identifyElement.addEventListener("click", async () => {
  reverseAbort?.abort();
  const controller = new AbortController();
  reverseAbort = controller;
  const request = ++reverseRequest;
  reverseStatusElement.textContent = "Identifying map center…";
  try {
    const place = await reversePlace(surface.centerCoordinate(), { signal: controller.signal });
    if (request !== reverseRequest) return;
    if (!place) {
      reverseStatusElement.textContent = "No place found at the map center.";
      return;
    }
    replaceWithPlace(place);
    reverseStatusElement.textContent = "Map center identified.";
  } catch (error) {
    if (controller.signal.aborted || request !== reverseRequest) return;
    reverseStatusElement.textContent = error instanceof PlaceApiError ? error.message : "Place lookup is temporarily unavailable.";
  }
});

surface.setScene(currentScene);

const samplePositions: PositionFix[] = [
  { coordinate: { longitude: 10.01, latitude: 53.55 }, accuracyMeters: 3500, observedAt: "2026-08-14T12:00:00.000Z" },
  { coordinate: { longitude: 10.08, latitude: 53.58 }, accuracyMeters: 1800, observedAt: "2026-08-14T12:01:00.000Z" },
];
let samplePositionIndex = 0;
function setSamplePosition(): void {
  const sample = samplePositions[samplePositionIndex]!;
  surface.setCurrentPosition(sample);
  locationStatusElement.textContent = `Sample position · ${sample.accuracyMeters} m accuracy`;
}
setLocationElement.addEventListener("click", () => { samplePositionIndex = 0; setSamplePosition(); });
updateLocationElement.addEventListener("click", () => { samplePositionIndex = Math.min(samplePositionIndex + 1, samplePositions.length - 1); setSamplePosition(); });
clearLocationElement.addEventListener("click", () => { surface.clearCurrentPosition(); locationStatusElement.textContent = "No position supplied."; });
window.addEventListener("beforeunload", () => surface.destroy(), { once: true });

function createDemoScene(): SpatialScene {
  return {
    features: [
      {
        ref: "reference/hamburg", sourceRef: "reference/provider", coordinate: { longitude: 9.9937, latitude: 53.5511 }, title: "Hamburg", subtitle: "Generic reference feature",
        information: [{ title: "Example details", rows: [{ label: "Category", value: "Provider-neutral example" }, { label: "Availability", value: "Presented by the host" }] }],
        resources: [{ ref: "reference/resource", label: "Example external resource", uri: "https://example.com" }], actions: [{ ref: "route-here", label: "Route here" }],
      },
      { ref: "reference/berlin", sourceRef: "reference/provider", coordinate: { longitude: 13.405, latitude: 52.52 }, title: "Berlin", subtitle: "A second provider-neutral feature" },
      { ref: "reference/karlsruhe", sourceRef: "reference/provider", coordinate: { longitude: 8.4037, latitude: 49.0069 }, title: "Karlsruhe", subtitle: "A third provider-neutral feature" },
    ],
  };
}
