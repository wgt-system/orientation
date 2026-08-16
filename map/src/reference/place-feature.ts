import type { Place } from "./place-api";
import type { SpatialFeature } from "../lib/model";

export const PLACE_SOURCE_REF = "orientation/place";

export function placeToSpatialFeature(place: Place): SpatialFeature {
  const address = place.address;
  const locality = [address.city, address.state, address.country].filter(Boolean).join(", ");
  const subtitle = locality || undefined;
  const rows = [
    ["Kind", place.kind],
    ["Street", address.street],
    ["House number", address.houseNumber],
    ["Postcode", address.postcode],
    ["City", address.city],
    ["County", address.county],
    ["State", address.state],
    ["Country", address.country],
  ].filter((row): row is [string, string] => typeof row[1] === "string" && Boolean(row[1].trim()));

  return {
    ref: `${PLACE_SOURCE_REF}/${place.providerReference}`,
    sourceRef: PLACE_SOURCE_REF,
    coordinate: place.coordinate,
    title: place.displayLabel,
    ...(subtitle ? { subtitle } : {}),
    ...(rows.length ? { information: [{ title: "Place details", rows: rows.map(([label, value]) => ({ label, value })) }] } : {}),
  };
}
