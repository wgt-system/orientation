import {
  type SpatialFeature,
  type SpatialActionActivatedEvent,
  type SpatialResourceActivatedEvent,
  type SpatialFeatureSelectedEvent,
  type SpatialScene,
  validateScene,
} from "./model";

function snapshotFeature(feature: SpatialFeature): SpatialFeature {
  return Object.freeze({
    ...feature,
    coordinate: Object.freeze({ ...feature.coordinate }),
    ...(feature.information
      ? {
          information: Object.freeze(
            feature.information.map((section) =>
              Object.freeze({
                ...section,
                rows: Object.freeze(section.rows.map((row) => Object.freeze({ ...row }))),
              }),
            ),
          ),
        }
      : {}),
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

  find(featureRef: string, sourceRef: string): SpatialFeature | undefined {
    return this.scene.features.find(
      (candidate) => candidate.ref === featureRef && candidate.sourceRef === sourceRef,
    );
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

  activateResource(featureRef: string, resourceRef: string): SpatialResourceActivatedEvent {
    const feature = this.requireFeature(featureRef);
    if (!feature.resources?.some((resource) => resource.ref === resourceRef)) {
      throw new Error(`Unknown spatial resource ref: ${resourceRef}`);
    }
    return Object.freeze({ featureRef, sourceRef: feature.sourceRef, resourceRef });
  }

  activateAction(featureRef: string, actionRef: string): SpatialActionActivatedEvent {
    const feature = this.requireFeature(featureRef);
    if (!feature.actions?.some((action) => action.ref === actionRef)) {
      throw new Error(`Unknown spatial action ref: ${actionRef}`);
    }
    return Object.freeze({ featureRef, sourceRef: feature.sourceRef, actionRef });
  }

  private requireFeature(featureRef: string): SpatialFeature {
    const feature = this.scene.features.find((candidate) => candidate.ref === featureRef);
    if (!feature) {
      throw new Error(`Unknown spatial feature ref: ${featureRef}`);
    }
    return feature;
  }
}
