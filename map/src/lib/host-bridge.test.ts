import { describe, expect, it, vi } from "vitest";
import Ajv2020 from "ajv/dist/2020";
import addFormats from "ajv-formats";
import {
  ORIENTATION_HOST_BRIDGE_CONTRACT,
  ORIENTATION_HOST_BRIDGE_VERSION,
  OrientationHostBridgeCore,
  parseInboundMessage,
  serializeOutboundMessage,
} from "./host-bridge";
import { readFileSync } from "node:fs";

const validScene = { features: [] };
const validPosition = {
  coordinate: { longitude: 10, latitude: 50 },
  accuracyMeters: 25,
  observedAt: "2026-08-14T12:00:00.000Z",
};

function envelope(type: string, payload: unknown): string {
  return JSON.stringify({ contract: ORIENTATION_HOST_BRIDGE_CONTRACT, version: ORIENTATION_HOST_BRIDGE_VERSION, type, payload });
}

describe("Orientation host bridge protocol", () => {
  it("accepts the three inbound v1 messages", () => {
    expect(parseInboundMessage(envelope("scene.replace", validScene)).type).toBe("scene.replace");
    expect(parseInboundMessage(envelope("current-position.set", validPosition)).type).toBe("current-position.set");
    expect(parseInboundMessage(envelope("current-position.clear", {})).type).toBe("current-position.clear");
  });

  it("rejects wrong contract, version, command, malformed payload and unsupported keys", () => {
    expect(() => parseInboundMessage("not-json")).toThrow(/valid JSON/);
    expect(() => parseInboundMessage(JSON.stringify({ contract: "other", version: "1.0", type: "scene.replace", payload: validScene }))).toThrow(/contract/);
    expect(() => parseInboundMessage(JSON.stringify({ contract: ORIENTATION_HOST_BRIDGE_CONTRACT, version: "2.0", type: "scene.replace", payload: validScene }))).toThrow(/version/);
    expect(() => parseInboundMessage(envelope("window.open", {}))).toThrow(/message type/);
    expect(() => parseInboundMessage(envelope("scene.replace", { features: [{ ref: "x", sourceRef: "p", coordinate: { longitude: 0, latitude: 0 }, title: "x", execute: "bad" }] }))).toThrow(/unsupported fields/);
    expect(() => parseInboundMessage(envelope("current-position.set", { ...validPosition, accuracyMeters: -1 }))).toThrow(/validation/);
  });

  it("keeps bridge core independently testable and rejects after destroy", () => {
    const emitted: string[] = [];
    const handlers = { replaceScene: vi.fn(), setCurrentPosition: vi.fn(), clearCurrentPosition: vi.fn() };
    const core = new OrientationHostBridgeCore((message) => emitted.push(message), handlers);

    core.initialize();
    core.initialize();
    core.receive(envelope("scene.replace", validScene));
    core.receive(envelope("current-position.set", validPosition));
    core.receive(envelope("current-position.clear", {}));
    expect(handlers.replaceScene).toHaveBeenCalledWith(validScene);
    expect(handlers.setCurrentPosition).toHaveBeenCalledWith(validPosition);
    expect(handlers.clearCurrentPosition).toHaveBeenCalledOnce();
    expect(emitted.filter((message) => JSON.parse(message).type === "bridge.ready")).toHaveLength(1);

    core.destroy();
    core.receive(envelope("scene.replace", validScene));
    expect(JSON.parse(emitted.at(-1)!).payload.code).toBe("bridge.destroyed");
  });

  it("serializes all generic outbound event shapes", () => {
    const identity = { featureRef: "feature/1", sourceRef: "provider/1" };
    expect(JSON.parse(serializeOutboundMessage({ contract: ORIENTATION_HOST_BRIDGE_CONTRACT, version: ORIENTATION_HOST_BRIDGE_VERSION, type: "bridge.ready", payload: {} })).type).toBe("bridge.ready");
    expect(JSON.parse(serializeOutboundMessage({ contract: ORIENTATION_HOST_BRIDGE_CONTRACT, version: ORIENTATION_HOST_BRIDGE_VERSION, type: "map.status", payload: { status: "ready" } })).payload.status).toBe("ready");
    expect(JSON.parse(serializeOutboundMessage({ contract: ORIENTATION_HOST_BRIDGE_CONTRACT, version: ORIENTATION_HOST_BRIDGE_VERSION, type: "feature.selected", payload: identity })).payload).toEqual(identity);
    expect(JSON.parse(serializeOutboundMessage({ contract: ORIENTATION_HOST_BRIDGE_CONTRACT, version: ORIENTATION_HOST_BRIDGE_VERSION, type: "resource.activated", payload: { ...identity, resourceRef: "resource/1" } })).payload.resourceRef).toBe("resource/1");
    expect(JSON.parse(serializeOutboundMessage({ contract: ORIENTATION_HOST_BRIDGE_CONTRACT, version: ORIENTATION_HOST_BRIDGE_VERSION, type: "action.activated", payload: { ...identity, actionRef: "action/1" } })).payload.actionRef).toBe("action/1");
  });

  it("validates documented examples against the Draft 2020-12 schema", () => {
    const schema = JSON.parse(readFileSync(new URL("../../../contracts/orientation-host-bridge-v1.schema.json", import.meta.url), "utf8"));
    const ajv = new Ajv2020({ strict: false });
    addFormats(ajv);
    const validate = ajv.compile(schema);
    expect(schema.$id).toBe("https://schemas.wgt-system.org/orientation/host-bridge/1.0/schema.json");
    const examples = [
      { contract: ORIENTATION_HOST_BRIDGE_CONTRACT, version: ORIENTATION_HOST_BRIDGE_VERSION, type: "scene.replace", payload: validScene },
      { contract: ORIENTATION_HOST_BRIDGE_CONTRACT, version: ORIENTATION_HOST_BRIDGE_VERSION, type: "current-position.set", payload: validPosition },
      { contract: ORIENTATION_HOST_BRIDGE_CONTRACT, version: ORIENTATION_HOST_BRIDGE_VERSION, type: "current-position.clear", payload: {} },
      { contract: ORIENTATION_HOST_BRIDGE_CONTRACT, version: ORIENTATION_HOST_BRIDGE_VERSION, type: "feature.selected", payload: { featureRef: "feature/1", sourceRef: "provider/1" } },
    ];
    expect(examples.every((example) => validate(example))).toBe(true);
    expect(validate({ ...examples[0], version: "9.0" })).toBe(false);
    expect(validate({ ...examples[0], payload: { features: "not-an-array" } })).toBe(false);
    expect(validate({ ...examples[1], payload: { ...validPosition, observedAt: "2026-02-31T12:00:00.000Z" } })).toBe(false);
    for (const uri of ["javascript:alert(1)", "data:text/plain,x", "file:///tmp/x", "HTTPS://example.com/item"]) {
      const candidate = { ...examples[0], payload: { features: [{ ref: "f", sourceRef: "p", coordinate: { longitude: 0, latitude: 0 }, title: "F", resources: [{ ref: "r", label: "R", uri }] }] } };
      expect(validate(candidate)).toBe(false);
      expect(() => parseInboundMessage(JSON.stringify(candidate))).toThrow();
    }
    const httpResource = { ...examples[0], payload: { features: [{ ref: "f", sourceRef: "p", coordinate: { longitude: 0, latitude: 0 }, title: "F", resources: [{ ref: "r", label: "R", uri: "https://example.com/item" }] }] } };
    expect(validate(httpResource)).toBe(true);
    expect(() => parseInboundMessage(JSON.stringify(httpResource))).not.toThrow();
  });
});
