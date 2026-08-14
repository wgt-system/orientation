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
    expect(controller.find("feature/1", "provider/1")?.title).toBe("Feature");
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
    expect(controller.find("feature/1", "provider/1")).toBeUndefined();
  });

  it("does not retain mutable caller-owned scene state", () => {
    const controller = new SpatialSceneController();
    const scene = { features: [feature] };

    controller.replace(scene);
    expect(controller.current()).not.toBe(scene);
    expect(controller.current().features).not.toBe(scene.features);
  });

  it("deeply snapshots information, resources and actions", () => {
    const scene = {
      features: [{
        ...feature,
        information: [{ title: "Details", rows: [{ label: "Kind", value: "Example" }] }],
        resources: [{ ref: "resource/1", label: "Resource", uri: "https://example.com" }],
        actions: [{ ref: "action/1", label: "Action" }],
      }],
    };
    const snapshot = new SpatialSceneController().replace(scene);

    expect(snapshot.features[0]!.information).not.toBe(scene.features[0]!.information);
    expect(snapshot.features[0]!.information![0]!.rows).not.toBe(scene.features[0]!.information![0]!.rows);
    expect(Object.isFrozen(snapshot.features[0]!.information)).toBe(true);
    expect(Object.isFrozen(snapshot.features[0]!.information![0]!.rows)).toBe(true);
    expect(Object.isFrozen(snapshot.features[0]!.resources)).toBe(true);
    expect(Object.isFrozen(snapshot.features[0]!.actions)).toBe(true);
  });

  it("returns host-mediated resource and action activation identities", () => {
    const controller = new SpatialSceneController();
    controller.replace({
      features: [{
        ...feature,
        resources: [{ ref: "resource/1", label: "Resource" }],
        actions: [{ ref: "action/1", label: "Action" }],
      }],
    });

    expect(controller.activateResource("feature/1", "resource/1")).toEqual({
      featureRef: "feature/1",
      sourceRef: "provider/1",
      resourceRef: "resource/1",
    });
    expect(controller.activateAction("feature/1", "action/1")).toEqual({
      featureRef: "feature/1",
      sourceRef: "provider/1",
      actionRef: "action/1",
    });
    controller.clear();
    expect(() => controller.activateResource("feature/1", "resource/1")).toThrow(/Unknown spatial feature/);
  });
});
