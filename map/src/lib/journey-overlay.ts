import {
  isCoordinate,
  resolveCoordinateBounds,
  type Coordinate,
  type ResolvedViewportBounds,
} from "./model";

export const MAX_JOURNEY_LEGS = 64;
export const MAX_JOURNEY_LEG_COORDINATES = 10_000;

export type JourneyLegMode =
  | "WALK"
  | "RAIL"
  | "SUBURBAN_RAIL"
  | "SUBWAY"
  | "TRAM"
  | "BUS"
  | "COACH"
  | "FERRY"
  | "OTHER_TRANSIT";

export type JourneyOverlayStop = Readonly<{
  name: string;
  coordinate: Coordinate;
}>;

export type JourneyOverlayLeg = Readonly<{
  mode: JourneyLegMode;
  origin: JourneyOverlayStop;
  destination: JourneyOverlayStop;
  geometry?: readonly Coordinate[];
}>;

export type JourneyOverlay = Readonly<{
  legs: readonly JourneyOverlayLeg[];
}>;

export type JourneyViewportIntent = Readonly<{
  kind: "fit" | "preserve";
  padding?: number;
  maxZoom?: number;
}>;

export type ResolvedJourneyViewport =
  | Readonly<{ kind: "preserve" }>
  | Readonly<{
      kind: "fit";
      bounds: ResolvedViewportBounds;
      padding: number;
      maxZoom: number;
    }>;

const JOURNEY_LEG_MODES: readonly JourneyLegMode[] = [
  "WALK",
  "RAIL",
  "SUBURBAN_RAIL",
  "SUBWAY",
  "TRAM",
  "BUS",
  "COACH",
  "FERRY",
  "OTHER_TRANSIT",
];

export function validateJourneyOverlay(journey: JourneyOverlay): void {
  if (!journey || !Array.isArray(journey.legs) || journey.legs.length === 0) {
    throw new Error("Journey overlay requires at least one leg.");
  }
  if (journey.legs.length > MAX_JOURNEY_LEGS) {
    throw new Error("Journey overlay exceeds the leg limit.");
  }

  let hasTransitLeg = false;
  for (const leg of journey.legs) {
    if (!leg || !JOURNEY_LEG_MODES.includes(leg.mode)) {
      throw new Error("Journey overlay contains an invalid leg mode.");
    }
    hasTransitLeg ||= leg.mode !== "WALK";
    validateStop(leg.origin);
    validateStop(leg.destination);

    if (leg.geometry !== undefined) {
      if (!Array.isArray(leg.geometry) || leg.geometry.length < 2) {
        throw new Error("Journey leg geometry requires at least two coordinates when present.");
      }
      if (leg.geometry.length > MAX_JOURNEY_LEG_COORDINATES) {
        throw new Error("Journey leg geometry exceeds the coordinate limit.");
      }
      if (!leg.geometry.every(isCoordinate)) {
        throw new Error("Journey leg geometry contains an invalid coordinate.");
      }
    }
  }

  if (!hasTransitLeg) {
    throw new Error("Public-transit Journey overlay requires at least one transit leg.");
  }
}

export function snapshotJourneyOverlay(journey: JourneyOverlay): JourneyOverlay {
  validateJourneyOverlay(journey);
  return Object.freeze({
    legs: Object.freeze(
      journey.legs.map((leg) =>
        Object.freeze({
          ...leg,
          origin: snapshotStop(leg.origin),
          destination: snapshotStop(leg.destination),
          ...(leg.geometry === undefined
            ? {}
            : {
                geometry: Object.freeze(
                  leg.geometry.map((coordinate) => Object.freeze({ ...coordinate })),
                ),
              }),
        }),
      ),
    ),
  });
}

export function resolveJourneyViewport(
  journey: JourneyOverlay,
  intent: JourneyViewportIntent = { kind: "fit" },
): ResolvedJourneyViewport {
  validateJourneyOverlay(journey);
  if (intent.kind === "preserve") {
    return { kind: "preserve" };
  }
  if (intent.kind !== "fit") {
    throw new Error("Unsupported Journey viewport intent.");
  }
  if (intent.padding !== undefined && (!Number.isFinite(intent.padding) || intent.padding < 0)) {
    throw new Error("Journey viewport padding must be non-negative.");
  }
  if (intent.maxZoom !== undefined && (!Number.isFinite(intent.maxZoom) || intent.maxZoom <= 0)) {
    throw new Error("Journey viewport max zoom must be positive.");
  }

  const coordinates = journey.legs.flatMap((leg) => [
    leg.origin.coordinate,
    ...(leg.geometry ?? []),
    leg.destination.coordinate,
  ]);
  return {
    kind: "fit",
    bounds: resolveCoordinateBounds(coordinates),
    padding: intent.padding ?? 64,
    maxZoom: intent.maxZoom ?? 14,
  };
}

export class JourneyOverlayController {
  private journey: JourneyOverlay | null = null;

  set(journey: JourneyOverlay): JourneyOverlay {
    this.journey = snapshotJourneyOverlay(journey);
    return this.journey;
  }

  clear(): void {
    this.journey = null;
  }

  current(): JourneyOverlay | null {
    return this.journey;
  }
}

export function createJourneyFeatureCollection(journey: JourneyOverlay) {
  validateJourneyOverlay(journey);
  const lineFeatures = journey.legs.flatMap((leg, legIndex) => {
    if (leg.geometry === undefined) {
      return [];
    }
    return [
      {
        type: "Feature" as const,
        properties: {
          kind: "leg",
          legIndex,
          mode: leg.mode,
          travelKind: leg.mode === "WALK" ? "walk" : "transit",
        },
        geometry: {
          type: "LineString" as const,
          coordinates: leg.geometry.map((coordinate) => [coordinate.longitude, coordinate.latitude]),
        },
      },
    ];
  });

  const firstLeg = journey.legs[0]!;
  const lastLeg = journey.legs[journey.legs.length - 1]!;
  const transitStops = new Map<string, JourneyOverlayStop>();
  for (const leg of journey.legs) {
    if (leg.mode === "WALK") {
      continue;
    }
    transitStops.set(stopKey(leg.origin), leg.origin);
    transitStops.set(stopKey(leg.destination), leg.destination);
  }

  return {
    type: "FeatureCollection" as const,
    features: [
      ...lineFeatures,
      ...Array.from(transitStops.values()).map((stop) => ({
        type: "Feature" as const,
        properties: { kind: "transit-stop", label: stop.name },
        geometry: {
          type: "Point" as const,
          coordinates: [stop.coordinate.longitude, stop.coordinate.latitude],
        },
      })),
      {
        type: "Feature" as const,
        properties: { kind: "origin" },
        geometry: {
          type: "Point" as const,
          coordinates: [firstLeg.origin.coordinate.longitude, firstLeg.origin.coordinate.latitude],
        },
      },
      {
        type: "Feature" as const,
        properties: { kind: "destination" },
        geometry: {
          type: "Point" as const,
          coordinates: [lastLeg.destination.coordinate.longitude, lastLeg.destination.coordinate.latitude],
        },
      },
    ],
  };
}

function validateStop(stop: JourneyOverlayStop): void {
  if (!stop || typeof stop.name !== "string" || stop.name.trim().length === 0) {
    throw new Error("Journey overlay stop name is required.");
  }
  if (!isCoordinate(stop.coordinate)) {
    throw new Error("Journey overlay stop coordinate is invalid.");
  }
}

function snapshotStop(stop: JourneyOverlayStop): JourneyOverlayStop {
  return Object.freeze({
    name: stop.name.trim(),
    coordinate: Object.freeze({ ...stop.coordinate }),
  });
}

function stopKey(stop: JourneyOverlayStop): string {
  return `${stop.name}\u0000${stop.coordinate.longitude}\u0000${stop.coordinate.latitude}`;
}
