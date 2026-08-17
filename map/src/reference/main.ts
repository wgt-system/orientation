import "maplibre-gl/dist/maplibre-gl.css";
import "../styles.css";

import { OrientationMapSurface } from "../lib/map-surface";
import { renderFeatureDetails } from "../lib/feature-details";
import type { PositionFix } from "../lib/current-location";
import type { SpatialFeature, SpatialFeatureSelectedEvent, SpatialScene } from "../lib/model";
import type { TravelProfile } from "../lib/route-overlay";
import { PlaceApiError, reversePlace, searchPlaces, type Place } from "./place-api";
import { placeToSpatialFeature } from "./place-feature";
import { requestRoute, RouteApiError } from "./route-api";

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
const routeOriginInput = document.querySelector<HTMLInputElement>("#route-origin-query");
const routeOriginSearch = document.querySelector<HTMLButtonElement>("#route-origin-search");
const routeOriginStatus = document.querySelector<HTMLElement>("#route-origin-status");
const routeOriginResults = document.querySelector<HTMLOListElement>("#route-origin-results");
const routeDestinationInput = document.querySelector<HTMLInputElement>("#route-destination-query");
const routeDestinationSearch = document.querySelector<HTMLButtonElement>("#route-destination-search");
const routeDestinationStatus = document.querySelector<HTMLElement>("#route-destination-status");
const routeDestinationResults = document.querySelector<HTMLOListElement>("#route-destination-results");
const routeProfile = document.querySelector<HTMLSelectElement>("#route-profile");
const requestRouteButton = document.querySelector<HTMLButtonElement>("#request-route");
const clearRouteButton = document.querySelector<HTMLButtonElement>("#clear-route");
const routeStatus = document.querySelector<HTMLElement>("#route-status");
const routeSummary = document.querySelector<HTMLElement>("#route-summary");

if (
  !mapContainer ||
  !selection ||
  !searchForm ||
  !queryInput ||
  !searchStatus ||
  !results ||
  !identifyCenterButton ||
  !reverseStatus ||
  !locationStatus ||
  !setLocationButton ||
  !updateLocationButton ||
  !clearLocationButton ||
  !mapStatus ||
  !routeOriginInput ||
  !routeOriginSearch ||
  !routeOriginStatus ||
  !routeOriginResults ||
  !routeDestinationInput ||
  !routeDestinationSearch ||
  !routeDestinationStatus ||
  !routeDestinationResults ||
  !routeProfile ||
  !requestRouteButton ||
  !clearRouteButton ||
  !routeStatus ||
  !routeSummary
) {
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
const routeOriginInputElement = routeOriginInput;
const routeOriginSearchElement = routeOriginSearch;
const routeOriginStatusElement = routeOriginStatus;
const routeOriginResultsElement = routeOriginResults;
const routeDestinationInputElement = routeDestinationInput;
const routeDestinationSearchElement = routeDestinationSearch;
const routeDestinationStatusElement = routeDestinationStatus;
const routeDestinationResultsElement = routeDestinationResults;
const routeProfileElement = routeProfile;
const requestRouteElement = requestRouteButton;
const clearRouteElement = clearRouteButton;
const routeStatusElement = routeStatus;
const routeSummaryElement = routeSummary;

let currentScene: SpatialScene = createDemoScene();
let searchAbort: AbortController | undefined;
let reverseAbort: AbortController | undefined;
let searchRequest = 0;
let reverseRequest = 0;
let routeOrigin: Place | undefined;
let routeDestination: Place | undefined;
let routeOriginAbort: AbortController | undefined;
let routeDestinationAbort: AbortController | undefined;
let routeAbort: AbortController | undefined;
let routeOriginRequest = 0;
let routeDestinationRequest = 0;
let routeRequest = 0;
let routePending = false;
let hasRenderedRoute = false;

function renderMapStatus(status: "initializing" | "ready" | "error" | "destroyed"): void {
  mapStatusElement.hidden = status !== "error";
  mapStatusElement.textContent =
    status === "error" ? "Map unavailable. The basemap style or tiles could not be loaded." : "";
}

function renderSelection(event: SpatialFeatureSelectedEvent): void {
  const feature = currentScene.features.find(
    (candidate) => candidate.ref === event.featureRef && candidate.sourceRef === event.sourceRef,
  );
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
    searchStatusElement.textContent =
      error instanceof PlaceApiError ? error.message : "Place search is temporarily unavailable.";
  }
});

function renderSearchResults(places: readonly Place[]): void {
  resultsElement.replaceChildren();
  if (!places.length) {
    searchStatusElement.textContent = "No places found.";
    return;
  }
  searchStatusElement.textContent = `${places.length} place${places.length === 1 ? "" : "s"} found.`;
  renderPlaceList(places, resultsElement, replaceWithPlace);
}

function renderPlaceList(
  places: readonly Place[],
  target: HTMLOListElement,
  onSelect: (place: Place) => void,
): void {
  target.replaceChildren();
  for (const place of places) {
    const item = document.createElement("li");
    const button = document.createElement("button");
    button.type = "button";
    button.className = "place-result";
    button.addEventListener("click", () => onSelect(place));
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
    target.append(item);
  }
}

function placeDisambiguation(place: Place): string {
  const address = place.address;
  return [
    address.street && address.houseNumber ? `${address.street} ${address.houseNumber}` : address.street,
    address.postcode,
    address.city,
    address.state,
    address.country,
  ]
    .filter(Boolean)
    .join(" · ");
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
    reverseStatusElement.textContent =
      error instanceof PlaceApiError ? error.message : "Place lookup is temporarily unavailable.";
  }
});

function bindRouteEndpointSearch(
  input: HTMLInputElement,
  button: HTMLButtonElement,
  status: HTMLElement,
  list: HTMLOListElement,
  role: "origin" | "destination",
): void {
  const execute = async (): Promise<void> => {
    const query = input.value.trim();
    if (!query) return;

    const previousAbort = role === "origin" ? routeOriginAbort : routeDestinationAbort;
    previousAbort?.abort();
    const controller = new AbortController();
    if (role === "origin") routeOriginAbort = controller;
    else routeDestinationAbort = controller;

    const request = role === "origin" ? ++routeOriginRequest : ++routeDestinationRequest;
    list.replaceChildren();
    status.textContent = "Searching…";
    try {
      const places = await searchPlaces(query, { signal: controller.signal });
      const currentRequest = role === "origin" ? routeOriginRequest : routeDestinationRequest;
      if (request !== currentRequest) return;
      if (!places.length) {
        status.textContent = "No places found.";
        return;
      }
      status.textContent = `${places.length} place${places.length === 1 ? "" : "s"} found. Select one.`;
      renderPlaceList(places, list, (place) => selectRouteEndpoint(role, place));
    } catch (error) {
      const currentRequest = role === "origin" ? routeOriginRequest : routeDestinationRequest;
      if (controller.signal.aborted || request !== currentRequest) return;
      status.textContent =
        error instanceof PlaceApiError ? error.message : "Place search is temporarily unavailable.";
    }
  };

  button.addEventListener("click", () => void execute());
  input.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      void execute();
    }
  });
}

function selectRouteEndpoint(role: "origin" | "destination", place: Place): void {
  if (role === "origin") {
    routeOrigin = place;
    routeOriginStatusElement.textContent = `Selected: ${place.displayLabel}`;
    routeOriginResultsElement.replaceChildren();
  } else {
    routeDestination = place;
    routeDestinationStatusElement.textContent = `Selected: ${place.displayLabel}`;
    routeDestinationResultsElement.replaceChildren();
  }
  invalidateRouteActivity("Route endpoints changed. Request the route again.");
  updateRouteControls();
}

bindRouteEndpointSearch(
  routeOriginInputElement,
  routeOriginSearchElement,
  routeOriginStatusElement,
  routeOriginResultsElement,
  "origin",
);
bindRouteEndpointSearch(
  routeDestinationInputElement,
  routeDestinationSearchElement,
  routeDestinationStatusElement,
  routeDestinationResultsElement,
  "destination",
);

routeProfileElement.addEventListener("change", () => {
  invalidateRouteActivity("Travel profile changed. Request the route again.");
});

requestRouteElement.addEventListener("click", async () => {
  const origin = routeOrigin;
  const destination = routeDestination;
  if (!origin || !destination || routePending) return;

  routeAbort?.abort();
  const controller = new AbortController();
  routeAbort = controller;
  const request = ++routeRequest;
  const profile = routeProfileElement.value as TravelProfile;
  routePending = true;
  routeStatusElement.textContent = "Routing…";
  routeSummaryElement.hidden = true;
  updateRouteControls();

  try {
    const route = await requestRoute(
      {
        origin: origin.coordinate,
        destination: destination.coordinate,
        profile,
      },
      { signal: controller.signal },
    );
    if (request !== routeRequest) return;
    surface.setRoute(route);
    hasRenderedRoute = true;
    renderRouteSummary(origin.displayLabel, destination.displayLabel, route.distanceMeters, route.durationSeconds, route.profile);
    routeStatusElement.textContent = "Route ready.";
  } catch (error) {
    if (controller.signal.aborted || request !== routeRequest) return;
    surface.clearRoute();
    hasRenderedRoute = false;
    routeSummaryElement.hidden = true;
    routeStatusElement.textContent =
      error instanceof RouteApiError ? error.message : "Routing is temporarily unavailable.";
  } finally {
    if (request === routeRequest) {
      routePending = false;
      if (routeAbort === controller) routeAbort = undefined;
      updateRouteControls();
    }
  }
});

clearRouteElement.addEventListener("click", () => {
  routeAbort?.abort();
  routeAbort = undefined;
  routeRequest += 1;
  routePending = false;
  surface.clearRoute();
  hasRenderedRoute = false;
  routeSummaryElement.hidden = true;
  routeStatusElement.textContent = "No route requested.";
  updateRouteControls();
});

function invalidateRouteActivity(message: string): void {
  const hadActivity = routePending || hasRenderedRoute;
  routeAbort?.abort();
  routeAbort = undefined;
  routeRequest += 1;
  routePending = false;
  if (hasRenderedRoute) surface.clearRoute();
  hasRenderedRoute = false;
  routeSummaryElement.hidden = true;
  if (hadActivity) routeStatusElement.textContent = message;
  updateRouteControls();
}

function updateRouteControls(): void {
  requestRouteElement.disabled = !routeOrigin || !routeDestination || routePending;
  clearRouteElement.disabled = !hasRenderedRoute && !routePending;
}

function renderRouteSummary(
  originLabel: string,
  destinationLabel: string,
  distanceMeters: number,
  durationSeconds: number,
  profile: TravelProfile,
): void {
  const title = document.createElement("strong");
  title.textContent = `${formatDistance(distanceMeters)} · ${formatDuration(durationSeconds)}`;
  const endpoints = document.createElement("span");
  endpoints.textContent = `${originLabel} → ${destinationLabel}`;
  const mode = document.createElement("small");
  mode.textContent = profileLabel(profile);
  routeSummaryElement.replaceChildren(title, endpoints, mode);
  routeSummaryElement.hidden = false;
}

function formatDistance(meters: number): string {
  if (meters < 1000) return `${Math.round(meters)} m`;
  return `${(meters / 1000).toFixed(meters < 10_000 ? 1 : 0)} km`;
}

function formatDuration(seconds: number): string {
  const minutes = Math.max(1, Math.round(seconds / 60));
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  const remaining = minutes % 60;
  return remaining ? `${hours} h ${remaining} min` : `${hours} h`;
}

function profileLabel(profile: TravelProfile): string {
  switch (profile) {
    case "DRIVING":
      return "Driving";
    case "CYCLING":
      return "Cycling";
    case "WALKING":
      return "Walking";
  }
}

surface.setScene(currentScene);
updateRouteControls();

const samplePositions: PositionFix[] = [
  {
    coordinate: { longitude: 10.01, latitude: 53.55 },
    accuracyMeters: 3500,
    observedAt: "2026-08-14T12:00:00.000Z",
  },
  {
    coordinate: { longitude: 10.08, latitude: 53.58 },
    accuracyMeters: 1800,
    observedAt: "2026-08-14T12:01:00.000Z",
  },
];
let samplePositionIndex = 0;
function setSamplePosition(): void {
  const sample = samplePositions[samplePositionIndex]!;
  surface.setCurrentPosition(sample);
  locationStatusElement.textContent = `Sample position · ${sample.accuracyMeters} m accuracy`;
}
setLocationElement.addEventListener("click", () => {
  samplePositionIndex = 0;
  setSamplePosition();
});
updateLocationElement.addEventListener("click", () => {
  samplePositionIndex = Math.min(samplePositionIndex + 1, samplePositions.length - 1);
  setSamplePosition();
});
clearLocationElement.addEventListener("click", () => {
  surface.clearCurrentPosition();
  locationStatusElement.textContent = "No position supplied.";
});
window.addEventListener(
  "beforeunload",
  () => {
    searchAbort?.abort();
    reverseAbort?.abort();
    routeOriginAbort?.abort();
    routeDestinationAbort?.abort();
    routeAbort?.abort();
    surface.destroy();
  },
  { once: true },
);

function createDemoScene(): SpatialScene {
  return {
    features: [
      {
        ref: "reference/hamburg",
        sourceRef: "reference/provider",
        coordinate: { longitude: 9.9937, latitude: 53.5511 },
        title: "Hamburg",
        subtitle: "Generic reference feature",
        information: [
          {
            title: "Example details",
            rows: [
              { label: "Category", value: "Provider-neutral example" },
              { label: "Availability", value: "Presented by the host" },
            ],
          },
        ],
        resources: [
          { ref: "reference/resource", label: "Example external resource", uri: "https://example.com" },
        ],
        actions: [{ ref: "route-here", label: "Route here" }],
      },
      {
        ref: "reference/berlin",
        sourceRef: "reference/provider",
        coordinate: { longitude: 13.405, latitude: 52.52 },
        title: "Berlin",
        subtitle: "A second provider-neutral feature",
      },
      {
        ref: "reference/karlsruhe",
        sourceRef: "reference/provider",
        coordinate: { longitude: 8.4037, latitude: 49.0069 },
        title: "Karlsruhe",
        subtitle: "A third provider-neutral feature",
      },
    ],
  };
}
