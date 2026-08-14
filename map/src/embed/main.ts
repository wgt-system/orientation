import "maplibre-gl/dist/maplibre-gl.css";
import "../styles.css";

import { OrientationHostBridge } from "../lib/host-bridge";

declare global {
  interface Window {
    orientationHostBridge?: Readonly<{
      receive: (serializedMessage: string) => void;
      destroy: () => void;
    }>;
  }
}

const mapContainer = document.querySelector<HTMLElement>("#map");
const detailsContainer = document.querySelector<HTMLElement>("#details");
if (!mapContainer || !detailsContainer) {
  throw new Error("Orientation embed DOM is incomplete.");
}
if (window.orientationHostBridge) {
  throw new Error("Orientation host bridge is already initialized.");
}

const bridge = new OrientationHostBridge(mapContainer, (serializedMessage) => {
  queueMicrotask(() => {
    window.dispatchEvent(new CustomEvent("orientation-host-bridge-message", {
      detail: serializedMessage,
    }));
    if (window.parent !== window) {
      window.parent.postMessage(serializedMessage, "*");
    }
  });
}, { detailsContainer });

window.orientationHostBridge = Object.freeze({
  receive: (serializedMessage: string) => bridge.receive(serializedMessage),
  destroy: () => bridge.destroy(),
});

window.addEventListener("message", (event) => {
  if (event.source === window.parent && typeof event.data === "string") {
    bridge.receive(event.data);
  }
});

window.addEventListener("beforeunload", () => bridge.destroy(), { once: true });
