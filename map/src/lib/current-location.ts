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
export const MAX_ACCURACY_DISPLAY_METERS = 5_000_000;

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
  geometry:
    | Readonly<{ type: "Polygon"; coordinates: readonly (readonly (readonly [number, number])[])[] }>
    | Readonly<{ type: "MultiPolygon"; coordinates: readonly (readonly (readonly (readonly [number, number])[])[])[] }>;
}>;

export function createAccuracyGeometry(position: PositionFix): AccuracyGeometry | null {
  const snapshot = snapshotPositionFix(position);
  const { longitude, latitude } = snapshot.coordinate;
  if (snapshot.accuracyMeters > MAX_ACCURACY_DISPLAY_METERS) {
    return null;
  }
  const angularDistance = snapshot.accuracyMeters / EARTH_RADIUS_METERS;
  if (Math.abs((latitude * Math.PI) / 180) + angularDistance >= Math.PI / 2) {
    return null;
  }
  const latitudeRadians = (latitude * Math.PI) / 180;
  const coordinates: [number, number][] = [];

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
    const normalizedLatitude = Math.max(-90, Math.min(90, (destinationLatitude * 180) / Math.PI));
    coordinates.push([unwrapLongitude((destinationLongitude * 180) / Math.PI, longitude), normalizedLatitude]);
  }

  const parts = [0, -360, 360]
    .map((shift) => clipLongitudeRange(coordinates.map(([lon, lat]) => [lon + shift, lat] as [number, number])))
    .filter((ring): ring is [number, number][] => ring !== null);
  const canonicalParts = parts.map((ring) => closeRing(canonicalizeRing(ring)));

  if (snapshot.accuracyMeters === 0) {
    const point: [number, number] = [longitude, latitude];
    canonicalParts.splice(0, canonicalParts.length, closeRing(Array.from({ length: ACCURACY_SEGMENTS + 1 }, () => point)));
  }

  if (canonicalParts.length === 0) {
    return null;
  }

  if (canonicalParts.length === 1) {
    return Object.freeze({
      type: "Feature" as const,
      geometry: Object.freeze({
        type: "Polygon" as const,
        coordinates: Object.freeze([Object.freeze(canonicalParts[0]!)]),
      }),
    });
  }
  return Object.freeze({
    type: "Feature" as const,
    geometry: Object.freeze({
      type: "MultiPolygon" as const,
      coordinates: Object.freeze(canonicalParts.map((ring) => Object.freeze([Object.freeze(ring)]))),
    }),
  });
}

function clipLongitudeRange(points: readonly [number, number][]): [number, number][] | null {
  const lower = clipAt(points, -180, true);
  const clipped = clipAt(lower, 180, false);
  if (clipped.length < 3) return null;
  const unique = new Set(clipped.map(([lon, lat]) => `${lon}:${lat}`));
  return unique.size >= 3 ? clipped : null;
}

function clipAt(points: readonly [number, number][], boundary: number, keepGreater: boolean): [number, number][] {
  if (points.length === 0) return [];
  const output: [number, number][] = [];
  let previous = points[points.length - 1]!;
  let previousInside = keepGreater ? previous[0] >= boundary : previous[0] <= boundary;
  for (const current of points) {
    const currentInside = keepGreater ? current[0] >= boundary : current[0] <= boundary;
    if (currentInside !== previousInside) {
      const denominator = current[0] - previous[0];
      const ratio = denominator === 0 ? 0 : (boundary - previous[0]) / denominator;
      output.push([boundary, previous[1] + (current[1] - previous[1]) * ratio]);
    }
    if (currentInside) output.push(current);
    previous = current;
    previousInside = currentInside;
  }
  return output;
}

function closeRing(points: [number, number][]): [number, number][] {
  const first = points[0]!;
  const last = points[points.length - 1]!;
  if (first[0] !== last[0] || first[1] !== last[1]) points.push([...first]);
  return points;
}

function canonicalizeRing(points: readonly [number, number][]): [number, number][] {
  const canonical: [number, number][] = [];
  for (const [longitude, latitude] of points) {
    const normalized = normalizeLongitude(longitude);
    const previous = canonical[canonical.length - 1]?.[0];
    const adjusted = normalized === -180 && previous !== undefined && previous > 0
      ? 180
      : normalized === 180 && previous !== undefined && previous < 0
        ? -180
        : normalized;
    canonical.push([adjusted, latitude]);
  }
  return canonical;
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

function unwrapLongitude(longitude: number, reference: number): number {
  let result = longitude;
  while (result - reference > 180) result -= 360;
  while (result - reference < -180) result += 360;
  return result;
}
