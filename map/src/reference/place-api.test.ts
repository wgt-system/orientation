import { describe, expect, it, vi } from "vitest";
import { PlaceApiError, reversePlace, searchPlaces } from "./place-api";

const place = {
  providerReference: "N:123",
  displayLabel: "Hamburg Hauptbahnhof",
  coordinate: { longitude: 9.99, latitude: 53.55 },
  extent: null,
  kind: null,
  address: { name: "Hamburg Hauptbahnhof", street: "Hauptbahnhof", houseNumber: "1", postcode: "20095", city: "Hamburg", county: null, state: "Hamburg", country: "Germany", countryCode: "DE" },
};

function response(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

describe("place API client", () => {
  it("encodes search text, uses limit five and never adds a bias", async () => {
    const fetchImpl = vi.fn(async (input: RequestInfo | URL) => {
      expect(String(input)).toContain("/api/v1/places/search?");
      expect(String(input)).toContain("q=Hamburg+Hauptbahnhof");
      expect(String(input)).toContain("limit=5");
      expect(String(input)).not.toContain("photon.komoot.io");
      expect(String(input)).not.toContain("lat=");
      return response({ places: [place] });
    });

    await expect(searchPlaces("Hamburg Hauptbahnhof", { fetchImpl })).resolves.toHaveLength(1);
  });

  it("validates success, empty and reverse-null payloads", async () => {
    const fetchImpl = vi.fn()
      .mockResolvedValueOnce(response({ places: [] }))
      .mockResolvedValueOnce(response({ place }))
      .mockResolvedValueOnce(response({ place: null }));
    await expect(searchPlaces("unknown", { fetchImpl })).resolves.toEqual([]);
    await expect(reversePlace({ longitude: 9.99, latitude: 53.55 }, { fetchImpl })).resolves.toMatchObject({ displayLabel: place.displayLabel });
    await expect(reversePlace({ longitude: 9.99, latitude: 53.55 }, { fetchImpl })).resolves.toBeNull();
  });

  it.each([400, 429, 502, 503])("maps HTTP %s to a controlled error", async (status) => {
    const fetchImpl = vi.fn(async () => response({}, status));
    await expect(searchPlaces("x", { fetchImpl })).rejects.toBeInstanceOf(PlaceApiError);
  });

  it("rejects malformed first-party JSON", async () => {
    const fetchImpl = vi.fn(async () => response({ places: [{ ...place, coordinate: { longitude: 999, latitude: 0 } }] }));
    await expect(searchPlaces("x", { fetchImpl })).rejects.toMatchObject({ kind: "invalid-response" });
  });

  it("passes AbortController signals through to fetch", async () => {
    const controller = new AbortController();
    const fetchImpl = vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
      expect(init?.signal).toBe(controller.signal);
      throw new DOMException("aborted", "AbortError");
    });
    await expect(searchPlaces("x", { fetchImpl, signal: controller.signal })).rejects.toMatchObject({ name: "AbortError" });
  });
});
