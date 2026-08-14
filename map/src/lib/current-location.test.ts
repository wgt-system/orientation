import { describe, expect, it } from "vitest";
import {
  createAccuracyGeometry,
  CurrentLocationController,
  snapshotPositionFix,
  validatePositionFix,
  type PositionFix,
} from "./current-location";

const position: PositionFix = {
  coordinate: { longitude: 10, latitude: 50 },
  accuracyMeters: 100,
  observedAt: "2026-08-14T12:00:00.000Z",
};

describe("PositionFix", () => {
  it("accepts a valid serializable position fix", () => {
    expect(() => validatePositionFix(position)).not.toThrow();
  });

  it("rejects invalid accuracy and timestamps", () => {
    expect(() => validatePositionFix({ ...position, accuracyMeters: -1 })).toThrow(/accuracy/);
    expect(() => validatePositionFix({ ...position, accuracyMeters: Number.NaN })).toThrow(/accuracy/);
    expect(() => validatePositionFix({ ...position, accuracyMeters: Number.POSITIVE_INFINITY })).toThrow(/accuracy/);
    expect(() => validatePositionFix({ ...position, observedAt: "not-a-timestamp" })).toThrow(/observed-at/);
    expect(() => validatePositionFix({ ...position, observedAt: "2026-08-14T12:00:00Z" })).toThrow(/observed-at/);
    expect(() => validatePositionFix({ ...position, observedAt: "2026-02-31T12:00:00.000Z" })).toThrow(/observed-at/);
    expect(() => validatePositionFix({ ...position, observedAt: "2025-02-29T12:00:00.000Z" })).toThrow(/observed-at/);
    expect(() => validatePositionFix({ ...position, observedAt: "2024-02-29T12:00:00.000Z" })).not.toThrow();
    expect(() => validatePositionFix({ ...position, observedAt: "2026-13-01T12:00:00.000Z" })).toThrow(/observed-at/);
  });

  it("deeply snapshots caller-owned position values", () => {
    const callerOwned = {
      coordinate: { ...position.coordinate },
      accuracyMeters: position.accuracyMeters,
      observedAt: position.observedAt,
    };
    const snapshot = snapshotPositionFix(callerOwned);

    expect(snapshot).not.toBe(callerOwned);
    expect(snapshot.coordinate).not.toBe(callerOwned.coordinate);
    expect(Object.isFrozen(snapshot)).toBe(true);
    expect(Object.isFrozen(snapshot.coordinate)).toBe(true);
  });
});

describe("CurrentLocationController", () => {
  it("replaces updates, clears state and retains no history", () => {
    const controller = new CurrentLocationController();
    const update = { ...position, coordinate: { longitude: 11, latitude: 51 } };

    controller.set(position);
    controller.set(update);
    expect(controller.current()).toEqual(update);
    controller.clear();
    expect(controller.current()).toBeNull();
  });
});

describe("createAccuracyGeometry", () => {
  it("creates a geographic accuracy polygon in metres", () => {
    const geometry = createAccuracyGeometry(position);
    expect(geometry).not.toBeNull();
    if (!geometry) return;
    const ring = geometry.geometry.coordinates[0]!;

    expect(geometry.geometry.type).toBe("Polygon");
    expect(ring).toHaveLength(65);
    expect(ring[0]![0]).toBeCloseTo(position.coordinate.longitude, 8);
    expect(ring[0]![1]).toBeGreaterThan(position.coordinate.latitude);
    expect(ring[16]![0]).toBeGreaterThan(position.coordinate.longitude);
  });

  it("keeps polar, antimeridian and large-accuracy coordinates valid", () => {
    for (const sample of [
      { ...position, coordinate: { longitude: 179.9, latitude: 80 }, accuracyMeters: 50_000 },
      { ...position, coordinate: { longitude: 0, latitude: 0 }, accuracyMeters: 0 },
      { ...position, coordinate: { longitude: 0, latitude: 0 }, accuracyMeters: 0 },
    ]) {
      const geometry = createAccuracyGeometry(sample);
      expect(geometry).not.toBeNull();
      if (!geometry) continue;
      const rings = geometry.geometry.type === "Polygon"
        ? geometry.geometry.coordinates
        : geometry.geometry.coordinates.flat();
      for (const ring of rings) {
        expect(ring.every(([longitude, latitude]) => longitude >= -180 && longitude <= 180 && latitude >= -90 && latitude <= 90)).toBe(true);
        expect(Math.max(...ring.slice(1).map(([longitude], index) => Math.abs(longitude - ring[index]![0])))).toBeLessThan(181);
      }
    }
  });

  it("splits antimeridian accuracy into canonical parts without world-spanning edges", () => {
    const geometry = createAccuracyGeometry({ ...position, coordinate: { longitude: 179.9, latitude: 0 }, accuracyMeters: 50_000 });
    expect(geometry?.geometry.type).toBe("MultiPolygon");
    if (!geometry || geometry.geometry.type !== "MultiPolygon") return;
    expect(geometry.geometry.coordinates.length).toBe(2);
  });

  it("omits pathological accuracy areas while retaining the position fix", () => {
    expect(createAccuracyGeometry({ ...position, accuracyMeters: 5_000_001 })).toBeNull();
    expect(createAccuracyGeometry({ ...position, coordinate: { longitude: -179.9, latitude: 89.9 }, accuracyMeters: 500_000 })).toBeNull();
  });
});
