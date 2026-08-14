export type Coordinate = Readonly<{
  longitude: number;
  latitude: number;
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
  coordinate: Coordinate;
  title: string;
  subtitle?: string;
  resources?: readonly SpatialResource[];
  actions?: readonly SpatialAction[];
}>;

export type SpatialScene = Readonly<{
  features: readonly SpatialFeature[];
}>;

export function isCoordinate(value: Coordinate): boolean {
  return (
    Number.isFinite(value.longitude) &&
    Number.isFinite(value.latitude) &&
    value.longitude >= -180 &&
    value.longitude <= 180 &&
    value.latitude >= -90 &&
    value.latitude <= 90
  );
}

export function validateScene(scene: SpatialScene): void {
  const refs = new Set<string>();

  for (const feature of scene.features) {
    if (!feature.ref.trim()) {
      throw new Error("Spatial feature ref must be non-empty.");
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
