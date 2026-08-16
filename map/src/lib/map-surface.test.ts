import { describe, expect, it } from "vitest";
import { BASEMAP_READINESS_TIMEOUT_MS, DEFAULT_BASEMAP_STYLE_URL } from "./map-surface";

describe("Orientation default basemap", () => {
  it("uses the production-appropriate OpenFreeMap Liberty style", () => {
    expect(DEFAULT_BASEMAP_STYLE_URL).toBe("https://tiles.openfreemap.org/styles/liberty");
    expect(DEFAULT_BASEMAP_STYLE_URL).not.toContain("demotiles.maplibre.org");
  });

  it("uses a bounded readiness timeout for silent worker/bootstrap failures", () => {
    expect(BASEMAP_READINESS_TIMEOUT_MS).toBe(15_000);
  });
});
