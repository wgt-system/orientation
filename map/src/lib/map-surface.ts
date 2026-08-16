import { Map, Marker, NavigationControl, setWorkerUrl, type GeoJSONSource } from "maplibre-gl";
import workerUrl from "maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url";
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
import {
  createAccuracyGeometry,
  CurrentLocationController,
  type PositionFix,
} from "./current-location";

const CURRENT_LOCATION_SOURCE = "orientation-current-location";
const EMPTY_FEATURE_COLLECTION = { type: "FeatureCollection", features: [] } as const;
const VECTOR_BASEMAP_SOURCE = "openmaptiles";
export const BASEMAP_READINESS_TIMEOUT_MS = 15_000;
export const DEFAULT_BASEMAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";

setWorkerUrl(workerUrl);

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
  private readonly currentLocationController = new CurrentLocationController();
  private readonly callbacks: OrientationMapCallbacks;
  private readonly map: Map;
  private markers: Marker[] = [];
  private status: OrientationMapStatus = "initializing";
  private readinessTimer: number | undefined;

  constructor(container: HTMLElement, callbacks: OrientationMapCallbacks = {}) {
    this.callbacks = callbacks;
    this.emitStatus();

    try {
      this.map = new Map({
        container,
        style: DEFAULT_BASEMAP_STYLE_URL,
        center: [0, 0],
        zoom: 1.5,
      });
      this.readinessTimer = window.setTimeout(() => {
        if (this.status === "initializing") {
          this.setStatus("error");
        }
      }, BASEMAP_READINESS_TIMEOUT_MS);
      this.map.addControl(new NavigationControl(), "top-right");
      this.map.once("load", () => {
        if (this.status !== "destroyed") {
          this.installCurrentLocationLayers();
          this.renderCurrentLocation();
          this.waitForVectorBasemap();
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

  setCurrentPosition(position: PositionFix): void {
    this.assertUsable();
    this.currentLocationController.set(position);
    this.renderCurrentLocation();
  }

  clearCurrentPosition(): void {
    this.assertUsable();
    this.currentLocationController.clear();
    this.renderCurrentLocation();
  }

  currentPosition(): PositionFix | null {
    return this.currentLocationController.current();
  }

  feature(featureRef: string, sourceRef: string): SpatialFeature | undefined {
    this.assertUsable();
    return this.sceneController.find(featureRef, sourceRef);
  }

  featureMarkerCount(): number {
    return this.markers.length;
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
    this.currentLocationController.clear();
    this.removeCurrentLocationLayers();
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
            [viewport.bounds.west, viewport.bounds.south],
            [viewport.bounds.east, viewport.bounds.north],
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

  private installCurrentLocationLayers(): void {
    if (this.map.getSource(CURRENT_LOCATION_SOURCE)) {
      return;
    }

    this.map.addSource(CURRENT_LOCATION_SOURCE, {
      type: "geojson",
      data: EMPTY_FEATURE_COLLECTION,
    });
    this.map.addLayer({
      id: "orientation-current-location-accuracy",
      type: "fill",
      source: CURRENT_LOCATION_SOURCE,
      paint: {
        "fill-color": "#4d8290",
        "fill-opacity": 0.28,
      },
    });
    this.map.addLayer({
      id: "orientation-current-location-accuracy-outline",
      type: "line",
      source: CURRENT_LOCATION_SOURCE,
      paint: {
        "line-color": "#315a60",
        "line-opacity": 0.65,
        "line-width": 2,
      },
    });
    this.map.addLayer({
      id: "orientation-current-location-dot",
      type: "circle",
      source: CURRENT_LOCATION_SOURCE,
      paint: {
        "circle-color": "#163f49",
        "circle-radius": 6,
        "circle-stroke-color": "#f7f4ed",
        "circle-stroke-width": 2,
      },
    });
  }

  private renderCurrentLocation(): void {
    if (this.status === "destroyed" || !this.map.getSource(CURRENT_LOCATION_SOURCE)) {
      return;
    }

    const source = this.map.getSource(CURRENT_LOCATION_SOURCE) as GeoJSONSource | undefined;
    if (!source || source.type !== "geojson") {
      return;
    }

    const position = this.currentLocationController.current();
    source.setData(position ? createLocationFeatureCollection(position) : EMPTY_FEATURE_COLLECTION);
  }

  private removeCurrentLocationLayers(): void {
    if (this.map.getLayer("orientation-current-location-dot")) {
      this.map.removeLayer("orientation-current-location-dot");
    }
    if (this.map.getLayer("orientation-current-location-accuracy-outline")) {
      this.map.removeLayer("orientation-current-location-accuracy-outline");
    }
    if (this.map.getLayer("orientation-current-location-accuracy")) {
      this.map.removeLayer("orientation-current-location-accuracy");
    }
    if (this.map.getSource(CURRENT_LOCATION_SOURCE)) {
      this.map.removeSource(CURRENT_LOCATION_SOURCE);
    }
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
    if (status === "ready" || status === "error" || status === "destroyed") {
      this.clearReadinessTimer();
    }
    this.status = status;
    this.emitStatus();
  }

  private waitForVectorBasemap(): void {
    const settle = (): void => {
      if (this.status !== "initializing") {
        return;
      }
      if (!this.map.isSourceLoaded(VECTOR_BASEMAP_SOURCE)) {
        return;
      }
      this.map.off("sourcedata", settle);
      this.map.off("idle", settle);
      this.setStatus("ready");
    };

    if (this.map.isSourceLoaded(VECTOR_BASEMAP_SOURCE)) {
      this.setStatus("ready");
      return;
    }

    this.map.on("sourcedata", settle);
    this.map.on("idle", settle);
    settle();
  }

  private clearReadinessTimer(): void {
    if (this.readinessTimer !== undefined) {
      window.clearTimeout(this.readinessTimer);
      this.readinessTimer = undefined;
    }
  }

  private emitStatus(): void {
    this.callbacks.onStatusChanged?.({ status: this.status });
  }
}

function createLocationFeatureCollection(position: PositionFix) {
  const accuracy = createAccuracyGeometry(position);
  return {
    type: "FeatureCollection" as const,
    features: [
      ...(accuracy ? [accuracy] : []),
      {
        type: "Feature" as const,
        geometry: {
          type: "Point" as const,
          coordinates: [position.coordinate.longitude, position.coordinate.latitude],
        },
      },
    ],
  };
}
