import { describe, expect, it } from "vitest";
import { createFeatureDetailsModel } from "./feature-details";

describe("createFeatureDetailsModel", () => {
  it("preserves provider strings as data and keeps generic identities", () => {
    const details = createFeatureDetailsModel({
      ref: "feature/1",
      sourceRef: "provider/1",
      coordinate: { longitude: 10, latitude: 50 },
      title: "<img src=x onerror=alert(1)>",
      subtitle: "<b>plain text</b>",
      information: [{ rows: [{ label: "<label>", value: "<value>" }] }],
      resources: [{ ref: "resource/1", label: "Open resource" }],
      actions: [{ ref: "action/1", label: "Run action" }],
    });

    expect(details.title).toContain("<img");
    expect(details.subtitle).toBe("<b>plain text</b>");
    expect(details.information[0]!.rows[0]!.value).toBe("<value>");
    expect(details.resources[0]!.ref).toBe("resource/1");
    expect(details.actions[0]!.ref).toBe("action/1");
    expect(details.featureRef).toBe("feature/1");
    expect(details.sourceRef).toBe("provider/1");
  });

  it("replaces details deterministically without retaining prior content", () => {
    const first = createFeatureDetailsModel({
      ref: "first", sourceRef: "provider", coordinate: { longitude: 0, latitude: 0 }, title: "First",
      resources: [{ ref: "old", label: "Old" }],
    });
    const second = createFeatureDetailsModel({
      ref: "second", sourceRef: "provider", coordinate: { longitude: 1, latitude: 1 }, title: "Second",
    });

    expect(first.resources).toHaveLength(1);
    expect(second.resources).toHaveLength(0);
    expect(second.title).not.toBe(first.title);
  });
});
