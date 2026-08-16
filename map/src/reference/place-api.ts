import { isCoordinate, type Coordinate } from "../lib/model";

export type PlaceAddress = Readonly<{
  name: string | null;
  street: string | null;
  houseNumber: string | null;
  postcode: string | null;
  city: string | null;
  county: string | null;
  state: string | null;
  country: string | null;
  countryCode: string | null;
}>;

export type Place = Readonly<{
  providerReference: string;
  displayLabel: string;
  coordinate: Coordinate;
  extent: Readonly<{ west: number; south: number; east: number; north: number }> | null;
  kind: string | null;
  address: PlaceAddress;
}>;

export type PlaceApiFailureKind = "invalid-response" | "rate-limited" | "unavailable";

export class PlaceApiError extends Error {
  constructor(readonly kind: PlaceApiFailureKind, message: string) {
    super(message);
    this.name = "PlaceApiError";
  }
}

type FetchLike = typeof fetch;

export type PlaceApiOptions = Readonly<{
  fetchImpl?: FetchLike;
  signal?: AbortSignal;
}>;

export async function searchPlaces(query: string, options: PlaceApiOptions = {}): Promise<readonly Place[]> {
  const params = new URLSearchParams({ q: query, limit: "5" });
  return readPlaces(await request(`/api/v1/places/search?${params.toString()}`, options), "places");
}

export async function reversePlace(coordinate: Coordinate, options: PlaceApiOptions = {}): Promise<Place | null> {
  if (!isCoordinate(coordinate)) {
    throw new PlaceApiError("invalid-response", "The map center coordinate is invalid.");
  }
  const params = new URLSearchParams({
    lat: String(coordinate.latitude),
    lon: String(coordinate.longitude),
  });
  const payload = await request(`/api/v1/places/reverse?${params.toString()}`, options);
  if (!payload || typeof payload !== "object" || !("place" in payload)) {
    throw new PlaceApiError("invalid-response", "The Orientation response is invalid.");
  }
  return payload.place === null ? null : parsePlace(payload.place);
}

async function request(path: string, options: PlaceApiOptions): Promise<unknown> {
  const fetchImpl = options.fetchImpl ?? fetch;
  let response: Response;
  try {
    response = await fetchImpl(path, {
      headers: { Accept: "application/json" },
      ...(options.signal ? { signal: options.signal } : {}),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new PlaceApiError("unavailable", "Place search is temporarily unavailable.");
  }
  if (!response.ok) {
    if (response.status === 429) throw new PlaceApiError("rate-limited", "Place search is rate limited. Try again shortly.");
    if ([502, 503, 504].includes(response.status)) throw new PlaceApiError("unavailable", "Place search is temporarily unavailable.");
    throw new PlaceApiError("invalid-response", "The Orientation place request was rejected.");
  }
  try {
    return await response.json();
  } catch {
    throw new PlaceApiError("invalid-response", "The Orientation response is invalid.");
  }
}

function readPlaces(payload: unknown, field: "places"): readonly Place[] {
  if (!payload || typeof payload !== "object" || !(field in payload) || !Array.isArray(payload[field])) {
    throw new PlaceApiError("invalid-response", "The Orientation response is invalid.");
  }
  return payload[field].map(parsePlace);
}

function parsePlace(value: unknown): Place {
  if (!value || typeof value !== "object") throw invalidPlace();
  const candidate = value as Record<string, unknown>;
  if (typeof candidate.providerReference !== "string" || !candidate.providerReference.trim()) throw invalidPlace();
  if (typeof candidate.displayLabel !== "string" || !candidate.displayLabel.trim()) throw invalidPlace();
  if (!isCoordinate(candidate.coordinate)) throw invalidPlace();
  if (candidate.extent !== null && candidate.extent !== undefined && !isExtent(candidate.extent)) throw invalidPlace();
  if (candidate.kind !== null && candidate.kind !== undefined && typeof candidate.kind !== "string") throw invalidPlace();
  if (!isAddress(candidate.address)) throw invalidPlace();
  return {
    providerReference: candidate.providerReference.trim(),
    displayLabel: candidate.displayLabel.trim(),
    coordinate: { longitude: candidate.coordinate.longitude, latitude: candidate.coordinate.latitude },
    extent: candidate.extent === null || candidate.extent === undefined ? null : candidate.extent,
    kind: candidate.kind === null || candidate.kind === undefined ? null : candidate.kind,
    address: candidate.address,
  };
}

function isExtent(value: unknown): value is Place["extent"] {
  if (!value || typeof value !== "object") return false;
  const extent = value as Record<string, unknown>;
  const west = extent.west;
  const south = extent.south;
  const east = extent.east;
  const north = extent.north;
  return [west, south, east, north].every((value) => typeof value === "number" && Number.isFinite(value))
    && (west as number) >= -180 && (east as number) <= 180
    && (south as number) >= -90 && (north as number) <= 90
    && (west as number) <= (east as number) && (south as number) <= (north as number);
}

function isAddress(value: unknown): value is PlaceAddress {
  if (!value || typeof value !== "object") return false;
  const address = value as Record<string, unknown>;
  return ["name", "street", "houseNumber", "postcode", "city", "county", "state", "country", "countryCode"]
    .every((key) => address[key] === null || typeof address[key] === "string");
}

function invalidPlace(): PlaceApiError {
  return new PlaceApiError("invalid-response", "The Orientation place response is invalid.");
}
