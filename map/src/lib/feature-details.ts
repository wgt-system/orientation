import type {
  SpatialActionActivatedEvent,
  SpatialFeature,
  SpatialInformationSection,
  SpatialResourceActivatedEvent,
} from "./model";

export type FeatureDetailsCallbacks = Readonly<{
  onResourceActivated: (event: SpatialResourceActivatedEvent) => void;
  onActionActivated: (event: SpatialActionActivatedEvent) => void;
}>;

export type FeatureDetailsModel = Readonly<{
  title: string;
  subtitle?: string;
  information: readonly SpatialInformationSection[];
  resources: readonly Readonly<{ ref: string; label: string }>[];
  actions: readonly Readonly<{ ref: string; label: string }>[];
  featureRef: string;
  sourceRef: string;
}>;

export function createFeatureDetailsModel(feature: SpatialFeature): FeatureDetailsModel {
  return {
    title: feature.title,
    ...(feature.subtitle !== undefined ? { subtitle: feature.subtitle } : {}),
    information: feature.information ?? [],
    resources: feature.resources ?? [],
    actions: feature.actions ?? [],
    featureRef: feature.ref,
    sourceRef: feature.sourceRef,
  };
}

export function renderFeatureDetails(
  container: HTMLElement,
  feature: SpatialFeature,
  callbacks: FeatureDetailsCallbacks,
): void {
  const details = createFeatureDetailsModel(feature);
  container.replaceChildren();

  const title = document.createElement("strong");
  title.textContent = details.title;
  container.append(title);

  if (details.subtitle) {
    const subtitle = document.createElement("span");
    subtitle.textContent = details.subtitle;
    container.append(subtitle);
  }

  for (const section of details.information) {
    if (section.title) {
      const heading = document.createElement("h2");
      heading.textContent = section.title;
      container.append(heading);
    }
    const rows = document.createElement("dl");
    for (const row of section.rows) {
      const label = document.createElement("dt");
      label.textContent = row.label;
      const value = document.createElement("dd");
      value.textContent = row.value;
      rows.append(label, value);
    }
    container.append(rows);
  }

  if (details.resources.length) {
    appendControlGroup(container, "Resources", details.resources.map((resource) => ({
      label: resource.label,
      onActivate: () => callbacks.onResourceActivated({
        featureRef: details.featureRef,
        sourceRef: details.sourceRef,
        resourceRef: resource.ref,
      }),
    })));
  }

  if (details.actions.length) {
    appendControlGroup(container, "Actions", details.actions.map((action) => ({
      label: action.label,
      onActivate: () => callbacks.onActionActivated({
        featureRef: details.featureRef,
        sourceRef: details.sourceRef,
        actionRef: action.ref,
      }),
    })));
  }

  const ref = document.createElement("code");
  ref.textContent = details.featureRef;
  container.append(ref);
}

function appendControlGroup(
  container: HTMLElement,
  title: string,
  controls: readonly { label: string; onActivate: () => void }[],
): void {
  const heading = document.createElement("h2");
  heading.textContent = title;
  container.append(heading);

  const group = document.createElement("div");
  group.className = "selection-controls";
  for (const control of controls) {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = control.label;
    button.addEventListener("click", control.onActivate);
    group.append(button);
  }
  container.append(group);
}
