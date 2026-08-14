import "maplibre-gl/dist/maplibre-gl.css";
import "../styles.css";

import { OrientationMapSurface } from "../lib/map-surface";
import { renderFeatureDetails } from "../lib/feature-details";
import type { PositionFix } from "../lib/current-location";
import type { SpatialFeature, SpatialFeatureSelectedEvent, SpatialScene } from "../lib/model";

const mapContainer = document.querySelector<HTMLElement>("#map");
const selection = document.querySelector<HTMLElement>("#selection");

if (!mapContainer || !selection) {
  throw new Error("Reference host DOM is incomplete.");
}

const selectionElement = selection;
const locationStatus = document.querySelector<HTMLElement>("#location-status");
const setLocationButton = document.querySelector<HTMLButtonElement>("#set-location");
const updateLocationButton = document.querySelector<HTMLButtonElement>("#update-location");
const clearLocationButton = document.querySelector<HTMLButtonElement>("#clear-location");

if (!locationStatus || !setLocationButton || !updateLocationButton || !clearLocationButton) {
  throw new Error("Reference host location controls are incomplete.");
}
const locationStatusElement = locationStatus;

const scene: SpatialScene = {
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
        {
          ref: "reference/resource",
          label: "Example external resource",
          uri: "https://example.com",
        },
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

function renderSelection(event: SpatialFeatureSelectedEvent): void {
  const feature = scene.features.find(
    (candidate) => candidate.ref === event.featureRef && candidate.sourceRef === event.sourceRef,
  );
  if (!feature) {
    return;
  }

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

const surface = new OrientationMapSurface(mapContainer, {
  onFeatureSelected: renderSelection,
});

surface.setScene(scene);

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

setLocationButton.addEventListener("click", () => {
  samplePositionIndex = 0;
  setSamplePosition();
});

updateLocationButton.addEventListener("click", () => {
  samplePositionIndex = Math.min(samplePositionIndex + 1, samplePositions.length - 1);
  setSamplePosition();
});

clearLocationButton.addEventListener("click", () => {
  surface.clearCurrentPosition();
  locationStatusElement.textContent = "No position supplied.";
});

window.addEventListener("beforeunload", () => surface.destroy(), { once: true });
