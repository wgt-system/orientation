import { isCoordinate, type Coordinate } from "./model";

export type PositionFix = Readonly<{
  coordinate: Coordinate;
  accuracyMeters: number;
  observedAt: string;
}>;

const ISO_TIMESTAMP =
  /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/;
const ACCURACY_SEGMENTS = 64;
const EARTH_RADIUS_METERS = 6_371_008.8;

export function validatePositionFix(position: PositionFix): void {
  if (!position || typeof position !== "object" || !isCoordinate(position.coordinate)) {
    throw new Error("Position fix coordinate is invalid.");
  }
  if (!Number.isFinite(position.accuracyMeters) || position.accuracyMeters < 0) {
    throw new Error("Position fix accuracy must be finite and non-negative.");
  }
  if (
    typeof position.observedAt !== "string" ||
    !isCanonicalTimestamp(position.observedAt)
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
  const coordinates: [number, number][] = [];
  const angularDistance = Math.min(snapshot.accuracyMeters / EARTH_RADIUS_METERS, Math.PI);
  const latitudeRadians = (latitude * Math.PI) / 180;

  for (let index = 0; index <= ACCURACY_SEGMENTS; index += 1) {
    const angle = (index / ACCURACY_SEGMENTS) * Math.PI * 2;
    const destinationLatitude = Math.asin(
      Math.sin(latitudeRadians) * Math.cos(angularDistance) +
        Math.cos(latitudeRadians) * Math.sin(angularDistance) * Math.cos(angle),
    );
    const destinationLongitude =
      (longitude * Math.PI) / 180 +
      Math.atan2(
        Math.sin(angle) * Math.sin(angularDistance) * Math.cos(latitudeRadians),
        Math.cos(angularDistance) - Math.sin(latitudeRadians) * Math.sin(destinationLatitude),
      );
    coordinates.push([
      normalizeLongitude((destinationLongitude * 180) / Math.PI),
      Math.max(-90, Math.min(90, (destinationLatitude * 180) / Math.PI)),
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

function isCanonicalTimestamp(timestamp: string): boolean {
  if (!ISO_TIMESTAMP.test(timestamp)) {
    return false;
  }
  const parsed = Date.parse(timestamp);
  return Number.isFinite(parsed) && new Date(parsed).toISOString() === timestamp;
}

function normalizeLongitude(longitude: number): number {
  return ((longitude + 180) % 360 + 360) % 360 - 180;
}
