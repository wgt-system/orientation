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

export type SpatialFeature = Readonly<{
  ref: string;
  sourceRef: string;
  coordinate: Coordinate;
  title: string;
  subtitle?: string;
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
    if (!feature.ref.trim()) {
      throw new Error("Spatial feature ref must be non-empty.");
    }
    if (!feature.sourceRef.trim()) {
      throw new Error(`Spatial feature source ref must be non-empty: ${feature.ref}`);
    }
    if (refs.has(feature.ref)) {
      throw new Error(`Duplicate spatial feature ref: ${feature.ref}`);
    }
    refs.add(feature.ref);

    if (!isCoordinate(feature.coordinate)) {
      throw new Error(`Invalid coordinate for feature: ${feature.ref}`);
    }
    if (!feature.title.trim()) {
      throw new Error(`Spatial feature title must be non-empty: ${feature.ref}`);
    }
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

  const longitudes = scene.features.map((feature) => feature.coordinate.longitude);
  const latitudes = scene.features.map((feature) => feature.coordinate.latitude);

  return {
    kind: "fit",
    bounds: {
      southWest: {
        longitude: Math.min(...longitudes),
        latitude: Math.min(...latitudes),
      },
      northEast: {
        longitude: Math.max(...longitudes),
        latitude: Math.max(...latitudes),
      },
    },
    padding: scene.viewport?.padding ?? 48,
    maxZoom: scene.viewport?.maxZoom ?? 14,
  };
}
