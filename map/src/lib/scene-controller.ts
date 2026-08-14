import {
  type SpatialFeature,
  type SpatialFeatureSelectedEvent,
  type SpatialScene,
  validateScene,
} from "./model";

function snapshotFeature(feature: SpatialFeature): SpatialFeature {
  return Object.freeze({
    ...feature,
    coordinate: Object.freeze({ ...feature.coordinate }),
    ...(feature.resources
      ? { resources: Object.freeze(feature.resources.map((resource) => Object.freeze({ ...resource }))) }
      : {}),
    ...(feature.actions
      ? { actions: Object.freeze(feature.actions.map((action) => Object.freeze({ ...action }))) }
      : {}),
  });
}

export function snapshotScene(scene: SpatialScene): SpatialScene {
  validateScene(scene);

  return Object.freeze({
    ...scene,
    ...(scene.viewport ? { viewport: Object.freeze({ ...scene.viewport }) } : {}),
    features: Object.freeze(scene.features.map(snapshotFeature)),
  });
}

export class SpatialSceneController {
  private scene: SpatialScene = snapshotScene({ features: [] });

  replace(scene: SpatialScene): SpatialScene {
    this.scene = snapshotScene(scene);
    return this.scene;
  }

  clear(): SpatialScene {
    return this.replace({ features: [] });
  }

  current(): SpatialScene {
    return this.scene;
  }

  select(featureRef: string): SpatialFeatureSelectedEvent {
    const feature = this.scene.features.find((candidate) => candidate.ref === featureRef);
    if (!feature) {
      throw new Error(`Unknown spatial feature ref: ${featureRef}`);
    }

    return Object.freeze({
      featureRef: feature.ref,
      sourceRef: feature.sourceRef,
    });
  }
}
