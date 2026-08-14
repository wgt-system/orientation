import { isCoordinate, type Coordinate } from "./model";

export type PositionFix = Readonly<{
  coordinate: Coordinate;
  accuracyMeters: number;
  observedAt: string;
}>;

const ISO_TIMESTAMP =
  /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/;
const METERS_PER_LATITUDE_DEGREE = 111_320;
const ACCURACY_SEGMENTS = 64;

export function validatePositionFix(position: PositionFix): void {
  if (!position || typeof position !== "object" || !isCoordinate(position.coordinate)) {
    throw new Error("Position fix coordinate is invalid.");
  }
  if (!Number.isFinite(position.accuracyMeters) || position.accuracyMeters < 0) {
    throw new Error("Position fix accuracy must be finite and non-negative.");
  }
  if (
    typeof position.observedAt !== "string" ||
    !ISO_TIMESTAMP.test(position.observedAt) ||
    !Number.isFinite(Date.parse(position.observedAt))
  ) {
    throw new Error("Position fix observed-at must be a valid UTC ISO timestamp.");
  }
}

export function snapshotPositionFix(position: PositionFix): PositionFix {
  validatePositionFix(position);
  return Object.freeze({
    coordinate: Object.freeze({ ...position.coordinate }),
    accuracyMeters: position.accuracyMeters,
    observedAt: position.observedAt,
  });
}

export class CurrentLocationController {
  private position: PositionFix | null = null;

  set(position: PositionFix): PositionFix {
    this.position = snapshotPositionFix(position);
    return this.position;
  }

  clear(): void {
    this.position = null;
  }

  current(): PositionFix | null {
    return this.position;
  }
}

export type AccuracyGeometry = Readonly<{
  type: "Feature";
  geometry: Readonly<{
    type: "Polygon";
    coordinates: readonly (readonly (readonly [number, number])[])[];
  }>;
}>;

export function createAccuracyGeometry(position: PositionFix): AccuracyGeometry {
  const snapshot = snapshotPositionFix(position);
  const { longitude, latitude } = snapshot.coordinate;
  const latitudeRadius = snapshot.accuracyMeters / METERS_PER_LATITUDE_DEGREE;
  const longitudeScale = Math.max(Math.cos((latitude * Math.PI) / 180), 0.01);
  const longitudeRadius = snapshot.accuracyMeters / (METERS_PER_LATITUDE_DEGREE * longitudeScale);
  const coordinates: [number, number][] = [];

  for (let index = 0; index <= ACCURACY_SEGMENTS; index += 1) {
    const angle = (index / ACCURACY_SEGMENTS) * Math.PI * 2;
    coordinates.push([
      normalizeLongitude(longitude + Math.cos(angle) * longitudeRadius),
      latitude + Math.sin(angle) * latitudeRadius,
    ]);
  }

  return Object.freeze({
    type: "Feature",
    geometry: Object.freeze({
      type: "Polygon",
      coordinates: Object.freeze([Object.freeze(coordinates)]),
    }),
  });
}

function normalizeLongitude(longitude: number): number {
  return ((longitude + 180) % 360 + 360) % 360 - 180;
}
