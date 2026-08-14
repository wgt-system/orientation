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

  it("accepts rich generic information, resources and actions", () => {
    expect(() =>
      validateScene({
        features: [
          {
            ref: "feature/rich",
            sourceRef: "provider/example",
            coordinate: { longitude: 10, latitude: 50 },
            title: "Rich feature",
            information: [{ title: "Details", rows: [{ label: "Kind", value: "Example" }] }],
            resources: [{ ref: "resource/1", label: "Read more", uri: "https://example.com/item" }],
            actions: [{ ref: "action/1", label: "Inspect" }],
          },
        ],
      }),
    ).not.toThrow();
  });

  it("rejects malformed rich content, duplicate refs and unsafe URIs", () => {
    const base = {
      ref: "feature/rich",
      sourceRef: "provider/example",
      coordinate: { longitude: 10, latitude: 50 },
      title: "Rich feature",
    } as const;

    expect(() =>
      validateScene({
        features: [{ ...base, resources: [
          { ref: "same", label: "One" },
          { ref: "same", label: "Two" },
        ] }],
      }),
    ).toThrow(/Duplicate spatial resource/);
    expect(() =>
      validateScene({
        features: [{ ...base, actions: [
          { ref: "same", label: "One" },
          { ref: "same", label: "Two" },
        ] }],
      }),
    ).toThrow(/Duplicate spatial action/);
    expect(() =>
      validateScene({ features: [{ ...base, resources: [{ ref: "unsafe", label: "Unsafe", uri: "javascript:alert(1)" }] }] }),
    ).toThrow(/scheme is not allowed/);
    expect(() =>
      validateScene({ features: [{ ...base, resources: [{ ref: "invalid", label: "Invalid", uri: "not a URI" }] }] }),
    ).toThrow(/valid HTTP/);
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
