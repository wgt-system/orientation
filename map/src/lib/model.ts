export type Coordinate = Readonly<{
  longitude: number;
  latitude: number;
}>;

export type ViewportIntent = Readonly<{
  kind: "automatic" | "preserve";
  padding?: number;
  maxZoom?: number;
}>;

export type CoordinateBounds = Readonly<{
  southWest: Coordinate;
  northEast: Coordinate;
}>;

export type ResolvedViewport =
  | Readonly<{ kind: "empty" }>
  | Readonly<{ kind: "preserve" }>
  | Readonly<{ kind: "focus"; coordinate: Coordinate; zoom: number }>
  | Readonly<{
      kind: "fit";
      bounds: CoordinateBounds;
      padding: number;
      maxZoom: number;
    }>;

export type SpatialResource = Readonly<{
  ref: string;
  label: string;
  uri?: string;
}>;

export type SpatialAction = Readonly<{
  ref: string;
  label: string;
}>;

export type SpatialInformationRow = Readonly<{
  label: string;
  value: string;
}>;

export type SpatialInformationSection = Readonly<{
  title?: string;
  rows: readonly SpatialInformationRow[];
}>;

export type SpatialFeature = Readonly<{
  ref: string;
  sourceRef: string;
  coordinate: Coordinate;
  title: string;
  subtitle?: string;
  information?: readonly SpatialInformationSection[];
  resources?: readonly SpatialResource[];
  actions?: readonly SpatialAction[];
}>;

export type SpatialScene = Readonly<{
  features: readonly SpatialFeature[];
  viewport?: ViewportIntent;
}>;

export type SpatialFeatureSelectedEvent = Readonly<{
  featureRef: string;
  sourceRef: string;
}>;

export type SpatialResourceActivatedEvent = Readonly<{
  featureRef: string;
  sourceRef: string;
  resourceRef: string;
}>;

export type SpatialActionActivatedEvent = Readonly<{
  featureRef: string;
  sourceRef: string;
  actionRef: string;
}>;

export function isCoordinate(value: unknown): value is Coordinate {
  if (!value || typeof value !== "object") {
    return false;
  }

  const coordinate = value as Coordinate;
  return (
    Number.isFinite(coordinate.longitude) &&
    Number.isFinite(coordinate.latitude) &&
    coordinate.longitude >= -180 &&
    coordinate.longitude <= 180 &&
    coordinate.latitude >= -90 &&
    coordinate.latitude <= 90
  );
}

function requireNonEmpty(value: unknown, message: string): asserts value is string {
  if (typeof value !== "string" || !value.trim()) {
    throw new Error(message);
  }
}

function validateResource(resource: SpatialResource, featureRef: string, refs: Set<string>): void {
  requireNonEmpty(resource.ref, `Spatial resource ref must be non-empty: ${featureRef}`);
  requireNonEmpty(resource.label, `Spatial resource label must be non-empty: ${featureRef}`);
  if (refs.has(resource.ref)) {
    throw new Error(`Duplicate spatial resource ref: ${resource.ref}`);
  }
  refs.add(resource.ref);

  if (resource.uri !== undefined) {
    if (typeof resource.uri !== "string" || !resource.uri.trim()) {
      throw new Error(`Spatial resource URI must be a valid HTTP(S) URI: ${resource.ref}`);
    }
    let parsed: URL;
    try {
      parsed = new URL(resource.uri);
    } catch {
      throw new Error(`Spatial resource URI must be a valid HTTP(S) URI: ${resource.ref}`);
    }
    if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
      throw new Error(`Spatial resource URI scheme is not allowed: ${resource.ref}`);
    }
  }
}

function validateFeatureContent(feature: SpatialFeature): void {
  if (feature.information !== undefined) {
    if (!Array.isArray(feature.information)) {
      throw new Error(`Spatial feature information must be an array: ${feature.ref}`);
    }
    feature.information.forEach((section: SpatialInformationSection) => {
      if (section.title !== undefined) {
        requireNonEmpty(section.title, `Spatial information section title must be non-empty: ${feature.ref}`);
      }
      if (!Array.isArray(section.rows) || section.rows.length === 0) {
        throw new Error(`Spatial information section rows must be non-empty: ${feature.ref}`);
      }
      section.rows.forEach((row) => {
        requireNonEmpty(row.label, `Spatial information row label must be non-empty: ${feature.ref}`);
        requireNonEmpty(row.value, `Spatial information row value must be non-empty: ${feature.ref}`);
      });
    });
  }

  if (feature.resources !== undefined) {
    if (!Array.isArray(feature.resources)) {
      throw new Error(`Spatial feature resources must be an array: ${feature.ref}`);
    }
    const refs = new Set<string>();
    feature.resources.forEach((resource) => validateResource(resource, feature.ref, refs));
  }

  if (feature.actions !== undefined) {
    if (!Array.isArray(feature.actions)) {
      throw new Error(`Spatial feature actions must be an array: ${feature.ref}`);
    }
    const refs = new Set<string>();
    feature.actions.forEach((action) => {
      requireNonEmpty(action.ref, `Spatial action ref must be non-empty: ${feature.ref}`);
      requireNonEmpty(action.label, `Spatial action label must be non-empty: ${feature.ref}`);
      if (refs.has(action.ref)) {
        throw new Error(`Duplicate spatial action ref: ${action.ref}`);
      }
      refs.add(action.ref);
    });
  }
}

export function validateScene(scene: SpatialScene): void {
  if (!scene || !Array.isArray(scene.features)) {
    throw new Error("Spatial scene features must be an array.");
  }

  if (scene.viewport) {
    if (!["automatic", "preserve"].includes(scene.viewport.kind)) {
      throw new Error("Unsupported spatial scene viewport intent.");
    }
    if (
      scene.viewport.padding !== undefined &&
      (!Number.isFinite(scene.viewport.padding) || scene.viewport.padding < 0)
    ) {
      throw new Error("Spatial scene viewport padding must be non-negative.");
    }
    if (
      scene.viewport.maxZoom !== undefined &&
      (!Number.isFinite(scene.viewport.maxZoom) || scene.viewport.maxZoom <= 0)
    ) {
      throw new Error("Spatial scene viewport max zoom must be positive.");
    }
  }

  const refs = new Set<string>();

  for (const feature of scene.features) {
    requireNonEmpty(feature.ref, "Spatial feature ref must be non-empty.");
    requireNonEmpty(feature.sourceRef, `Spatial feature source ref must be non-empty: ${feature.ref}`);
    if (refs.has(feature.ref)) {
      throw new Error(`Duplicate spatial feature ref: ${feature.ref}`);
    }
    refs.add(feature.ref);

    if (!isCoordinate(feature.coordinate)) {
      throw new Error(`Invalid coordinate for feature: ${feature.ref}`);
    }
    requireNonEmpty(feature.title, `Spatial feature title must be non-empty: ${feature.ref}`);
    validateFeatureContent(feature);
  }
}

export function resolveViewport(scene: SpatialScene): ResolvedViewport {
  validateScene(scene);

  if (scene.viewport?.kind === "preserve") {
    return { kind: "preserve" };
  }

  if (scene.features.length === 0) {
    return { kind: "empty" };
  }

  if (scene.features.length === 1) {
    return {
      kind: "focus",
      coordinate: scene.features[0]!.coordinate,
      zoom: Math.min(scene.viewport?.maxZoom ?? 12, 12),
    };
  }

  const latitudes = scene.features.map((feature) => feature.coordinate.latitude);
  const longitudes = scene.features.map((feature) => feature.coordinate.longitude);
  const longitudeBounds = resolveMinimalLongitudeSpan(longitudes);

  return {
    kind: "fit",
    bounds: {
      southWest: {
        longitude: longitudeBounds.start,
        latitude: Math.min(...latitudes),
      },
      northEast: {
        longitude: longitudeBounds.end,
        latitude: Math.max(...latitudes),
      },
    },
    padding: scene.viewport?.padding ?? 48,
    maxZoom: scene.viewport?.maxZoom ?? 14,
  };
}

function resolveMinimalLongitudeSpan(longitudes: readonly number[]): Readonly<{ start: number; end: number }> {
  const sorted = [...longitudes].sort((left, right) => left - right);
  let largestGap = -1;
  let largestGapIndex = 0;

  for (let index = 0; index < sorted.length; index += 1) {
    const current = sorted[index]!;
    const next = index === sorted.length - 1 ? sorted[0]! + 360 : sorted[index + 1]!;
    const gap = next - current;
    if (gap > largestGap) {
      largestGap = gap;
      largestGapIndex = index;
    }
  }

  const startIndex = (largestGapIndex + 1) % sorted.length;
  const start = sorted[startIndex]!;
  const end = start + (360 - largestGap);
  return { start, end };
}
