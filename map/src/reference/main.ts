import "maplibre-gl/dist/maplibre-gl.css";
import "../styles.css";

import { OrientationMapSurface } from "../lib/map-surface";
import { renderFeatureDetails } from "../lib/feature-details";
import type { SpatialFeature, SpatialFeatureSelectedEvent, SpatialScene } from "../lib/model";

const mapContainer = document.querySelector<HTMLElement>("#map");
const selection = document.querySelector<HTMLElement>("#selection");

if (!mapContainer || !selection) {
  throw new Error("Reference host DOM is incomplete.");
}

const selectionElement = selection;

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

window.addEventListener("beforeunload", () => surface.destroy(), { once: true });
