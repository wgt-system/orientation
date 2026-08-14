import { Map, Marker, NavigationControl } from "maplibre-gl";
import type { SpatialFeature, SpatialScene } from "./model";
import { validateScene } from "./model";

export type OrientationMapCallbacks = Readonly<{
  onFeatureSelected?: (feature: SpatialFeature) => void;
}>;

export class OrientationMapSurface {
  private readonly map: Map;
  private markers: Marker[] = [];

  constructor(
    container: HTMLElement,
    private readonly callbacks: OrientationMapCallbacks = {},
  ) {
    this.map = new Map({
      container,
      style: "https://demotiles.maplibre.org/style.json",
      center: [10.4515, 51.1657],
      zoom: 4.6,
    });
    this.map.addControl(new NavigationControl(), "top-right");
  }

  setScene(scene: SpatialScene): void {
    validateScene(scene);
    this.clearMarkers();

    for (const feature of scene.features) {
      const element = document.createElement("button");
      element.type = "button";
      element.className = "orientation-marker";
      element.setAttribute("aria-label", feature.title);
      element.addEventListener("click", () => {
        this.callbacks.onFeatureSelected?.(feature);
      });

      const marker = new Marker({ element })
        .setLngLat([feature.coordinate.longitude, feature.coordinate.latitude])
        .addTo(this.map);

      this.markers.push(marker);
    }

    if (scene.features.length === 1) {
      const feature = scene.features[0];
      if (feature) {
        this.map.flyTo({
          center: [feature.coordinate.longitude, feature.coordinate.latitude],
          zoom: 11,
        });
      }
    }
  }

  destroy(): void {
    this.clearMarkers();
    this.map.remove();
  }

  private clearMarkers(): void {
    for (const marker of this.markers) {
      marker.remove();
    }
    this.markers = [];
  }
}
