import { describe, expect, it } from "vitest";
import {
  JourneyOverlayController,
  MAX_JOURNEY_LEGS,
  MAX_JOURNEY_LEG_COORDINATES,
  createJourneyFeatureCollection,
  resolveJourneyViewport,
  snapshotJourneyOverlay,
  validateJourneyOverlay,
  type JourneyOverlay,
} from "./journey-overlay";

const journey: JourneyOverlay = {
  legs: [
    {
      mode: "WALK",
      origin: { name: "Origin", coordinate: { longitude: 9.99, latitude: 53.55 } },
      destination: { name: "Station A", coordinate: { longitude: 10.0, latitude: 53.56 } },
      geometry: [
        { longitude: 9.99, latitude: 53.55 },
        { longitude: 10.0, latitude: 53.56 },
      ],
    },
    {
      mode: "SUBWAY",
      origin: { name: "Station A", coordinate: { longitude: 10.0, latitude: 53.56 } },
      destination: { name: "Station B", coordinate: { longitude: 10.02, latitude: 53.58 } },
      geometry: [
        { longitude: 10.0, latitude: 53.56 },
        { longitude: 10.01, latitude: 53.57 },
        { longitude: 10.02, latitude: 53.58 },
      ],
    },
  ],
};

describe("Journey overlay model", () => {
  it("snapshots provider-neutral legs, stops and geometry", () => {
    expect(() => validateJourneyOverlay(journey)).not.toThrow();

    const mutable = {
      legs: journey.legs.map((leg) => ({
        ...leg,
        origin: { ...leg.origin, coordinate: { ...leg.origin.coordinate } },
        destination: { ...leg.destination, coordinate: { ...leg.destination.coordinate } },
        geometry: leg.geometry?.map((coordinate) => ({ ...coordinate })),
      })),
    };
    const snapshot = snapshotJourneyOverlay(mutable);
    mutable.legs[0]!.origin.coordinate.longitude = 42;
    mutable.legs[1]!.geometry![0]!.longitude = 42;

    expect(snapshot.legs[0]!.origin.coordinate.longitude).toBe(9.99);
    expect(snapshot.legs[1]!.geometry![0]!.longitude).toBe(10.0);
    expect(Object.isFrozen(snapshot)).toBe(true);
    expect(Object.isFrozen(snapshot.legs)).toBe(true);
  });

  it("rejects invalid modes, all-walking Journeys and excessive leg/geometry sizes", () => {
    expect(() =>
      validateJourneyOverlay({
        legs: [{ ...journey.legs[1]!, mode: "RIDE_SHARING" as typeof journey.legs[number]["mode"] }],
      }),
    ).toThrow(/mode/);
    expect(() => validateJourneyOverlay({ legs: [journey.legs[0]!] })).toThrow(/transit leg/);
    expect(() =>
      validateJourneyOverlay({
        legs: Array.from({ length: MAX_JOURNEY_LEGS + 1 }, () => journey.legs[1]!),
      }),
    ).toThrow(/leg limit/);
    expect(() =>
      validateJourneyOverlay({
        legs: [
          {
            ...journey.legs[1]!,
            geometry: Array.from({ length: MAX_JOURNEY_LEG_COORDINATES + 1 }, () => ({
              longitude: 10,
              latitude: 53,
            })),
          },
        ],
      }),
    ).toThrow(/coordinate limit/);
  });

  it("replaces and clears the selected Journey independently", () => {
    const controller = new JourneyOverlayController();
    expect(controller.current()).toBeNull();

    const first = controller.set(journey);
    expect(controller.current()).toBe(first);

    const replacement = controller.set({ legs: [journey.legs[1]!] });
    expect(controller.current()?.legs).toHaveLength(1);
    expect(controller.current()?.legs[0]?.mode).toBe("SUBWAY");

    controller.clear();
    expect(controller.current()).toBeNull();
  });

  it("creates separate dashed-walk/solid-transit semantics and deduplicated transit stops", () => {
    const collection = createJourneyFeatureCollection(journey);
    const legFeatures = collection.features.filter((feature) => feature.properties.kind === "leg");
    const stopFeatures = collection.features.filter((feature) => feature.properties.kind === "transit-stop");

    expect(legFeatures).toHaveLength(2);
    expect(legFeatures.map((feature) => feature.properties.travelKind)).toEqual(["walk", "transit"]);
    expect(stopFeatures).toHaveLength(2);
    expect(collection.features.some((feature) => feature.properties.kind === "origin")).toBe(true);
    expect(collection.features.some((feature) => feature.properties.kind === "destination")).toBe(true);
  });
});

describe("Journey viewport", () => {
  it("fits all leg geometry and stop endpoints with Journey defaults", () => {
    expect(resolveJourneyViewport(journey)).toEqual({
      kind: "fit",
      bounds: { west: 9.99, south: 53.55, east: 10.02, north: 53.58 },
      padding: 64,
      maxZoom: 14,
    });
  });

  it("supports preserve and antimeridian-safe fitting", () => {
    expect(resolveJourneyViewport(journey, { kind: "preserve" })).toEqual({ kind: "preserve" });

    const crossing: JourneyOverlay = {
      legs: [
        {
          mode: "FERRY",
          origin: { name: "West", coordinate: { longitude: 179, latitude: 10 } },
          destination: { name: "East", coordinate: { longitude: -179, latitude: 11 } },
          geometry: [
            { longitude: 179, latitude: 10 },
            { longitude: -179, latitude: 11 },
          ],
        },
      ],
    };

    expect(resolveJourneyViewport(crossing)).toEqual({
      kind: "fit",
      bounds: { west: 179, south: 10, east: 181, north: 11 },
      padding: 64,
      maxZoom: 14,
    });
  });
});
