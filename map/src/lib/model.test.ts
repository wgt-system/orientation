import { describe, expect, it } from "vitest";
import { resolveViewport, validateScene, type SpatialScene } from "./model";

describe("validateScene", () => {
  it("accepts generic spatial features", () => {
    const scene: SpatialScene = {
      features: [
        {
          ref: "example/1",
          sourceRef: "example/provider",
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
          sourceRef: "example/provider",
          coordinate: { longitude: 181, latitude: 53.5 },
          title: "Example",
        },
      ],
    };

    expect(() => validateScene(scene)).toThrow(/Invalid coordinate/);
  });

  it("rejects empty feature and source refs", () => {
    expect(() =>
      validateScene({
        features: [
          {
            ref: " ",
            sourceRef: "provider",
            coordinate: { longitude: 0, latitude: 0 },
            title: "Example",
          },
        ],
      }),
    ).toThrow(/ref must be non-empty/);

    expect(() =>
      validateScene({
        features: [
          {
            ref: "example/1",
            sourceRef: " ",
            coordinate: { longitude: 0, latitude: 0 },
            title: "Example",
          },
        ],
      }),
    ).toThrow(/source ref must be non-empty/);
  });

  it("rejects duplicate feature refs", () => {
    const scene: SpatialScene = {
      features: [
        {
          ref: "same",
          sourceRef: "example/provider",
          coordinate: { longitude: 10, latitude: 53 },
          title: "A",
        },
        {
          ref: "same",
          sourceRef: "example/provider",
          coordinate: { longitude: 11, latitude: 54 },
          title: "B",
        },
      ],
    };

    expect(() => validateScene(scene)).toThrow(/Duplicate/);
  });
});

describe("resolveViewport", () => {
  it("resolves empty, focus, fit and preserved viewport intents", () => {
    expect(resolveViewport({ features: [] })).toEqual({ kind: "empty" });
    expect(
      resolveViewport({
        features: [
          {
            ref: "one",
            sourceRef: "provider",
            coordinate: { longitude: 10, latitude: 50 },
            title: "One",
          },
        ],
      }),
    ).toEqual({ kind: "focus", coordinate: { longitude: 10, latitude: 50 }, zoom: 12 });
    expect(
      resolveViewport({
        features: [
          {
            ref: "west",
            sourceRef: "provider",
            coordinate: { longitude: 10, latitude: 50 },
            title: "West",
          },
          {
            ref: "east",
            sourceRef: "provider",
            coordinate: { longitude: 20, latitude: 60 },
            title: "East",
          },
        ],
      }),
    ).toEqual({
      kind: "fit",
      bounds: {
        southWest: { longitude: 10, latitude: 50 },
        northEast: { longitude: 20, latitude: 60 },
      },
      padding: 48,
      maxZoom: 14,
    });
    expect(resolveViewport({ features: [], viewport: { kind: "preserve" } })).toEqual({ kind: "preserve" });
  });
});
