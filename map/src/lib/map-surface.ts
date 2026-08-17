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
  type Coordinate,
} from "./model";
import { SpatialSceneController } from "./scene-controller";
import {
  createAccuracyGeometry,
  CurrentLocationController,
  type PositionFix,
} from "./current-location";
import {
  createJourneyFeatureCollection,
  JourneyOverlayController,
  resolveJourneyViewport,
  type JourneyOverlay,
  type JourneyViewportIntent,
  type ResolvedJourneyViewport,
} from "./journey-overlay";
import {
  resolveRouteViewport,
  RouteOverlayController,
  type Route,
  type RouteViewportIntent,
  type ResolvedRouteViewport,
} from "./route-overlay";

const CURRENT_LOCATION_SOURCE = "orientation-current-location";
const ROUTE_SOURCE = "orientation-route";
const ROUTE_CASING_LAYER = "orientation-route-casing";
const ROUTE_LINE_LAYER = "orientation-route-line";
const ROUTE_ORIGIN_LAYER = "orientation-route-origin";
const ROUTE_DESTINATION_LAYER = "orientation-route-destination";
const JOURNEY_SOURCE = "orientation-journey";
const JOURNEY_CASING_LAYER = "orientation-journey-casing";
const JOURNEY_WALK_LAYER = "orientation-journey-walk";
const JOURNEY_TRANSIT_LAYER = "orientation-journey-transit";
const JOURNEY_STOP_LAYER = "orientation-journey-transit-stop";
const JOURNEY_ORIGIN_LAYER = "orientation-journey-origin";
const JOURNEY_DESTINATION_LAYER = "orientation-journey-destination";
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
  private readonly routeController = new RouteOverlayController();
  private readonly journeyController = new JourneyOverlayController();
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
          this.installRouteLayers();
          this.installJourneyLayers();
          this.installCurrentLocationLayers();
          this.renderRoute();
          this.renderJourney();
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

  setRoute(route: Route, viewport: RouteViewportIntent = { kind: "fit" }): void {
    this.assertUsable();
    const snapshot = this.routeController.set(route);
    this.renderRoute();
    this.applyRouteViewport(resolveRouteViewport(snapshot, viewport));
  }

  clearRoute(): void {
    this.assertUsable();
    this.routeController.clear();
    this.renderRoute();
  }

  currentRoute(): Route | null {
    this.assertUsable();
    return this.routeController.current();
  }

  setJourney(journey: JourneyOverlay, viewport: JourneyViewportIntent = { kind: "fit" }): void {
    this.assertUsable();
    const snapshot = this.journeyController.set(journey);
    this.renderJourney();
    this.applyJourneyViewport(resolveJourneyViewport(snapshot, viewport));
  }

  clearJourney(): void {
    this.assertUsable();
    this.journeyController.clear();
    this.renderJourney();
  }

  currentJourney(): JourneyOverlay | null {
    this.assertUsable();
    return this.journeyController.current();
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

  centerCoordinate(): Coordinate {
    this.assertUsable();
    const center = this.map.getCenter();
    return { longitude: center.lng, latitude: center.lat };
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
    this.routeController.clear();
    this.journeyController.clear();
    this.currentLocationController.clear();
    this.removeCurrentLocationLayers();
    this.removeJourneyLayers();
    this.removeRouteLayers();
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
        this.fitBounds(viewport.bounds, viewport.padding, viewport.maxZoom);
        return;
    }
  }

  private applyRouteViewport(viewport: ResolvedRouteViewport): void {
    if (viewport.kind === "preserve") {
      return;
    }
    this.fitBounds(viewport.bounds, viewport.padding, viewport.maxZoom);
  }

  private applyJourneyViewport(viewport: ResolvedJourneyViewport): void {
    if (viewport.kind === "preserve") {
      return;
    }
    this.fitBounds(viewport.bounds, viewport.padding, viewport.maxZoom);
  }

  private fitBounds(
    bounds: Readonly<{ west: number; south: number; east: number; north: number }>,
    padding: number,
    maxZoom: number,
  ): void {
    this.map.fitBounds(
      [
        [bounds.west, bounds.south],
        [bounds.east, bounds.north],
      ],
      { padding, maxZoom, duration: 0 },
    );
  }

  private clearMarkers(): void {
    for (const marker of this.markers) {
      marker.remove();
    }
    this.markers = [];
  }

  private installRouteLayers(): void {
    if (this.map.getSource(ROUTE_SOURCE)) {
      return;
    }

    this.map.addSource(ROUTE_SOURCE, {
      type: "geojson",
      data: EMPTY_FEATURE_COLLECTION,
    });
    this.map.addLayer({
      id: ROUTE_CASING_LAYER,
      type: "line",
      source: ROUTE_SOURCE,
      filter: ["==", ["get", "kind"], "route"],
      layout: {
        "line-cap": "round",
        "line-join": "round",
      },
      paint: {
        "line-color": "#f7f4ed",
        "line-opacity": 0.92,
        "line-width": 8,
      },
    });
    this.map.addLayer({
      id: ROUTE_LINE_LAYER,
      type: "line",
      source: ROUTE_SOURCE,
      filter: ["==", ["get", "kind"], "route"],
      layout: {
        "line-cap": "round",
        "line-join": "round",
      },
      paint: {
        "line-color": "#c4513a",
        "line-opacity": 0.96,
        "line-width": 5,
      },
    });
    this.map.addLayer({
      id: ROUTE_ORIGIN_LAYER,
      type: "circle",
      source: ROUTE_SOURCE,
      filter: ["==", ["get", "kind"], "origin"],
      paint: {
        "circle-color": "#163f49",
        "circle-radius": 7,
        "circle-stroke-color": "#f7f4ed",
        "circle-stroke-width": 2,
      },
    });
    this.map.addLayer({
      id: ROUTE_DESTINATION_LAYER,
      type: "circle",
      source: ROUTE_SOURCE,
      filter: ["==", ["get", "kind"], "destination"],
      paint: {
        "circle-color": "#a63f31",
        "circle-radius": 7,
        "circle-stroke-color": "#f7f4ed",
        "circle-stroke-width": 2,
      },
    });
  }

  private renderRoute(): void {
    if (this.status === "destroyed" || !this.map.getSource(ROUTE_SOURCE)) {
      return;
    }
    const source = this.map.getSource(ROUTE_SOURCE) as GeoJSONSource | undefined;
    if (!source || source.type !== "geojson") {
      return;
    }
    const route = this.routeController.current();
    source.setData(route ? createRouteFeatureCollection(route) : EMPTY_FEATURE_COLLECTION);
  }

  private removeRouteLayers(): void {
    for (const layer of [ROUTE_DESTINATION_LAYER, ROUTE_ORIGIN_LAYER, ROUTE_LINE_LAYER, ROUTE_CASING_LAYER]) {
      if (this.map.getLayer(layer)) {
        this.map.removeLayer(layer);
      }
    }
    if (this.map.getSource(ROUTE_SOURCE)) {
      this.map.removeSource(ROUTE_SOURCE);
    }
  }

  private installJourneyLayers(): void {
    if (this.map.getSource(JOURNEY_SOURCE)) {
      return;
    }

    this.map.addSource(JOURNEY_SOURCE, {
      type: "geojson",
      data: EMPTY_FEATURE_COLLECTION,
    });
    this.map.addLayer({
      id: JOURNEY_CASING_LAYER,
      type: "line",
      source: JOURNEY_SOURCE,
      filter: ["==", ["get", "kind"], "leg"],
      layout: {
        "line-cap": "round",
        "line-join": "round",
      },
      paint: {
        "line-color": "#f7f4ed",
        "line-opacity": 0.94,
        "line-width": 8,
      },
    });
    this.map.addLayer({
      id: JOURNEY_WALK_LAYER,
      type: "line",
      source: JOURNEY_SOURCE,
      filter: ["all", ["==", ["get", "kind"], "leg"], ["==", ["get", "travelKind"], "walk"]],
      layout: {
        "line-cap": "round",
        "line-join": "round",
      },
      paint: {
        "line-color": "#315a60",
        "line-opacity": 0.96,
        "line-width": 4,
        "line-dasharray": [1.2, 1.6],
      },
    });
    this.map.addLayer({
      id: JOURNEY_TRANSIT_LAYER,
      type: "line",
      source: JOURNEY_SOURCE,
      filter: ["all", ["==", ["get", "kind"], "leg"], ["==", ["get", "travelKind"], "transit"]],
      layout: {
        "line-cap": "round",
        "line-join": "round",
      },
      paint: {
        "line-color": "#2f6f8f",
        "line-opacity": 0.98,
        "line-width": 5,
      },
    });
    this.map.addLayer({
      id: JOURNEY_STOP_LAYER,
      type: "circle",
      source: JOURNEY_SOURCE,
      filter: ["==", ["get", "kind"], "transit-stop"],
      paint: {
        "circle-color": "#f7f4ed",
        "circle-radius": 5,
        "circle-stroke-color": "#2f6f8f",
        "circle-stroke-width": 2,
      },
    });
    this.map.addLayer({
      id: JOURNEY_ORIGIN_LAYER,
      type: "circle",
      source: JOURNEY_SOURCE,
      filter: ["==", ["get", "kind"], "origin"],
      paint: {
        "circle-color": "#163f49",
        "circle-radius": 7,
        "circle-stroke-color": "#f7f4ed",
        "circle-stroke-width": 2,
      },
    });
    this.map.addLayer({
      id: JOURNEY_DESTINATION_LAYER,
      type: "circle",
      source: JOURNEY_SOURCE,
      filter: ["==", ["get", "kind"], "destination"],
      paint: {
        "circle-color": "#a63f31",
        "circle-radius": 7,
        "circle-stroke-color": "#f7f4ed",
        "circle-stroke-width": 2,
      },
    });
  }

  private renderJourney(): void {
    if (this.status === "destroyed" || !this.map.getSource(JOURNEY_SOURCE)) {
      return;
    }
    const source = this.map.getSource(JOURNEY_SOURCE) as GeoJSONSource | undefined;
    if (!source || source.type !== "geojson") {
      return;
    }
    const journey = this.journeyController.current();
    source.setData(journey ? createJourneyFeatureCollection(journey) : EMPTY_FEATURE_COLLECTION);
  }

  private removeJourneyLayers(): void {
    for (const layer of [
      JOURNEY_DESTINATION_LAYER,
      JOURNEY_ORIGIN_LAYER,
      JOURNEY_STOP_LAYER,
      JOURNEY_TRANSIT_LAYER,
      JOURNEY_WALK_LAYER,
      JOURNEY_CASING_LAYER,
    ]) {
      if (this.map.getLayer(layer)) {
        this.map.removeLayer(layer);
      }
    }
    if (this.map.getSource(JOURNEY_SOURCE)) {
      this.map.removeSource(JOURNEY_SOURCE);
    }
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

function createRouteFeatureCollection(route: Route) {
  return {
    type: "FeatureCollection" as const,
    features: [
      {
        type: "Feature" as const,
        properties: { kind: "route" },
        geometry: {
          type: "LineString" as const,
          coordinates: route.geometry.map((coordinate) => [coordinate.longitude, coordinate.latitude]),
        },
      },
      {
        type: "Feature" as const,
        properties: { kind: "origin" },
        geometry: {
          type: "Point" as const,
          coordinates: [route.origin.longitude, route.origin.latitude],
        },
      },
      {
        type: "Feature" as const,
        properties: { kind: "destination" },
        geometry: {
          type: "Point" as const,
          coordinates: [route.destination.longitude, route.destination.latitude],
        },
      },
    ],
  };
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