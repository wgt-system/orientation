import { describe, expect, it } from "vitest";
import {
  MAX_ROUTE_COORDINATES,
  RouteOverlayController,
  resolveRouteViewport,
  snapshotRoute,
  validateRoute,
  type Route,
} from "./route-overlay";

const route: Route = {
  origin: { longitude: 9.99, latitude: 53.55 },
  destination: { longitude: 10.01, latitude: 53.57 },
  profile: "WALKING",
  geometry: [
    { longitude: 9.99, latitude: 53.55 },
    { longitude: 10.0, latitude: 53.56 },
    { longitude: 10.01, latitude: 53.57 },
  ],
  distanceMeters: 1800,
  durationSeconds: 1200,
};

describe("route overlay model", () => {
  it("accepts provider-neutral decoded routes and snapshots nested coordinates", () => {
    expect(() => validateRoute(route)).not.toThrow();

    const mutable = {
      ...route,
      origin: { ...route.origin },
      geometry: route.geometry.map((coordinate) => ({ ...coordinate })),
    };
    const snapshot = snapshotRoute(mutable);
    mutable.origin.longitude = 42;
    mutable.geometry[0]!.longitude = 42;

    expect(snapshot.origin.longitude).toBe(9.99);
    expect(snapshot.geometry[0]!.longitude).toBe(9.99);
    expect(Object.isFrozen(snapshot)).toBe(true);
    expect(Object.isFrozen(snapshot.geometry)).toBe(true);
  });

  it("rejects invalid profile, metrics, coordinates and geometry size", () => {
    expect(() => validateRoute({ ...route, profile: "TRANSIT" as Route["profile"] })).toThrow(/profile/);
    expect(() => validateRoute({ ...route, distanceMeters: -1 })).toThrow(/distance/);
    expect(() => validateRoute({ ...route, durationSeconds: Number.NaN })).toThrow(/duration/);
    expect(() => validateRoute({ ...route, geometry: [{ longitude: 9.99, latitude: 53.55 }] })).toThrow(
      /at least two/,
    );
    expect(() =>
      validateRoute({
        ...route,
        geometry: Array.from({ length: MAX_ROUTE_COORDINATES + 1 }, () => ({ longitude: 10, latitude: 53 })),
      }),
    ).toThrow(/coordinate limit/);
    expect(() =>
      validateRoute({
        ...route,
        geometry: [route.origin, { longitude: 181, latitude: 53.57 }],
      }),
    ).toThrow(/invalid coordinate/);
  });

  it("replaces and clears current route deterministically", () => {
    const controller = new RouteOverlayController();
    expect(controller.current()).toBeNull();

    const first = controller.set(route);
    expect(controller.current()).toBe(first);

    const replacement = controller.set({ ...route, profile: "CYCLING", distanceMeters: 1500 });
    expect(controller.current()?.profile).toBe("CYCLING");
    expect(controller.current()?.distanceMeters).toBe(1500);

    controller.clear();
    expect(controller.current()).toBeNull();
  });
});

describe("route viewport", () => {
  it("fits the decoded route with route-specific defaults", () => {
    expect(resolveRouteViewport(route)).toEqual({
      kind: "fit",
      bounds: { west: 9.99, south: 53.55, east: 10.01, north: 53.57 },
      padding: 64,
      maxZoom: 14,
    });
  });

  it("can preserve the viewport and validates fit options", () => {
    expect(resolveRouteViewport(route, { kind: "preserve" })).toEqual({ kind: "preserve" });
    expect(() => resolveRouteViewport(route, { kind: "fit", padding: -1 })).toThrow(/padding/);
    expect(() => resolveRouteViewport(route, { kind: "fit", maxZoom: 0 })).toThrow(/max zoom/);
  });

  it("uses the minimal longitude span across the antimeridian", () => {
    const crossing: Route = {
      ...route,
      origin: { longitude: 179, latitude: 10 },
      destination: { longitude: -179, latitude: 11 },
      geometry: [
        { longitude: 179, latitude: 10 },
        { longitude: -179, latitude: 11 },
      ],
    };

    expect(resolveRouteViewport(crossing)).toEqual({
      kind: "fit",
      bounds: { west: 179, south: 10, east: 181, north: 11 },
      padding: 64,
      maxZoom: 14,
    });
  });
});
