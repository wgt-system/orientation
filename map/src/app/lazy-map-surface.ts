import type { PositionFix } from "../lib/current-location";
import type { JourneyOverlay, JourneyViewportIntent } from "../lib/journey-overlay";
import type { SpatialScene } from "../lib/model";
import type {
  OrientationMapCallbacks,
  OrientationMapSurface,
  OrientationMapStatus,
} from "../lib/map-surface";
import type { Route, RouteViewportIntent } from "../lib/route-overlay";

type PendingRoute = Readonly<{ route: Route; viewport: RouteViewportIntent }>;
type PendingJourney = Readonly<{ journey: JourneyOverlay; viewport: JourneyViewportIntent }>;

/**
 * Standalone-app loading boundary around the reusable Map Surface.
 *
 * The navigation/research shell is allowed to initialize and become interactive
 * before MapLibre, its worker and the hosted basemap are requested. The latest
 * presentation state is retained while the real surface loads and replayed once
 * the module is available.
 */
export class LazyOrientationMapSurface {
  private surface: OrientationMapSurface | undefined;
  private loading: Promise<void> | undefined;
  private destroyed = false;
  private scene: SpatialScene = { features: [] };
  private route: PendingRoute | undefined;
  private journey: PendingJourney | undefined;
  private position: PositionFix | undefined;

  constructor(
    private readonly container: HTMLElement,
    private readonly callbacks: OrientationMapCallbacks = {},
  ) {
    this.emitStatus("initializing");
    this.loadAfterFirstPaint();
  }

  setScene(scene: SpatialScene): void {
    this.scene = scene;
    this.surface?.setScene(scene);
  }

  setRoute(route: Route, viewport: RouteViewportIntent = { kind: "fit" }): void {
    this.route = { route, viewport };
    this.surface?.setRoute(route, viewport);
  }

  clearRoute(): void {
    this.route = undefined;
    this.surface?.clearRoute();
  }

  setJourney(journey: JourneyOverlay, viewport: JourneyViewportIntent = { kind: "fit" }): void {
    this.journey = { journey, viewport };
    this.surface?.setJourney(journey, viewport);
  }

  clearJourney(): void {
    this.journey = undefined;
    this.surface?.clearJourney();
  }

  setCurrentPosition(position: PositionFix): void {
    this.position = position;
    this.surface?.setCurrentPosition(position);
  }

  clearCurrentPosition(): void {
    this.position = undefined;
    this.surface?.clearCurrentPosition();
  }

  destroy(): void {
    if (this.destroyed) return;
    this.destroyed = true;
    this.surface?.destroy();
    this.surface = undefined;
  }

  private loadAfterFirstPaint(): void {
    window.requestAnimationFrame(() => {
      window.setTimeout(() => void this.ensureSurface(), 0);
    });
  }

  private ensureSurface(): Promise<void> {
    if (this.destroyed || this.surface) return Promise.resolve();
    if (this.loading) return this.loading;

    this.loading = Promise.all([
      import("../lib/map-surface"),
      import("maplibre-gl/dist/maplibre-gl.css"),
    ])
      .then(([module]) => {
        if (this.destroyed) return;
        const surface = new module.OrientationMapSurface(this.container, this.callbacks);
        this.surface = surface;
        surface.setScene(this.scene);
        if (this.position) surface.setCurrentPosition(this.position);
        if (this.route) surface.setRoute(this.route.route, this.route.viewport);
        if (this.journey) surface.setJourney(this.journey.journey, this.journey.viewport);
      })
      .catch(() => {
        if (!this.destroyed) this.emitStatus("error");
      })
      .finally(() => {
        this.loading = undefined;
      });

    return this.loading;
  }

  private emitStatus(status: OrientationMapStatus): void {
    this.callbacks.onStatusChanged?.({ status });
  }
}
