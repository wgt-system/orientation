import {
  isCoordinate,
  resolveCoordinateBounds,
  type Coordinate,
  type ResolvedViewportBounds,
} from "./model";

export const MAX_ROUTE_COORDINATES = 10_000;

export type TravelProfile = "DRIVING" | "CYCLING" | "WALKING";

export type Route = Readonly<{
  origin: Coordinate;
  destination: Coordinate;
  profile: TravelProfile;
  geometry: readonly Coordinate[];
  distanceMeters: number;
  durationSeconds: number;
}>;

export type RouteViewportIntent = Readonly<{
  kind: "fit" | "preserve";
  padding?: number;
  maxZoom?: number;
}>;

export type ResolvedRouteViewport =
  | Readonly<{ kind: "preserve" }>
  | Readonly<{
      kind: "fit";
      bounds: ResolvedViewportBounds;
      padding: number;
      maxZoom: number;
    }>;

export function validateRoute(route: Route): void {
  if (!route || !isCoordinate(route.origin) || !isCoordinate(route.destination)) {
    throw new Error("Route origin and destination must be valid coordinates.");
  }
  if (!["DRIVING", "CYCLING", "WALKING"].includes(route.profile)) {
    throw new Error("Route travel profile is invalid.");
  }
  if (!Array.isArray(route.geometry) || route.geometry.length < 2) {
    throw new Error("Route geometry requires at least two coordinates.");
  }
  if (route.geometry.length > MAX_ROUTE_COORDINATES) {
    throw new Error("Route geometry exceeds the coordinate limit.");
  }
  if (!route.geometry.every(isCoordinate)) {
    throw new Error("Route geometry contains an invalid coordinate.");
  }
  if (!Number.isFinite(route.distanceMeters) || route.distanceMeters < 0) {
    throw new Error("Route distance must be finite and non-negative.");
  }
  if (!Number.isFinite(route.durationSeconds) || route.durationSeconds < 0) {
    throw new Error("Route duration must be finite and non-negative.");
  }
}

export function snapshotRoute(route: Route): Route {
  validateRoute(route);
  return Object.freeze({
    ...route,
    origin: Object.freeze({ ...route.origin }),
    destination: Object.freeze({ ...route.destination }),
    geometry: Object.freeze(route.geometry.map((coordinate) => Object.freeze({ ...coordinate }))),
  });
}

export function resolveRouteViewport(
  route: Route,
  intent: RouteViewportIntent = { kind: "fit" },
): ResolvedRouteViewport {
  validateRoute(route);
  if (intent.kind === "preserve") {
    return { kind: "preserve" };
  }
  if (intent.kind !== "fit") {
    throw new Error("Unsupported route viewport intent.");
  }
  if (intent.padding !== undefined && (!Number.isFinite(intent.padding) || intent.padding < 0)) {
    throw new Error("Route viewport padding must be non-negative.");
  }
  if (intent.maxZoom !== undefined && (!Number.isFinite(intent.maxZoom) || intent.maxZoom <= 0)) {
    throw new Error("Route viewport max zoom must be positive.");
  }
  return {
    kind: "fit",
    bounds: resolveCoordinateBounds(route.geometry),
    padding: intent.padding ?? 64,
    maxZoom: intent.maxZoom ?? 14,
  };
}

export class RouteOverlayController {
  private route: Route | null = null;

  set(route: Route): Route {
    this.route = snapshotRoute(route);
    return this.route;
  }

  clear(): void {
    this.route = null;
  }

  current(): Route | null {
    return this.route;
  }
}
