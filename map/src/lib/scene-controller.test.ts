import { describe, expect, it } from "vitest";
import { SpatialSceneController } from "./scene-controller";

const feature = {
  ref: "feature/1",
  sourceRef: "provider/1",
  coordinate: { longitude: 10, latitude: 50 },
  title: "Feature",
} as const;

describe("SpatialSceneController", () => {
  it("replaces scenes deterministically and clears old features", () => {
    const controller = new SpatialSceneController();

    controller.replace({ features: [feature] });
    expect(controller.current().features.map((item) => item.ref)).toEqual(["feature/1"]);

    controller.replace({
      features: [
        { ...feature, ref: "feature/2", title: "Replacement" },
        { ...feature, ref: "feature/3", title: "Second replacement" },
      ],
    });
    expect(controller.current().features.map((item) => item.ref)).toEqual([
      "feature/2",
      "feature/3",
    ]);

    controller.clear();
    expect(controller.current().features).toHaveLength(0);
  });

  it("returns opaque selection identity and rejects stale selections", () => {
    const controller = new SpatialSceneController();
    controller.replace({ features: [feature] });

    expect(controller.select("feature/1")).toEqual({
      featureRef: "feature/1",
      sourceRef: "provider/1",
    });

    controller.replace({ features: [] });
    expect(() => controller.select("feature/1")).toThrow(/Unknown spatial feature ref/);
  });

  it("does not retain mutable caller-owned scene state", () => {
    const controller = new SpatialSceneController();
    const scene = { features: [feature] };

    controller.replace(scene);
    expect(controller.current()).not.toBe(scene);
    expect(controller.current().features).not.toBe(scene.features);
  });
});
