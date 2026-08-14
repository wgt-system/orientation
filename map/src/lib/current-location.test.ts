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
    const ring = geometry.geometry.coordinates[0]!;

    expect(geometry.geometry.type).toBe("Polygon");
    expect(ring).toHaveLength(65);
    expect(ring[0]![1]).toBe(position.coordinate.latitude);
    expect(ring[0]![0]).toBeGreaterThan(position.coordinate.longitude);
    expect(ring[16]![1]).toBeGreaterThan(position.coordinate.latitude);
    expect(ring[0]![0] - position.coordinate.longitude).toBeCloseTo(100 / (111_320 * Math.cos((50 * Math.PI) / 180)), 8);
  });
});
