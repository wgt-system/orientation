import { Map, Marker, NavigationControl } from "maplibre-gl";
import {
  resolveViewport,
  type ResolvedViewport,
  type SpatialFeature,
  type SpatialActionActivatedEvent,
  type SpatialFeatureSelectedEvent,
  type SpatialResourceActivatedEvent,
  type SpatialScene,
} from "./model";
import { SpatialSceneController } from "./scene-controller";

export type OrientationMapStatus = "initializing" | "ready" | "error" | "destroyed";

export type OrientationMapStatusEvent = Readonly<{
  status: OrientationMapStatus;
}>;

export type OrientationMapCallbacks = Readonly<{
  onFeatureSelected?: (event: SpatialFeatureSelectedEvent) => void;
  onResourceActivated?: (event: SpatialResourceActivatedEvent) => void;
  onActionActivated?: (event: SpatialActionActivatedEvent) => void;
  onStatusChanged?: (event: OrientationMapStatusEvent) => void;
}>;

export class OrientationMapSurface {
  private readonly sceneController = new SpatialSceneController();
  private readonly callbacks: OrientationMapCallbacks;
  private readonly map: Map;
  private markers: Marker[] = [];
  private status: OrientationMapStatus = "initializing";

  constructor(container: HTMLElement, callbacks: OrientationMapCallbacks = {}) {
    this.callbacks = callbacks;
    this.emitStatus();

    try {
      this.map = new Map({
        container,
        style: "https://demotiles.maplibre.org/style.json",
        center: [0, 0],
        zoom: 1.5,
      });
      this.map.addControl(new NavigationControl(), "top-right");
      this.map.once("load", () => {
        if (this.status !== "destroyed") {
          this.setStatus("ready");
        }
      });
      this.map.on("error", () => {
        if (this.status !== "destroyed") {
          this.setStatus("error");
        }
      });
    } catch (error) {
      this.status = "error";
      this.emitStatus();
      throw error;
    }
  }

  get lifecycle(): OrientationMapStatus {
    return this.status;
  }

  setScene(scene: SpatialScene): void {
    this.assertUsable();
    const snapshot = this.sceneController.replace(scene);
    const viewport = resolveViewport(snapshot);

    this.clearMarkers();
    for (const feature of snapshot.features) {
      this.markers.push(this.createMarker(feature));
    }
    this.applyViewport(viewport);
  }

  activateResource(featureRef: string, resourceRef: string): void {
    this.assertUsable();
    this.callbacks.onResourceActivated?.(this.sceneController.activateResource(featureRef, resourceRef));
  }

  activateAction(featureRef: string, actionRef: string): void {
    this.assertUsable();
    this.callbacks.onActionActivated?.(this.sceneController.activateAction(featureRef, actionRef));
  }

  destroy(): void {
    if (this.status === "destroyed") {
      return;
    }

    this.clearMarkers();
    this.sceneController.clear();
    this.map.remove();
    this.setStatus("destroyed");
  }

  private createMarker(feature: SpatialFeature): Marker {
    const element = document.createElement("button");
    element.type = "button";
    element.className = "orientation-marker";
    element.setAttribute("aria-label", feature.title);
    element.addEventListener("click", () => {
      this.callbacks.onFeatureSelected?.(this.sceneController.select(feature.ref));
    });

    return new Marker({ element })
      .setLngLat([feature.coordinate.longitude, feature.coordinate.latitude])
      .addTo(this.map);
  }

  private applyViewport(viewport: ResolvedViewport): void {
    switch (viewport.kind) {
      case "empty":
      case "preserve":
        return;
      case "focus":
        this.map.flyTo({
          center: [viewport.coordinate.longitude, viewport.coordinate.latitude],
          zoom: viewport.zoom,
          duration: 0,
        });
        return;
      case "fit":
        this.map.fitBounds(
          [
            [viewport.bounds.southWest.longitude, viewport.bounds.southWest.latitude],
            [viewport.bounds.northEast.longitude, viewport.bounds.northEast.latitude],
          ],
          { padding: viewport.padding, maxZoom: viewport.maxZoom, duration: 0 },
        );
        return;
    }
  }

  private clearMarkers(): void {
    for (const marker of this.markers) {
      marker.remove();
    }
    this.markers = [];
  }

  private assertUsable(): void {
    if (this.status === "destroyed") {
      throw new Error("Orientation map surface has been destroyed.");
    }
  }

  private setStatus(status: OrientationMapStatus): void {
    if (this.status === status) {
      return;
    }
    this.status = status;
    this.emitStatus();
  }

  private emitStatus(): void {
    this.callbacks.onStatusChanged?.({ status: this.status });
  }
}
