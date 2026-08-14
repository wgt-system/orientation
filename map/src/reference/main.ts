import "maplibre-gl/dist/maplibre-gl.css";
import "../styles.css";

import { OrientationMapSurface } from "../lib/map-surface";
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

  const resourceLines =
    feature.resources?.map((resource) => `Resource: ${resource.label}`) ?? [];
  const actionLines =
    feature.actions?.map((action) => `Action: ${action.label}`) ?? [];

  selectionElement.replaceChildren();

  const title = document.createElement("strong");
  title.textContent = feature.title;
  selectionElement.append(title);

  if (feature.subtitle) {
    const subtitle = document.createElement("span");
    subtitle.textContent = feature.subtitle;
    selectionElement.append(subtitle);
  }

  for (const line of [...resourceLines, ...actionLines]) {
    const row = document.createElement("small");
    row.textContent = line;
    selectionElement.append(row);
  }

  const ref = document.createElement("code");
  ref.textContent = feature.ref;
  selectionElement.append(ref);
}

const surface = new OrientationMapSurface(mapContainer, {
  onFeatureSelected: renderSelection,
});

surface.setScene(scene);

window.addEventListener("beforeunload", () => surface.destroy(), { once: true });
