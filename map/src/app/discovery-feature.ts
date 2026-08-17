import type { SpatialFeature } from "../lib/model";
import type { DiscoveryCandidate, DiscoveryDetail } from "./discovery-api";

export function candidateToSpatialFeature(
  collection: DiscoveryDetail,
  candidate: DiscoveryCandidate,
): SpatialFeature | null {
  const coordinate = candidate.researchedLocation.coordinate;
  if (!coordinate) return null;

  const criteriaByRef = new Map(collection.criteria.map((criterion) => [criterion.criterionRef, criterion]));
  const rows = candidate.claims.map((claim) => ({
    label: criteriaByRef.get(claim.criterionRef)?.description ?? claim.criterionRef,
    value: claimValue(claim),
  }));

  return {
    ref: candidate.candidateRef,
    sourceRef: `orientation/discovery/${collection.collectionId}`,
    coordinate,
    title: candidate.displayName,
    subtitle: candidate.researchedLocation.label,
    information: [{ title: "Research criteria", rows }],
    actions: [{ ref: "route-here", label: "Route here" }],
  };
}

function claimValue(claim: DiscoveryCandidate["claims"][number]): string {
  const observed = claim.observedValue ? ` · ${claim.observedValue.value}` : "";
  return `${claim.status.replace("_", " ")} · ${claim.basis.replace("_", " ")}${observed}`;
}
