import { isCoordinate, type Coordinate } from "../lib/model";
import {
  validateJourneyOverlay,
  type JourneyLegMode,
  type JourneyOverlay,
} from "../lib/journey-overlay";

export type JourneyTimeMode = "DEPART_AT" | "ARRIVE_BY";

export type JourneyApiFailureKind =
  | "invalid-request"
  | "no-journey"
  | "rate-limited"
  | "invalid-response"
  | "unavailable";

export class JourneyApiError extends Error {
  constructor(readonly kind: JourneyApiFailureKind, message: string) {
    super(message);
    this.name = "JourneyApiError";
  }
}

export type JourneyRequest = Readonly<{
  origin: Coordinate;
  destination: Coordinate;
  timeMode: JourneyTimeMode;
  time: string;
}>;

export type JourneyEventTime = Readonly<{
  scheduledTime: string;
  realtimeTime: string | null;
}>;

export type JourneyStop = Readonly<{
  name: string;
  coordinate: Coordinate;
}>;

export type TransitService = Readonly<{
  label: string;
  headsign: string | null;
}>;

export type JourneyLeg = Readonly<{
  mode: JourneyLegMode;
  origin: JourneyStop;
  destination: JourneyStop;
  departure: JourneyEventTime;
  arrival: JourneyEventTime;
  durationSeconds: number;
  transitService: TransitService | null;
  geometry: readonly Coordinate[];
  intermediateStops: readonly JourneyStop[];
}>;

export type Journey = Readonly<{
  departureTime: string;
  arrivalTime: string;
  durationSeconds: number;
  transfers: number;
  legs: readonly JourneyLeg[];
}>;

export type JourneyPlan = Readonly<{
  journeys: readonly Journey[];
}>;

type FetchLike = typeof fetch;

export type JourneyApiOptions = Readonly<{
  fetchImpl?: FetchLike;
  signal?: AbortSignal;
}>;

const MODES: readonly JourneyLegMode[] = [
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

export async function requestJourneys(
  request: JourneyRequest,
  options: JourneyApiOptions = {},
): Promise<JourneyPlan> {
  validateRequest(request);
  const fetchImpl = options.fetchImpl ?? fetch;
  let response: Response;
  try {
    response = await fetchImpl("/api/v1/journeys", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
      ...(options.signal ? { signal: options.signal } : {}),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new JourneyApiError("unavailable", "Public-transit planning is temporarily unavailable.");
  }

  if (!response.ok) throw await mapFailure(response);

  let payload: unknown;
  try {
    payload = await response.json();
  } catch {
    throw invalidResponse();
  }
  return parsePlan(payload);
}

export function toJourneyOverlay(journey: Journey): JourneyOverlay {
  const overlay: JourneyOverlay = {
    legs: journey.legs.map((leg) => ({
      mode: leg.mode,
      origin: leg.origin,
      destination: leg.destination,
      ...(leg.geometry.length ? { geometry: leg.geometry } : {}),
    })),
  };
  try {
    validateJourneyOverlay(overlay);
  } catch {
    throw invalidResponse();
  }
  return overlay;
}

function validateRequest(request: JourneyRequest): void {
  if (!isCoordinate(request.origin) || !isCoordinate(request.destination)) {
    throw new JourneyApiError("invalid-request", "Journey endpoints are invalid.");
  }
  if (request.timeMode !== "DEPART_AT" && request.timeMode !== "ARRIVE_BY") {
    throw new JourneyApiError("invalid-request", "Journey time mode is invalid.");
  }
  if (!isOffsetAwareTimestamp(request.time)) {
    throw new JourneyApiError("invalid-request", "Choose a valid local date and time.");
  }
}

async function mapFailure(response: Response): Promise<JourneyApiError> {
  let code: string | undefined;
  try {
    const payload = (await response.json()) as unknown;
    if (payload && typeof payload === "object" && "code" in payload && typeof payload.code === "string") {
      code = payload.code;
    }
  } catch {
    // HTTP status remains the fallback.
  }

  if (code === "journey.no-journey") {
    return new JourneyApiError("no-journey", "No public-transit journey was found for that time.");
  }
  if (code === "journey.rate-limited") {
    return new JourneyApiError("rate-limited", "Public-transit planning is rate limited. Try again shortly.");
  }
  if (code === "journey.invalid-provider-response") {
    return new JourneyApiError("invalid-response", "The journey provider returned an invalid response.");
  }
  if (code === "journey.timeout" || code === "journey.provider-unavailable") {
    return new JourneyApiError("unavailable", "Public-transit planning is temporarily unavailable.");
  }
  if (code === "invalid-input") {
    return new JourneyApiError("invalid-request", "The public-transit request is invalid.");
  }

  if (response.status === 400) return new JourneyApiError("invalid-request", "The public-transit request is invalid.");
  if (response.status === 404) return new JourneyApiError("no-journey", "No public-transit journey was found for that time.");
  if (response.status === 429) return new JourneyApiError("rate-limited", "Public-transit planning is rate limited. Try again shortly.");
  if (response.status === 502) return new JourneyApiError("invalid-response", "The journey provider returned an invalid response.");
  if (response.status === 503 || response.status === 504) {
    return new JourneyApiError("unavailable", "Public-transit planning is temporarily unavailable.");
  }
  return new JourneyApiError("invalid-response", "The Orientation journey request was rejected.");
}

function parsePlan(payload: unknown): JourneyPlan {
  if (!isRecord(payload) || !Array.isArray(payload.journeys) || payload.journeys.length < 1 || payload.journeys.length > 8) {
    throw invalidResponse();
  }
  return Object.freeze({ journeys: Object.freeze(payload.journeys.map(parseJourney)) });
}

function parseJourney(value: unknown): Journey {
  if (!isRecord(value) || !isTimestamp(value.departureTime) || !isTimestamp(value.arrivalTime)) throw invalidResponse();
  if (!isNonNegativeNumber(value.durationSeconds) || !Number.isInteger(value.transfers) || value.transfers < 0) throw invalidResponse();
  if (!Array.isArray(value.legs) || value.legs.length < 1 || value.legs.length > 64) throw invalidResponse();
  const legs = Object.freeze(value.legs.map(parseLeg));
  if (!legs.some((leg) => leg.mode !== "WALK")) throw invalidResponse();
  return Object.freeze({
    departureTime: value.departureTime,
    arrivalTime: value.arrivalTime,
    durationSeconds: value.durationSeconds,
    transfers: value.transfers,
    legs,
  });
}

function parseLeg(value: unknown): JourneyLeg {
  if (!isRecord(value) || !MODES.includes(value.mode as JourneyLegMode)) throw invalidResponse();
  if (!isNonNegativeNumber(value.durationSeconds)) throw invalidResponse();
  if (!Array.isArray(value.geometry) || value.geometry.length > 10_000 || !value.geometry.every(isCoordinate)) throw invalidResponse();
  if (!Array.isArray(value.intermediateStops) || value.intermediateStops.length > 128) throw invalidResponse();
  const mode = value.mode as JourneyLegMode;
  const transitService = parseTransitService(value.transitService);
  if ((mode === "WALK") !== (transitService === null)) throw invalidResponse();
  return Object.freeze({
    mode,
    origin: parseStop(value.origin),
    destination: parseStop(value.destination),
    departure: parseEventTime(value.departure),
    arrival: parseEventTime(value.arrival),
    durationSeconds: value.durationSeconds,
    transitService,
    geometry: Object.freeze(value.geometry.map(copyCoordinate)),
    intermediateStops: Object.freeze(value.intermediateStops.map(parseStop)),
  });
}

function parseStop(value: unknown): JourneyStop {
  if (!isRecord(value) || typeof value.name !== "string" || !value.name.trim() || !isCoordinate(value.coordinate)) throw invalidResponse();
  return Object.freeze({ name: value.name, coordinate: copyCoordinate(value.coordinate) });
}

function parseEventTime(value: unknown): JourneyEventTime {
  if (!isRecord(value) || !isTimestamp(value.scheduledTime)) throw invalidResponse();
  if (value.realtimeTime !== null && value.realtimeTime !== undefined && !isTimestamp(value.realtimeTime)) throw invalidResponse();
  return Object.freeze({ scheduledTime: value.scheduledTime, realtimeTime: typeof value.realtimeTime === "string" ? value.realtimeTime : null });
}

function parseTransitService(value: unknown): TransitService | null {
  if (value === null || value === undefined) return null;
  if (!isRecord(value) || typeof value.label !== "string" || !value.label.trim()) throw invalidResponse();
  if (value.headsign !== null && value.headsign !== undefined && typeof value.headsign !== "string") throw invalidResponse();
  return Object.freeze({ label: value.label, headsign: typeof value.headsign === "string" ? value.headsign : null });
}

function isOffsetAwareTimestamp(value: unknown): value is string {
  return typeof value === "string" && /(?:Z|[+-]\d{2}:\d{2})$/.test(value) && isTimestamp(value);
}

function isTimestamp(value: unknown): value is string {
  return typeof value === "string" && value.trim().length > 0 && !Number.isNaN(Date.parse(value));
}

function isNonNegativeNumber(value: unknown): value is number {
  return typeof value === "number" && Number.isFinite(value) && value >= 0;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function copyCoordinate(coordinate: Coordinate): Coordinate {
  return Object.freeze({ longitude: coordinate.longitude, latitude: coordinate.latitude });
}

function invalidResponse(): JourneyApiError {
  return new JourneyApiError("invalid-response", "The Orientation journey response is invalid.");
}
