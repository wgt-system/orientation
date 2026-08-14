import { describe, expect, it } from "vitest";
import { validateScene, type SpatialScene } from "./model";

describe("validateScene", () => {
  it("accepts generic spatial features", () => {
    const scene: SpatialScene = {
      features: [
        {
          ref: "example/1",
          coordinate: { longitude: 10.0, latitude: 53.5 },
          title: "Example",
        },
      ],
    };

    expect(() => validateScene(scene)).not.toThrow();
  });

  it("rejects invalid coordinates", () => {
    const scene: SpatialScene = {
      features: [
        {
          ref: "example/1",
          coordinate: { longitude: 181, latitude: 53.5 },
          title: "Example",
        },
      ],
    };

    expect(() => validateScene(scene)).toThrow(/Invalid coordinate/);
  });

  it("rejects duplicate feature refs", () => {
    const scene: SpatialScene = {
      features: [
        {
          ref: "same",
          coordinate: { longitude: 10, latitude: 53 },
          title: "A",
        },
        {
          ref: "same",
          coordinate: { longitude: 11, latitude: 54 },
          title: "B",
        },
      ],
    };

    expect(() => validateScene(scene)).toThrow(/Duplicate/);
  });
});
