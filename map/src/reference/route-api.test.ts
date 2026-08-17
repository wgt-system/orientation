import { describe, expect, it, vi } from "vitest";
import { RouteApiError, requestRoute } from "./route-api";

const route = {
  origin: { longitude: 10.0067, latitude: 53.5526 },
  destination: { longitude: 9.9921, latitude: 53.5504 },
  profile: "WALKING",
  distanceMeters: 1103,
  durationSeconds: 781.912,
  geometry: [
    { longitude: 10.0067, latitude: 53.5526 },
    { longitude: 10.0, latitude: 53.5515 },
    { longitude: 9.9921, latitude: 53.5504 },
  ],
};

function response(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("route API client", () => {
  it("posts only Orientation endpoint coordinates and generic profile", async () => {
    const fetchImpl = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      expect(String(input)).toBe("/api/v1/routes");
      expect(init?.method).toBe("POST");
      expect(JSON.parse(String(init?.body))).toEqual({
        origin: route.origin,
        destination: route.destination,
        profile: "WALKING",
      });
      expect(String(init?.body)).not.toContain("pedestrian");
      expect(String(init?.body)).not.toContain("valhalla");
      return response({ route });
    });

    await expect(
      requestRoute(
        { origin: route.origin, destination: route.destination, profile: "WALKING" },
        { fetchImpl },
      ),
    ).resolves.toMatchObject({ profile: "WALKING", distanceMeters: 1103 });
  });

  it.each([
    [400, "invalid-request"],
    [404, "no-route"],
    [429, "rate-limited"],
    [502, "invalid-response"],
    [503, "unavailable"],
    [504, "unavailable"],
  ] as const)("maps HTTP %s to %s", async (status, kind) => {
    const fetchImpl = vi.fn(async () => response({}, status));
    await expect(
      requestRoute(
        { origin: route.origin, destination: route.destination, profile: "WALKING" },
        { fetchImpl },
      ),
    ).rejects.toMatchObject({ kind });
  });

  it("prefers stable Orientation error codes over a fallback HTTP status", async () => {
    const fetchImpl = vi.fn(async () =>
      response({ code: "routing.no-route", message: "provider detail" }, 400),
    );
    await expect(
      requestRoute(
        { origin: route.origin, destination: route.destination, profile: "DRIVING" },
        { fetchImpl },
      ),
    ).rejects.toMatchObject({ kind: "no-route" });
  });

  it("rejects malformed successful route payloads", async () => {
    for (const malformed of [
      {},
      { route: { ...route, profile: "TRANSIT" } },
      { route: { ...route, distanceMeters: -1 } },
      { route: { ...route, geometry: [route.origin] } },
      { route: { ...route, geometry: [{ longitude: 999, latitude: 0 }, route.destination] } },
    ]) {
      const fetchImpl = vi.fn(async () => response(malformed));
      await expect(
        requestRoute(
          { origin: route.origin, destination: route.destination, profile: "WALKING" },
          { fetchImpl },
        ),
      ).rejects.toMatchObject({ kind: "invalid-response" });
    }
  });

  it("rejects invalid client-side endpoints before fetch", async () => {
    const fetchImpl = vi.fn();
    await expect(
      requestRoute(
        {
          origin: { longitude: 999, latitude: 0 },
          destination: route.destination,
          profile: "WALKING",
        },
        { fetchImpl },
      ),
    ).rejects.toBeInstanceOf(RouteApiError);
    expect(fetchImpl).not.toHaveBeenCalled();
  });

  it("passes AbortController signals through to fetch", async () => {
    const controller = new AbortController();
    const fetchImpl = vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
      expect(init?.signal).toBe(controller.signal);
      throw new DOMException("aborted", "AbortError");
    });
    await expect(
      requestRoute(
        { origin: route.origin, destination: route.destination, profile: "CYCLING" },
        { fetchImpl, signal: controller.signal },
      ),
    ).rejects.toMatchObject({ name: "AbortError" });
  });
});
