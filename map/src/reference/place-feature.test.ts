import { describe, expect, it } from "vitest";
import { placeToSpatialFeature } from "./place-feature";

describe("placeToSpatialFeature", () => {
  it("maps generic place data to a focused feature without provider actions", () => {
    const feature = placeToSpatialFeature({
      providerReference: "N:123",
      displayLabel: "Hamburg Hauptbahnhof",
      coordinate: { longitude: 9.99, latitude: 53.55 },
      extent: null,
      kind: null,
      address: { name: null, street: "Hauptbahnhof", houseNumber: "1", postcode: "20095", city: "Hamburg", county: null, state: null, country: "Germany", countryCode: "DE" },
    });

    expect(feature.ref).toBe("orientation/place/N:123");
    expect(feature.sourceRef).toBe("orientation/place");
    expect(feature.title).toBe("Hamburg Hauptbahnhof");
    expect(feature.coordinate).toEqual({ longitude: 9.99, latitude: 53.55 });
    expect(feature.subtitle).toBe("Hamburg, Germany");
    expect(feature.information?.[0]?.rows).toEqual([
      { label: "Street", value: "Hauptbahnhof" },
      { label: "House number", value: "1" },
      { label: "Postcode", value: "20095" },
      { label: "City", value: "Hamburg" },
      { label: "Country", value: "Germany" },
    ]);
    expect(feature.resources).toBeUndefined();
    expect(feature.actions).toBeUndefined();
  });
});
