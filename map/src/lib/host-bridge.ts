import { OrientationMapSurface, type OrientationMapCallbacks } from "./map-surface";
import { renderFeatureDetails } from "./feature-details";
import { snapshotScene } from "./scene-controller";
import { validatePositionFix, type PositionFix } from "./current-location";
import {
  type SpatialActionActivatedEvent,
  type SpatialFeatureSelectedEvent,
  type SpatialResourceActivatedEvent,
  type SpatialScene,
  validateScene,
} from "./model";

export const ORIENTATION_HOST_BRIDGE_CONTRACT = "orientation.host-bridge" as const;
export const ORIENTATION_HOST_BRIDGE_VERSION = "1.0" as const;

export type OrientationBridgeMessageSink = (serializedMessage: string) => void;

export type OrientationBridgeCommandHandlers = Readonly<{
  replaceScene: (scene: SpatialScene) => void;
  setCurrentPosition: (position: PositionFix) => void;
  clearCurrentPosition: () => void;
}>;

export type OrientationHostBridgeOptions = Readonly<{
  detailsContainer?: HTMLElement;
}>;

export type OrientationHostBridgeOutboundMessage =
  | Readonly<{ contract: typeof ORIENTATION_HOST_BRIDGE_CONTRACT; version: typeof ORIENTATION_HOST_BRIDGE_VERSION; type: "bridge.ready"; payload: Record<string, never> }>
  | Readonly<{ contract: typeof ORIENTATION_HOST_BRIDGE_CONTRACT; version: typeof ORIENTATION_HOST_BRIDGE_VERSION; type: "map.status"; payload: { status: "initializing" | "ready" | "error" | "destroyed" } }>
  | Readonly<{ contract: typeof ORIENTATION_HOST_BRIDGE_CONTRACT; version: typeof ORIENTATION_HOST_BRIDGE_VERSION; type: "feature.selected"; payload: SpatialFeatureSelectedEvent }>
  | Readonly<{ contract: typeof ORIENTATION_HOST_BRIDGE_CONTRACT; version: typeof ORIENTATION_HOST_BRIDGE_VERSION; type: "resource.activated"; payload: SpatialResourceActivatedEvent }>
  | Readonly<{ contract: typeof ORIENTATION_HOST_BRIDGE_CONTRACT; version: typeof ORIENTATION_HOST_BRIDGE_VERSION; type: "action.activated"; payload: SpatialActionActivatedEvent }>
  | Readonly<{ contract: typeof ORIENTATION_HOST_BRIDGE_CONTRACT; version: typeof ORIENTATION_HOST_BRIDGE_VERSION; type: "bridge.error"; payload: { code: string; message: string } }>;

export function serializeOutboundMessage(message: OrientationHostBridgeOutboundMessage): string {
  return JSON.stringify(message);
}

type InboundMessage =
  | Readonly<{ type: "scene.replace"; payload: SpatialScene }>
  | Readonly<{ type: "current-position.set"; payload: PositionFix }>
  | Readonly<{ type: "current-position.clear"; payload: Record<string, never> }>;

export class OrientationHostBridge {
  private readonly surface: OrientationMapSurface;
  private readonly core: OrientationHostBridgeCore;
  private readonly sink: OrientationBridgeMessageSink;
  private readonly options: OrientationHostBridgeOptions;
  private scene: SpatialScene = snapshotScene({ features: [] });

  constructor(container: HTMLElement, sink: OrientationBridgeMessageSink, options: OrientationHostBridgeOptions = {}) {
    this.sink = sink;
    this.options = options;
    const callbacks: OrientationMapCallbacks = {
      onStatusChanged: (event) => this.emit({
        contract: ORIENTATION_HOST_BRIDGE_CONTRACT,
        version: ORIENTATION_HOST_BRIDGE_VERSION,
        type: "map.status",
        payload: event,
      }),
      onFeatureSelected: (event) => {
        this.emitEvent("feature.selected", event);
        this.renderDetails(event.featureRef, event.sourceRef);
      },
      onResourceActivated: (event) => this.emitEvent("resource.activated", event),
      onActionActivated: (event) => this.emitEvent("action.activated", event),
    };
    this.surface = new OrientationMapSurface(container, callbacks);
    this.core = new OrientationHostBridgeCore(sink, {
      replaceScene: (scene) => {
        this.surface.setScene(scene);
        this.scene = snapshotScene(scene);
        this.options.detailsContainer?.replaceChildren();
      },
      setCurrentPosition: (position) => this.surface.setCurrentPosition(position),
      clearCurrentPosition: () => this.surface.clearCurrentPosition(),
    });
    this.core.initialize();
  }

  receive(serializedMessage: string): void {
    this.core.receive(serializedMessage);
  }

  destroy(): void {
    this.core.destroy();
    this.surface.destroy();
  }

  private emitEvent(type: "feature.selected" | "resource.activated" | "action.activated", payload: SpatialFeatureSelectedEvent | SpatialResourceActivatedEvent | SpatialActionActivatedEvent): void {
    this.emit({
      contract: ORIENTATION_HOST_BRIDGE_CONTRACT,
      version: ORIENTATION_HOST_BRIDGE_VERSION,
      type,
      payload,
    } as OrientationHostBridgeOutboundMessage);
  }

  private emit(message: OrientationHostBridgeOutboundMessage): void {
    this.sink(serializeOutboundMessage(message));
  }

  private renderDetails(featureRef: string, sourceRef: string): void {
    const container = this.options.detailsContainer;
    if (!container) return;
    const feature = this.scene.features.find((candidate) => candidate.ref === featureRef && candidate.sourceRef === sourceRef);
    if (!feature) {
      container.replaceChildren();
      return;
    }
    renderFeatureDetails(container, feature, {
      onResourceActivated: (event) => this.surface.activateResource(event.featureRef, event.resourceRef),
      onActionActivated: (event) => this.surface.activateAction(event.featureRef, event.actionRef),
    });
  }
}

export class OrientationHostBridgeCore {
  private initialized = false;
  private destroyed = false;

  constructor(
    private readonly sink: OrientationBridgeMessageSink,
    private readonly handlers: OrientationBridgeCommandHandlers,
  ) {}

  initialize(): void {
    if (this.initialized || this.destroyed) return;
    this.initialized = true;
    this.emit({
      contract: ORIENTATION_HOST_BRIDGE_CONTRACT,
      version: ORIENTATION_HOST_BRIDGE_VERSION,
      type: "bridge.ready",
      payload: {},
    });
  }

  receive(serializedMessage: string): void {
    if (this.destroyed) {
      this.emitError("bridge.destroyed", "Bridge is destroyed.");
      return;
    }
    try {
      const message = parseInboundMessage(serializedMessage);
      switch (message.type) {
        case "scene.replace": this.handlers.replaceScene(message.payload); return;
        case "current-position.set": this.handlers.setCurrentPosition(message.payload); return;
        case "current-position.clear": this.handlers.clearCurrentPosition(); return;
      }
    } catch (error) {
      this.emitError(error instanceof BridgeInputError ? error.code : "message.rejected", error instanceof BridgeInputError ? error.message : "Inbound bridge message was rejected.");
    }
  }

  destroy(): void {
    this.destroyed = true;
  }

  private emit(message: OrientationHostBridgeOutboundMessage): void {
    this.sink(serializeOutboundMessage(message));
  }

  private emitError(code: string, message: string): void {
    this.emit({
      contract: ORIENTATION_HOST_BRIDGE_CONTRACT,
      version: ORIENTATION_HOST_BRIDGE_VERSION,
      type: "bridge.error",
      payload: { code, message },
    });
  }
}

export function parseInboundMessage(serializedMessage: string): InboundMessage {
  let input: unknown;
  try {
    input = JSON.parse(serializedMessage);
  } catch {
    throw new BridgeInputError("invalid-json", "Inbound bridge message is not valid JSON.");
  }
  if (!isRecord(input)) {
    throw new BridgeInputError("invalid-envelope", "Inbound bridge message must be an object.");
  }
  assertKeys(input, ["contract", "version", "type", "payload"], "invalid-envelope");
  if (input.contract !== ORIENTATION_HOST_BRIDGE_CONTRACT) {
    throw new BridgeInputError("unsupported-contract", "Unsupported bridge contract.");
  }
  if (input.version !== ORIENTATION_HOST_BRIDGE_VERSION) {
    throw new BridgeInputError("unsupported-version", "Unsupported bridge contract version.");
  }
  if (typeof input.type !== "string") {
    throw new BridgeInputError("unknown-message-type", "Bridge message type is invalid.");
  }

  switch (input.type) {
    case "scene.replace":
      validateWireScene(input.payload);
      return { type: input.type, payload: input.payload as SpatialScene };
    case "current-position.set":
      validateWirePosition(input.payload);
      return { type: input.type, payload: input.payload as PositionFix };
    case "current-position.clear":
      if (!isRecord(input.payload)) {
        throw new BridgeInputError("malformed-payload", "Clear-position payload must be an object.");
      }
      assertKeys(input.payload, [], "malformed-payload");
      return { type: input.type, payload: {} };
    default:
      throw new BridgeInputError("unknown-message-type", "Unsupported bridge message type.");
  }
}

export class BridgeInputError extends Error {
  constructor(readonly code: string, message: string) {
    super(message);
  }
}

function validateWireScene(value: unknown): asserts value is SpatialScene {
  if (!isRecord(value)) {
    throw new BridgeInputError("malformed-payload", "Scene payload must be an object.");
  }
  assertKeys(value, ["features", "viewport"], "malformed-payload");
  if (!Array.isArray(value.features)) {
    throw new BridgeInputError("malformed-payload", "Scene features must be an array.");
  }
  for (const feature of value.features) {
    if (!isRecord(feature)) {
      throw new BridgeInputError("malformed-payload", "Scene feature must be an object.");
    }
    assertKeys(feature, ["ref", "sourceRef", "coordinate", "title", "subtitle", "information", "resources", "actions"], "malformed-payload");
    if (typeof feature.ref !== "string" || typeof feature.sourceRef !== "string" || typeof feature.title !== "string") {
      throw new BridgeInputError("malformed-payload", "Scene feature identity and title must be strings.");
    }
    validateWireCoordinate(feature.coordinate);
    if (feature.subtitle !== undefined && typeof feature.subtitle !== "string") {
      throw new BridgeInputError("malformed-payload", "Feature subtitle must be a string.");
    }
    validateWireInformation(feature.information);
    validateWireResources(feature.resources);
    validateWireActions(feature.actions);
  }
  if (value.viewport !== undefined) {
    if (!isRecord(value.viewport)) {
      throw new BridgeInputError("malformed-payload", "Scene viewport must be an object.");
    }
    assertKeys(value.viewport, ["kind", "padding", "maxZoom"], "malformed-payload");
  }
  try {
    validateScene(value as SpatialScene);
  } catch {
    throw new BridgeInputError("invalid-scene", "Scene payload failed Orientation validation.");
  }
}

function validateWirePosition(value: unknown): asserts value is PositionFix {
  if (!isRecord(value)) {
    throw new BridgeInputError("malformed-payload", "Position payload must be an object.");
  }
  assertKeys(value, ["coordinate", "accuracyMeters", "observedAt"], "malformed-payload");
  try {
    validatePositionFix(value as PositionFix);
  } catch {
    throw new BridgeInputError("invalid-position", "Position payload failed Orientation validation.");
  }
}

function validateWireCoordinate(value: unknown): void {
  if (!isRecord(value) || typeof value.longitude !== "number" || typeof value.latitude !== "number") {
    throw new BridgeInputError("malformed-payload", "Coordinate must contain numeric longitude and latitude.");
  }
}

function validateWireInformation(value: unknown): void {
  if (value === undefined) return;
  if (!Array.isArray(value)) throw new BridgeInputError("malformed-payload", "Information must be an array.");
  for (const section of value) {
    if (!isRecord(section)) throw new BridgeInputError("malformed-payload", "Information section must be an object.");
    assertKeys(section, ["title", "rows"], "malformed-payload");
    if (section.title !== undefined && typeof section.title !== "string") throw new BridgeInputError("malformed-payload", "Information title must be a string.");
    if (!Array.isArray(section.rows)) throw new BridgeInputError("malformed-payload", "Information rows must be an array.");
    for (const row of section.rows) {
      if (!isRecord(row) || typeof row.label !== "string" || typeof row.value !== "string") throw new BridgeInputError("malformed-payload", "Information row must contain string label and value.");
      assertKeys(row, ["label", "value"], "malformed-payload");
    }
  }
}

function validateWireResources(value: unknown): void {
  if (value === undefined) return;
  if (!Array.isArray(value)) throw new BridgeInputError("malformed-payload", "Resources must be an array.");
  for (const resource of value) {
    if (!isRecord(resource) || typeof resource.ref !== "string" || typeof resource.label !== "string") throw new BridgeInputError("malformed-payload", "Resource must contain string ref and label.");
    assertKeys(resource, ["ref", "label", "uri"], "malformed-payload");
    if (resource.uri !== undefined && typeof resource.uri !== "string") throw new BridgeInputError("malformed-payload", "Resource URI must be a string.");
  }
}

function validateWireActions(value: unknown): void {
  if (value === undefined) return;
  if (!Array.isArray(value)) throw new BridgeInputError("malformed-payload", "Actions must be an array.");
  for (const action of value) {
    if (!isRecord(action) || typeof action.ref !== "string" || typeof action.label !== "string") throw new BridgeInputError("malformed-payload", "Action must contain string ref and label.");
    assertKeys(action, ["ref", "label"], "malformed-payload");
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function assertKeys(value: Record<string, unknown>, allowed: readonly string[], code: string): void {
  if (Object.keys(value).some((key) => !allowed.includes(key))) {
    throw new BridgeInputError(code, "Bridge payload contains unsupported fields.");
  }
}
