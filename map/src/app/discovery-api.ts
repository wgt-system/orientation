export type EvaluationMode = "EVIDENCE_REQUIRED" | "HEURISTIC";
export type ClaimStatus = "MATCH" | "NO_MATCH" | "UNCERTAIN" | "UNKNOWN";
export type ClaimBasis = "DIRECT_EVIDENCE" | "HEURISTIC";

export interface CoordinateDto {
  readonly longitude: number;
  readonly latitude: number;
}

export interface ResearchPromptRequest {
  readonly questionRef: string;
  readonly text: string;
  readonly area: {
    readonly center: {
      readonly label: string;
      readonly coordinate?: CoordinateDto;
    };
    readonly radiusMeters: number;
  };
  readonly criteria: readonly {
    readonly criterionRef: string;
    readonly description: string;
    readonly evaluationMode: EvaluationMode;
  }[];
}

export interface ResearchPromptResponse {
  readonly contract: string;
  readonly version: string;
  readonly schemaId: string;
  readonly prompt: string;
}

export interface DiscoveryImportResponse {
  readonly status: "CREATED" | "UNCHANGED" | "REJECTED";
  readonly collectionId: string | null;
  readonly candidateCount: number;
  readonly sourceCount: number;
  readonly errors: readonly string[];
}

export interface DiscoverySummary {
  readonly collectionId: string;
  readonly researchedAt: string;
  readonly questionRef: string;
  readonly questionText: string;
  readonly centerLabel: string;
  readonly radiusMeters: number;
  readonly candidateCount: number;
}

export interface DiscoveryDetail {
  readonly collectionId: string;
  readonly researchedAt: string;
  readonly question: {
    readonly questionRef: string;
    readonly text: string;
    readonly centerLabel: string;
    readonly centerCoordinate: CoordinateDto | null;
    readonly radiusMeters: number;
  };
  readonly criteria: readonly {
    readonly criterionRef: string;
    readonly description: string;
    readonly evaluationMode: EvaluationMode;
  }[];
  readonly sources: readonly {
    readonly sourceRef: string;
    readonly url: string;
    readonly title: string | null;
    readonly retrievedAt: string;
  }[];
  readonly candidates: readonly DiscoveryCandidate[];
}

export interface DiscoveryCandidate {
  readonly candidateRef: string;
  readonly displayName: string;
  readonly identity: {
    readonly canonicalUri: string | null;
    readonly externalIds: readonly { readonly provider: string; readonly id: string }[];
  } | null;
  readonly researchedLocation: {
    readonly label: string;
    readonly coordinate: CoordinateDto | null;
    readonly sourceRefs: readonly string[];
  };
  readonly claims: readonly {
    readonly criterionRef: string;
    readonly status: ClaimStatus;
    readonly basis: ClaimBasis;
    readonly observedValue: { readonly kind: "TEXT" | "NUMBER" | "BOOLEAN"; readonly value: string } | null;
    readonly sourceRefs: readonly string[];
    readonly note: string | null;
  }[];
}

export class DiscoveryApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
    this.name = "DiscoveryApiError";
  }
}

export async function generateResearchPrompt(
  request: ResearchPromptRequest,
  options: { readonly signal?: AbortSignal } = {},
): Promise<ResearchPromptResponse> {
  return requestJson<ResearchPromptResponse>("/api/v1/research/prompts", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    ...(options.signal ? { signal: options.signal } : {}),
  });
}

export async function importDiscoveryBundle(
  bundleJson: string,
  options: { readonly signal?: AbortSignal } = {},
): Promise<DiscoveryImportResponse> {
  let parsed: unknown;
  try {
    parsed = JSON.parse(bundleJson);
  } catch {
    return { status: "REJECTED", collectionId: null, candidateCount: 0, sourceCount: 0, errors: ["Bundle is not valid JSON."] };
  }

  let response: Response;
  try {
    response = await fetch("/api/v1/discovery/imports", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(parsed),
      ...(options.signal ? { signal: options.signal } : {}),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new DiscoveryApiError("Orientation backend is temporarily unavailable.", 0);
  }

  let body: DiscoveryImportResponse;
  try {
    body = (await response.json()) as DiscoveryImportResponse;
  } catch {
    throw new DiscoveryApiError("Orientation returned an invalid discovery response.", response.status);
  }
  if (!response.ok && response.status !== 400) {
    throw new DiscoveryApiError("Discovery import is temporarily unavailable.", response.status);
  }
  return body;
}

export async function listDiscoveryCollections(
  options: { readonly signal?: AbortSignal } = {},
): Promise<readonly DiscoverySummary[]> {
  return requestJson<readonly DiscoverySummary[]>("/api/v1/discovery/collections", {
    ...(options.signal ? { signal: options.signal } : {}),
  });
}

export async function getDiscoveryCollection(
  collectionId: string,
  options: { readonly signal?: AbortSignal } = {},
): Promise<DiscoveryDetail> {
  return requestJson<DiscoveryDetail>(`/api/v1/discovery/collections/${encodeURIComponent(collectionId)}`, {
    ...(options.signal ? { signal: options.signal } : {}),
  });
}

async function requestJson<T>(url: string, init: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, init);
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new DiscoveryApiError("Orientation backend is temporarily unavailable.", 0);
  }
  if (!response.ok) {
    throw new DiscoveryApiError(response.status === 404 ? "Discovery collection was not found." : "Orientation request failed.", response.status);
  }
  try {
    return (await response.json()) as T;
  } catch {
    throw new DiscoveryApiError("Orientation returned an invalid response.", response.status);
  }
}
