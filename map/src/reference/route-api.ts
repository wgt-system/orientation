import { isCoordinate, type Coordinate } from "../lib/model";
import { validateRoute, type Route, type TravelProfile } from "../lib/route-overlay";

export type RouteApiFailureKind =
  | "invalid-request"
  | "no-route"
  | "rate-limited"
  | "invalid-response"
  | "unavailable";

export class RouteApiError extends Error {
  constructor(readonly kind: RouteApiFailureKind, message: string) {
    super(message);
    this.name = "RouteApiError";
  }
}

export type RouteRequest = Readonly<{
  origin: Coordinate;
  destination: Coordinate;
  profile: TravelProfile;
}>;

type FetchLike = typeof fetch;

export type RouteApiOptions = Readonly<{
  fetchImpl?: FetchLike;
  signal?: AbortSignal;
}>;

export async function requestRoute(request: RouteRequest, options: RouteApiOptions = {}): Promise<Route> {
  if (!isCoordinate(request.origin) || !isCoordinate(request.destination)) {
    throw new RouteApiError("invalid-request", "Route endpoints are invalid.");
  }
  if (!["DRIVING", "CYCLING", "WALKING"].includes(request.profile)) {
    throw new RouteApiError("invalid-request", "Travel profile is invalid.");
  }

  const fetchImpl = options.fetchImpl ?? fetch;
  let response: Response;
  try {
    response = await fetchImpl("/api/v1/routes", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        origin: request.origin,
        destination: request.destination,
        profile: request.profile,
      }),
      ...(options.signal ? { signal: options.signal } : {}),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new RouteApiError("unavailable", "Routing is temporarily unavailable.");
  }

  if (!response.ok) {
    throw await mapFailure(response);
  }

  let payload: unknown;
  try {
    payload = await response.json();
  } catch {
    throw new RouteApiError("invalid-response", "The Orientation route response is invalid.");
  }
  return parseRouteEnvelope(payload);
}

async function mapFailure(response: Response): Promise<RouteApiError> {
  let code: string | undefined;
  try {
    const payload = (await response.json()) as unknown;
    if (payload && typeof payload === "object" && "code" in payload && typeof payload.code === "string") {
      code = payload.code;
    }
  } catch {
    // The HTTP status still provides the stable fallback mapping.
  }

  if (code === "routing.no-route") {
    return new RouteApiError("no-route", "No route was found for these endpoints and profile.");
  }
  if (code === "routing.rate-limited") {
    return new RouteApiError("rate-limited", "Routing is rate limited. Try again shortly.");
  }
  if (code === "routing.invalid-provider-response") {
    return new RouteApiError("invalid-response", "The routing provider returned an invalid response.");
  }
  if (code === "routing.timeout" || code === "routing.provider-unavailable") {
    return new RouteApiError("unavailable", "Routing is temporarily unavailable.");
  }
  if (code === "invalid-input") {
    return new RouteApiError("invalid-request", "The route request is invalid.");
  }

  if (response.status === 400) {
    return new RouteApiError("invalid-request", "The route request is invalid.");
  }
  if (response.status === 404) {
    return new RouteApiError("no-route", "No route was found for these endpoints and profile.");
  }
  if (response.status === 429) {
    return new RouteApiError("rate-limited", "Routing is rate limited. Try again shortly.");
  }
  if (response.status === 502) {
    return new RouteApiError("invalid-response", "The routing provider returned an invalid response.");
  }
  if ([503, 504].includes(response.status)) {
    return new RouteApiError("unavailable", "Routing is temporarily unavailable.");
  }
  return new RouteApiError("invalid-response", "The Orientation route request was rejected.");
}

function parseRouteEnvelope(payload: unknown): Route {
  if (!payload || typeof payload !== "object" || !("route" in payload)) {
    throw invalidRouteResponse();
  }
  const candidate = payload.route;
  if (!candidate || typeof candidate !== "object") {
    throw invalidRouteResponse();
  }
  const value = candidate as Record<string, unknown>;
  if (!isCoordinate(value.origin) || !isCoordinate(value.destination)) {
    throw invalidRouteResponse();
  }
  if (!["DRIVING", "CYCLING", "WALKING"].includes(String(value.profile))) {
    throw invalidRouteResponse();
  }
  if (typeof value.distanceMeters !== "number" || typeof value.durationSeconds !== "number") {
    throw invalidRouteResponse();
  }
  if (!Array.isArray(value.geometry) || !value.geometry.every(isCoordinate)) {
    throw invalidRouteResponse();
  }

  const route: Route = {
    origin: { longitude: value.origin.longitude, latitude: value.origin.latitude },
    destination: { longitude: value.destination.longitude, latitude: value.destination.latitude },
    profile: value.profile as TravelProfile,
    geometry: value.geometry.map((coordinate) => ({
      longitude: coordinate.longitude,
      latitude: coordinate.latitude,
    })),
    distanceMeters: value.distanceMeters,
    durationSeconds: value.durationSeconds,
  };

  try {
    validateRoute(route);
  } catch {
    throw invalidRouteResponse();
  }
  return route;
}

function invalidRouteResponse(): RouteApiError {
  return new RouteApiError("invalid-response", "The Orientation route response is invalid.");
}
