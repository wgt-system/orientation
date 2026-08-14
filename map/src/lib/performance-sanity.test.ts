import { describe, expect, it } from "vitest";
import { snapshotScene } from "./scene-controller";
import { validateScene, type SpatialScene } from "./model";

function createLargeScene(): SpatialScene {
  return {
    features: Array.from({ length: 500 }, (_, index) => ({
      ref: `fixture/${index}`,
      sourceRef: "fixture/provider",
      coordinate: { longitude: 5 + (index % 25) * 0.5, latitude: 48 + Math.floor(index / 25) * 0.25 },
      title: `Fixture ${index}`,
    })),
  };
}

describe("v0.1.0 renderer sanity fixture", () => {
  it("validates and snapshots 500 provider features without retained history", () => {
    const scene = createLargeScene();
    validateScene(scene);
    const first = snapshotScene(scene);
    const second = snapshotScene(scene);

    expect(scene.features).toHaveLength(500);
    expect(first.features).toHaveLength(500);
    expect(second.features).toHaveLength(500);
    expect(first).not.toBe(second);
  });
});
